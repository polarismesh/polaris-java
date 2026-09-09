/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.plugin.filter.ConfigFileFilterChain;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.client.util.ConfigFileUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @author lepdou 2022-03-01
 */
public class RemoteConfigFileRepo extends AbstractConfigFileRepo {

    private static final long INIT_VERSION = 0;

    private static final int PULL_CONFIG_RETRY_TIMES = 3;

    /**
     * 首次拉取失败降级本地缓存后，后台补拉的延迟秒数。留出一点时间让瞬时故障自行恢复，
     * 同时远早于长轮询首轮（静默 5 秒起步，失败后最长退避 120 秒）。
     */
    private static final long CATCH_UP_PULL_DELAY_SECONDS = 3;

    private static ScheduledExecutorService pullExecutorService;

    private static Set<String> configFileInitSet = new HashSet<>();

    private final Object snapshotLock = new Object();

    private final AtomicReference<ConfigFile> remoteConfigFile;
    //服务端通知的版本号，此版本号有可能落后于服务端
    private final AtomicLong notifiedVersion;
    //配置在客户端本地的实际生效时刻（毫秒时间戳），仅在写入非删除配置时更新，删除场景保留上次生效时间
    private final AtomicLong effectiveTime;
    private final ConfigFileConnector configFileConnector;
    private final ConfigFileFilterChain configFileFilterChain;
    private final RetryPolicy retryPolicy;
    private ConfigFilePersistentHandler configFilePersistHandler;
    private final boolean fallbackToLocalCache;
    //是否已安排过降级后的后台补拉，保证每个配置文件最多补拉一次
    private final AtomicBoolean catchUpPullScheduled = new AtomicBoolean(false);

    private String token;

    private final boolean emptyProtection;

    private final long emptyProtectionExpiredInterval;

    /**
     * 淘汰线程
     */
    private final ScheduledExecutorService emptyProtectionExpireExecutor;

    private ScheduledFuture<?> emptyProtectionExpireFuture;

    static {
        createPullExecutorService();
    }

    public RemoteConfigFileRepo(SDKContext sdkContext,
                                ConfigFileLongPullService pullService,
                                ConfigFileFilterChain configFileFilterChain,
                                ConfigFileConnector connector,
                                ConfigFileMetadata configFileMetadata,
                                ConfigFilePersistentHandler handler) {
        super(sdkContext, configFileMetadata);
        //保证线程池正常初始化
        createPullExecutorService();
        this.token = sdkContext.getConfig().getConfigFile().getServerConnector().getToken();
        this.remoteConfigFile = new AtomicReference<>();
        this.notifiedVersion = new AtomicLong(INIT_VERSION);
        this.effectiveTime = new AtomicLong(0);
        this.retryPolicy = new ExponentialRetryPolicy(1, 120);
        this.configFilePersistHandler = handler;
        this.configFileFilterChain = configFileFilterChain;
        //获取远程调用插件实现类
        this.configFileConnector = connector;
        this.fallbackToLocalCache = sdkContext.getConfig().getConfigFile().getServerConnector().getFallbackToLocalCache();
        this.emptyProtection = sdkContext.getConfig().getConfigFile().getServerConnector().isEmptyProtectionEnable();
        this.emptyProtectionExpiredInterval = sdkContext.getConfig().getConfigFile().getServerConnector().getEmptyProtectionExpiredInterval();
        this.emptyProtectionExpireExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("polaris-config-empty-protection"));
        //注册 destroy hook
        registerRepoDestroyHook(sdkContext);
        //同步从远程仓库拉取一次
        pull();
        //加入到长轮询的池子里
        addToLongPollingPool(pullService, configFileMetadata);
        startCheckVersionTask();
    }

    private static void createPullExecutorService() {
        if (pullExecutorService == null || pullExecutorService.isShutdown() || pullExecutorService.isTerminated()) {
            pullExecutorService = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Configuration-Pull"));
        }
    }

    private void addToLongPollingPool(ConfigFileLongPullService pullService, ConfigFileMetadata configFileMetadata) {
        ConfigFile configFile = remoteConfigFile.get();

        //理论上，加到长轮询任务之前会同步一次配置文件，但是可能会同步失败。
        if (configFile == null) {
            configFile = new ConfigFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                    configFileMetadata.getFileName());
            //初始版本号为0
            configFile.setVersion(INIT_VERSION);
        }

        pullService.addConfigFile(this);
    }

    @Override
    public String getContent() {
        return remoteConfigFile.get() != null ? remoteConfigFile.get().getContent() : null;
    }

    public String getMd5() {
        return remoteConfigFile.get() != null ? remoteConfigFile.get().getMd5() : "";
    }

    /**
     * {@code remoteConfigFile} 持有的是服务端下发的响应对象，其 encrypted 是逐文件真值，
     * 可直接作为判据；请求侧对象会被加密过滤器无条件置 true，不可用。
     */
    @Override
    public boolean isEncrypted() {
        ConfigFile configFile = remoteConfigFile.get();
        return configFile != null && configFile.isEncrypted();
    }

    public long getConfigFileVersion() {
        if (remoteConfigFile.get() == null) {
            return INIT_VERSION;
        }
        return remoteConfigFile.get().getVersion();
    }

    /**
     * 一次性读取当前版本、版本名、MD5、源内容与生效时间。content 取源内容（sourceContent，加密配置为密文），
     * 与 md5（源内容摘要）自洽且不回传解密明文；非加密配置源内容为空时回退 content。
     *
     * @return 包含 version、versionName、md5、content、effectiveTime 的快照
     */
    public ConfigFileSnapshot getSnapshot() {
        synchronized (snapshotLock) {
            ConfigFile configFile = remoteConfigFile.get();
            if (configFile == null) {
                return null;
            }
            String content = configFile.getSourceContent();
            if (content == null) {
                content = configFile.isEncrypted() ? "" : configFile.getContent();
            }
            return new ConfigFileSnapshot(configFile, content, effectiveTime.get());
        }
    }

    /**
     * 服务端通知的版本号（可能落后于服务端实际版本）。pending 场景回带供服务端参考。
     *
     * @return 通知版本号
     */
    public long getNotifiedVersion() {
        return notifiedVersion.get();
    }

    private void updateRemoteConfigFile(ConfigFile configFile) {
        synchronized (snapshotLock) {
            remoteConfigFile.set(configFile);
            effectiveTime.set(System.currentTimeMillis());
        }
        super.fireChangeEvent(configFile);
    }

    private void removeRemoteConfigFile() {
        boolean removed = false;
        synchronized (snapshotLock) {
            if (remoteConfigFile.get() != null) {
                remoteConfigFile.set(null);
                removed = true;
            }
        }
        if (removed) {
            super.fireChangeEvent(null);
        }
    }

    @Override
    protected void doPull() {
        doPull(PULL_CONFIG_RETRY_TIMES);
    }

    /**
     * 拉取配置，失败后按指数退避重试至多 maxRetryTimes 次。
     *
     * @param maxRetryTimes 最大尝试次数。降级后的后台补拉传 1：只试一次且不退避，
     *                      避免长时间占用所有配置文件共用的单线程拉取器
     */
    private void doPull(int maxRetryTimes) {
        long startTime = System.currentTimeMillis();

        ConfigFile pullConfigFileReq = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        pullConfigFileReq.setVersion(notifiedVersion.get());

        LOGGER.info("[Config] start pull config file. config file = {}, version = {}",
                configFileMetadata, notifiedVersion.get());

        int retryTimes = 0;
        while (retryTimes < maxRetryTimes) {
            try {

                ConfigFileResponse response = configFileFilterChain
                        .execute(pullConfigFileReq, configFile -> configFileConnector.getConfigFile(pullConfigFileReq));

                retryPolicy.success();

                //打印请求信息
                long pulledConfigFileVersion =
                        response.getConfigFile() != null ? response.getConfigFile().getVersion() : -1;
                LOGGER.info("[Config] pull config file finished. config file = {}, code = {}, version = {}, duration = {} ms",
                        configFileMetadata, response.getCode(), pulledConfigFileVersion,
                        System.currentTimeMillis() - startTime);

                if (response.getCode() == ServerCodes.EXECUTE_SUCCESS) {
                    ConfigFile pulledConfigFile = response.getConfigFile();

                    //本地配置文件落后，更新内存缓存
                    boolean shouldUpdateLocalCache;
                    if (configFileConnector.isNotifiedVersionIncreaseStrictly()) {
                        shouldUpdateLocalCache = remoteConfigFile.get() == null || pulledConfigFile.getVersion() >= remoteConfigFile.get().getVersion();
                    } else {
                        shouldUpdateLocalCache = remoteConfigFile.get() == null || pulledConfigFile.getVersion() != remoteConfigFile.get().getVersion();
                    }
                    // 构造更新回调动作
                    Runnable runnable = () -> {
                        ConfigFile copiedConfigFile = deepCloneConfigFile(pulledConfigFile);
                        updateRemoteConfigFile(copiedConfigFile);

                        // update local file cache
                        this.configFilePersistHandler.asyncSaveConfigFile(pulledConfigFile);
                    };
                    if (shouldUpdateLocalCache && checkEmptyProtect(response)) {
                        shouldUpdateLocalCache = false;
                        fallbackIfNecessaryWhenStartingUp(pullConfigFileReq);
                        submitEmptyProtectionExpireTask(runnable);
                    }
                    if (shouldUpdateLocalCache) {
                        runnable.run();
                        cancelEmptyProtectionExpireTask();
                    }
                    return;
                }

                //远端没有此配置文件
                if (response.getCode() == ServerCodes.NOT_FOUND_RESOURCE) {
                    LOGGER.warn("[Config] config file not found, please check whether config file released. {}",
                            configFileMetadata);
                    // 构造更新回调动作
                    Runnable runnable = () -> {
                        //delete local file cache
                        this.configFilePersistHandler
                                .asyncDeleteConfigFile(new ConfigFile(configFileMetadata.getNamespace(),
                                        configFileMetadata.getFileGroup(), configFileMetadata.getFileName()));

                        //删除配置文件
                        removeRemoteConfigFile();
                    };
                    if (checkEmptyProtect(response)) {
                        fallbackIfNecessaryWhenStartingUp(pullConfigFileReq);
                        submitEmptyProtectionExpireTask(runnable);
                    } else {
                        runnable.run();
                        cancelEmptyProtectionExpireTask();
                    }
                    return;
                }

                //预期之外的状态码，重试
                LOGGER.error("[Config] pull response without expected code. retry times = {}, code = {}", retryTimes,
                        response.getCode());

                retryTimes++;
                if (afterPullFailure(retryTimes, maxRetryTimes, pullConfigFileReq)) {
                    return;
                }
            } catch (Throwable t) {
                LOGGER.error("[Config] failed to pull config file. retry times = " + retryTimes, t);

                retryTimes++;
                if (afterPullFailure(retryTimes, maxRetryTimes, pullConfigFileReq)) {
                    return;
                }
            }
        }
    }

    /**
     * 单次拉取失败后的收尾：记退避、按需降级本地缓存。
     *
     * @param retryTimes 已尝试次数
     * @param maxRetryTimes 最大尝试次数
     * @param configFileReq 拉取请求
     * @return 无需再重试返回 true
     */
    private boolean afterPullFailure(int retryTimes, int maxRetryTimes, ConfigFile configFileReq) {
        retryPolicy.fail();
        if (retryTimes == 1 && maxRetryTimes > 1 && fallbackToLocalCacheOnFirstMiss(configFileReq)) {
            return true;
        }
        //只有确实还要再试才退避，否则最后一次失败后白等一轮，平白拉长启动阻塞
        if (retryTimes < maxRetryTimes) {
            retryPolicy.executeDelay();
        }
        fallbackIfNecessary(retryTimes, maxRetryTimes, configFileReq);
        return false;
    }

    /**
     * 内存尚未有配置时，远端第一次失败就尝试本地缓存。命中则跳过剩余退避重试，
     * 避免 Spring config-data 阶段（早于 banner、同步阻塞）被 1/2/4 秒三次重试拖到分钟级。
     * 内存已有配置时不走这条捷径，仍耗尽重试再决定是否覆盖。
     *
     * @param configFileReq 拉取请求
     * @return 已用本地缓存填上内存则为 true
     */
    private boolean fallbackToLocalCacheOnFirstMiss(ConfigFile configFileReq) {
        if (remoteConfigFile.get() != null) {
            return false;
        }
        loadLocalCache(configFileReq, true);
        if (remoteConfigFile.get() == null) {
            return false;
        }
        LOGGER.warn("[Config] skip remaining pull retries, use local cache. config file = {}", configFileMetadata);
        scheduleCatchUpPull();
        return true;
    }

    /**
     * 降级本地缓存后，在后台补拉一次远端配置，每个配置文件最多一次。
     *
     * <p>本地缓存可能已落后于服务端：启动期间可能已发布新版本，或已完成密钥轮换与旧凭据吊销。
     * 跳过同步重试换来的启动速度，不应让客户端长期停留在旧配置上。只靠长轮询兜底不够及时——
     * 它首轮前静默 5 秒，失败后按指数退避，最长可达 120 秒才再试一次。
     *
     * <p>补拉只试一次且不退避：拉取线程池是全部配置文件共用的单线程，长轮询感知到变更后也要
     * 经它触发拉取，补拉不能把它占住。这一次没成也无妨，长轮询仍是最终兜底。
     */
    private void scheduleCatchUpPull() {
        if (!catchUpPullScheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            pullExecutorService.schedule(this::catchUpPull, CATCH_UP_PULL_DELAY_SECONDS, TimeUnit.SECONDS);
            LOGGER.info("[Config] catch up pull scheduled in {}s. config file = {}",
                    CATCH_UP_PULL_DELAY_SECONDS, configFileMetadata);
        } catch (RejectedExecutionException e) {
            LOGGER.warn("[Config] catch up pull rejected, rely on long polling. config file = {}", configFileMetadata);
        }
    }

    private void catchUpPull() {
        try {
            doPull(1);
        } catch (Throwable t) {
            LOGGER.warn("[Config] catch up pull failed, rely on long polling. config file = {}", configFileMetadata, t);
        }
    }

    private void fallbackIfNecessary(final int retryTimes, int maxRetryTimes, ConfigFile configFileReq) {
        if (retryTimes < maxRetryTimes) {
            return;
        }
        //内存已有配置（含此前的降级结果），本地缓存不会比它更新，重复加载只会多发一次无意义的变更通知
        if (remoteConfigFile.get() != null) {
            LOGGER.info("[Config] failed to pull config file from remote, keep current config in memory.");
            return;
        }
        LOGGER.info("[Config] failed to pull config file from remote.");
        //重试次数超过上限，从本地缓存拉取
        loadLocalCache(configFileReq, true);
    }

    private void fallbackIfNecessaryWhenStartingUp(ConfigFile configFileReq) {
        String identifier = getIdentifier();
        boolean initFlag = false;
        if (configFileInitSet.contains(identifier)) {
            initFlag = true;
        } else {
            configFileInitSet.add(identifier);
        }
        if (!initFlag) {
            // 第一次启动的时候，如果拉取到空配置，则尝试从缓存中获取
            LOGGER.info("[Config] load local cache because of empty config when starting up.");
            // 不需要重试
            loadLocalCache(configFileReq, false);
        }
    }

    private void loadLocalCache(ConfigFile configFileReq, boolean needRetry) {
        if (fallbackToLocalCache) {
            ConfigFile configFileRes = configFilePersistHandler.loadPersistedConfigFile(configFileReq, needRetry);
            if (configFileRes != null) {
                LOGGER.info("[Config] load local cache success. namespace={}, fileGroup={}, fileName={}, "
                                + "version={}, encrypted={}", configFileRes.getNamespace(),
                        configFileRes.getFileGroup(), configFileRes.getFileName(), configFileRes.getVersion(),
                        configFileRes.isEncrypted());
                updateRemoteConfigFile(configFileRes);
                return;
            }
            LOGGER.info("[Config] load local cache fail. namespace={}, fileGroup={}, fileName={}",
                    configFileReq.getNamespace(), configFileReq.getFileGroup(), configFileReq.getFileName());
        }
    }

    public void onLongPollNotified(long newVersion) {
        if (configFileConnector.isNotifiedVersionIncreaseStrictly()) {
            if (remoteConfigFile.get() != null && remoteConfigFile.get().getVersion() >= newVersion) {
                return;
            }
        } else {
            if (remoteConfigFile.get() != null && remoteConfigFile.get().getVersion() == newVersion) {
                return;
            }
        }

        notifiedVersion.set(newVersion);

        //版本落后，从服务端拉取最新的配置文件
        pullExecutorService.submit((Runnable) this::pull);
    }

    // 有可能出现收到通知时，重新拉取配置失败。此时 notifiedVersion 大于 remoteConfigFile.version，需要定时重试
    private void startCheckVersionTask() {
        pullExecutorService.scheduleAtFixedRate(() -> {
            //没有通知的版本号
            if (notifiedVersion == null || notifiedVersion.get() == 0) {
                return;
            }

            long pulledVersion =
                    remoteConfigFile.get() != null ? remoteConfigFile.get().getVersion() : INIT_VERSION;

            //版本落后，需要重新拉取
            boolean shouldRetry;
            if (configFileConnector.isNotifiedVersionIncreaseStrictly()) {
                shouldRetry = notifiedVersion.get() > pulledVersion;
            } else {
                shouldRetry = notifiedVersion.get() != pulledVersion;
            }
            if (shouldRetry) {
                LOGGER.info("[Config] notified version greater than pulled version, will pull config file."
                                + "file = {}, notified version = {}, pulled version = {}", getConfigFileMetadata(),
                        notifiedVersion, pulledVersion);
                pull();
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    private ConfigFile deepCloneConfigFile(ConfigFile sourceConfigFile) {
        ConfigFile configFile =
                new ConfigFile(sourceConfigFile.getNamespace(), sourceConfigFile.getFileGroup(),
                        sourceConfigFile.getFileName());
        configFile.setContent(sourceConfigFile.getContent());
        configFile.setSourceContent(sourceConfigFile.getSourceContent());
        configFile.setVersion(sourceConfigFile.getVersion());
        configFile.setName(sourceConfigFile.getName());
        configFile.setMd5(sourceConfigFile.getMd5());
        configFile.setEncrypted(sourceConfigFile.isEncrypted());
        configFile.setPublicKey(sourceConfigFile.getPublicKey());
        configFile.setDataKey(sourceConfigFile.getDataKey());
        configFile.setEncryptAlgo(sourceConfigFile.getEncryptAlgo());
        // 不复制 cacheEncrypted：业务内存对象恒为明文态
        return configFile;
    }

    public static void registerRepoDestroyHook(SDKContext context) {
        context.registerDestroyHook(new Destroyable() {
            @Override
            protected void doDestroy() {
                destroyPullExecutor();
            }
        });
    }

    static void destroyPullExecutor() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{pullExecutorService});
    }

    /**
     * 若配置为空，则推空保护开启，则不刷新配置
     *
     * @param configFileResponse
     * @return
     */
    private boolean checkEmptyProtect(ConfigFileResponse configFileResponse) {
        if (emptyProtection && ConfigFileUtils.checkConfigContentEmpty(configFileResponse)) {
            LOGGER.warn("Empty response from remote with {}, will not refresh config.", getIdentifier());
            return true;
        }
        return false;
    }

    private void submitEmptyProtectionExpireTask(Runnable runnable) {
        if (emptyProtectionExpireFuture == null || emptyProtectionExpireFuture.isCancelled() || emptyProtectionExpireFuture.isDone()) {
            LOGGER.info("Empty protection expire task of {} submit.", getIdentifier());
            emptyProtectionExpireFuture = emptyProtectionExpireExecutor.schedule(runnable, emptyProtectionExpiredInterval, TimeUnit.MILLISECONDS);
        }
    }

    private void cancelEmptyProtectionExpireTask() {
        if (emptyProtectionExpireFuture != null && !emptyProtectionExpireFuture.isCancelled() && !emptyProtectionExpireFuture.isDone()) {
            emptyProtectionExpireFuture.cancel(true);
            LOGGER.info("Empty protection expire task of {} cancel.", getIdentifier());
        }
    }

    @JustForTest
    String getIdentifier() {
        return configFileMetadata.getNamespace() + "."
                + configFileMetadata.getFileGroup() + "."
                + configFileMetadata.getFileName();
    }
}
