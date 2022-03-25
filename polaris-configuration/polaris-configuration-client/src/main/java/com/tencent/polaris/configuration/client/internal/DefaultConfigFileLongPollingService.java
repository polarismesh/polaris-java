package com.tencent.polaris.configuration.client.internal;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lepdou 2022-03-02
 */
public class DefaultConfigFileLongPollingService implements ConfigFileLongPollingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfigFileLongPollingService.class);

    private static ConfigFileLongPollingService instance;

    private final ConfigFileConnector configFileConnector;

    private final ExecutorService longPollingService;

    private final Map<ConfigFileMetadata, RemoteConfigFileRepo> configFilePool;
    //此处只缓存所有配置文件对应通知的版本号，跟远端拉取的配置文件返回的版本号没有关系
    private final Map<ConfigFileMetadata, Long>                 notifiedVersion;
    private final AtomicReference<Boolean>                      started;
    private final RetryPolicy                                   retryPolicy;

    DefaultConfigFileLongPollingService(SDKContext sdkContext, ConfigFileConnector configFileConnector) {

        started = new AtomicReference<>(false);
        configFilePool = Maps.newConcurrentMap();
        notifiedVersion = Maps.newConcurrentMap();
        retryPolicy = new ExponentialRetryPolicy(1, 120);

        if (configFileConnector != null) {
            this.configFileConnector = configFileConnector;
        } else {
            //获取远程调用插件实现类
            String configFileConnectorType = sdkContext.getConfig().getConfigFile().getServerConnector().getConnectorType();
            this.configFileConnector = (ConfigFileConnector) sdkContext.getExtensions().getPlugins()
                .getPlugin(PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType(), configFileConnectorType);
        }

        //初始化 long polling 线程池
        NamedThreadFactory threadFactory = new NamedThreadFactory("Configuration-LongPolling");
        longPollingService = Executors.newSingleThreadExecutor(threadFactory);
    }

    public static ConfigFileLongPollingService getInstance(SDKContext sdkContext) {
        if (instance == null) {
            synchronized (DefaultConfigFileLongPollingService.class) {
                if (instance == null) {
                    instance = new DefaultConfigFileLongPollingService(sdkContext, null);
                }
            }
        }
        return instance;
    }

    @Override
    public void addConfigFile(RemoteConfigFileRepo remoteConfigFileRepo) {
        ConfigFileMetadata configFileMetadata = remoteConfigFileRepo.getConfigFileMetadata();
        long version = remoteConfigFileRepo.getConfigFileVersion();

        LOGGER.info("[Config] add long polling config file. file = {}, version = {}", configFileMetadata, version);

        configFilePool.putIfAbsent(configFileMetadata, remoteConfigFileRepo);
        //长轮询起始的配置文件版本号应该以第一次同步拉取为准
        notifiedVersion.putIfAbsent(configFileMetadata, version);

        if (!started.get()) {
            startLongPollingTask();
        }
    }

    private void startLongPollingTask() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        try {
            longPollingService.submit(() -> {
                // 执行 Long Polling 之前，停顿一段时间。等待池子里有足够多的配置文件
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    //ignore
                }

                doLongPolling();
            });
        } catch (Throwable t) {
            started.set(false);
        }

    }

    private void doLongPolling() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<ConfigFile> watchConfigFiles = assembleWatchConfigFiles();

                LOGGER.info("[Config] do long polling. config file size = {}, delay time = {}", watchConfigFiles.size(),
                            retryPolicy.getCurrentDelayTime());

                ConfigFileResponse response = configFileConnector.watchConfigFiles(watchConfigFiles);

                retryPolicy.success();

                int responseCode = response.getCode();
                //感知到配置文件发布事件
                if (responseCode == ServerCodes.EXECUTE_SUCCESS && response.getConfigFile() != null) {
                    ConfigFile changedConfigFile = response.getConfigFile();
                    ConfigFileMetadata metadata = new DefaultConfigFileMetadata(changedConfigFile.getNamespace(),
                                                                                changedConfigFile.getFileGroup(),
                                                                                changedConfigFile.getFileName());
                    long newNotifiedVersion = changedConfigFile.getVersion();
                    long oldNotifiedVersion = notifiedVersion.get(metadata);

                    long maxVersion = Math.max(newNotifiedVersion, oldNotifiedVersion);

                    //更新版本号
                    notifiedVersion.put(metadata, maxVersion);

                    LOGGER.info(
                        "[Config] received change event by long polling. file = {}, new version = {}, old version = {}",
                        metadata, newNotifiedVersion, oldNotifiedVersion);

                    //通知 RemoteConfigFileRepo 拉取最新的配置文件
                    RemoteConfigFileRepo remoteConfigFileRepo = configFilePool.get(metadata);
                    remoteConfigFileRepo.onLongPollNotified(maxVersion);

                    continue;
                }

                //没有变更
                if (responseCode == ServerCodes.DATA_NO_CHANGE) {
                    LOGGER.info("[Config] long polling result: data no change");
                    continue;
                }

                //预期之外的状态码，退避重试
                LOGGER.error("[Config] long polling result with unexpect code. code = {}", responseCode);
                retryPolicy.fail();
                retryPolicy.executeDelay();
            } catch (Throwable t) {
                LOGGER.error("[Config] long polling failed.", t);
                retryPolicy.fail();
                retryPolicy.executeDelay();
            }
        }
    }

    private List<ConfigFile> assembleWatchConfigFiles() {
        List<ConfigFile> watchConfigFiles = Lists.newArrayList();
        for (Map.Entry<ConfigFileMetadata, RemoteConfigFileRepo> entry : configFilePool.entrySet()) {
            RemoteConfigFileRepo remoteConfigFileRepo = entry.getValue();
            ConfigFileMetadata metadata = remoteConfigFileRepo.getConfigFileMetadata();

            ConfigFile configFile = new ConfigFile(metadata.getNamespace(), metadata.getFileGroup(),
                                                   metadata.getFileName());

            configFile.setVersion(notifiedVersion.get(metadata));

            watchConfigFiles.add(configFile);
        }
        return watchConfigFiles;
    }
}
