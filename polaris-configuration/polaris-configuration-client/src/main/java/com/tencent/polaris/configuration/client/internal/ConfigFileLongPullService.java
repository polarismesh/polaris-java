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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author lepdou 2022-03-02
 */
public class ConfigFileLongPullService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileLongPullService.class);

    /**
     *
     */
    private final ConfigFileConnector connector;

    /**
     *
     */
    private final ExecutorService longPollingService;

    /**
     *
     */
    private final Map<ConfigFileMetadata, RemoteConfigFileRepo> configFilePool;

    /**
     * 此处只缓存所有配置文件对应通知的版本号，跟远端拉取的配置文件返回的版本号没有关系
     */
    private final Map<ConfigFileMetadata, Long> notifiedVersion;

    /**
     *
     */
    private final AtomicReference<Boolean> started;

    /**
     * 设置是否继续执行长轮询 flag, 解决主动退出长轮询时候线程退出问题
     */
    private final AtomicBoolean isLongPullingStopped;

    /**
     *
     */
    private final RetryPolicy retryPolicy;

    public ConfigFileLongPullService(SDKContext sdkContext, ConfigFileConnector configFileConnector) {
        isLongPullingStopped = new AtomicBoolean(false);
        this.started = new AtomicReference<>(false);
        this.configFilePool = Maps.newConcurrentMap();
        this.notifiedVersion = Maps.newConcurrentMap();
        this.retryPolicy = new ExponentialRetryPolicy(1, 120);
        this.connector = configFileConnector;
        //初始化 long polling 线程池
        NamedThreadFactory threadFactory = new NamedThreadFactory("Configuration-LongPolling");
        this.longPollingService = Executors.newSingleThreadExecutor(threadFactory);
    }

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
        while (!isLongPullingStopped.get() && !Thread.currentThread().isInterrupted()) {
            try {
                List<ConfigFile> watchConfigFiles = assembleWatchConfigFiles();
                LOGGER.debug("[Config] do long polling. config file size = {}, delay time = {}", watchConfigFiles.size(),
                        retryPolicy.getCurrentDelayTime());

                ConfigFileResponse response = connector.watchConfigFiles(watchConfigFiles);

                retryPolicy.success();

                int responseCode = response.getCode();
                //感知到配置文件发布事件
                if (responseCode == ServerCodes.EXECUTE_SUCCESS && response.getConfigFile() != null) {
                    ConfigFile changedConfigFile = response.getConfigFile();
                    ConfigFileMetadata metadata = new DefaultConfigFileMetadata(changedConfigFile.getNamespace(),
                            changedConfigFile.getFileGroup(),
                            changedConfigFile.getFileName());
                    long newNotifiedVersion = changedConfigFile.getVersion();
                    long oldNotifiedVersion = notifiedVersion.getOrDefault(metadata, -1L);

                    long maxVersion = newNotifiedVersion;
                    if (connector.isNotifiedVersionIncreaseStrictly()) {
                        maxVersion = Math.max(newNotifiedVersion, oldNotifiedVersion);
                    }

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

    public void stopLongPulling() {
        this.isLongPullingStopped.compareAndSet(false, true);
    }

    public void doLongPullingDestroy() {
        stopLongPulling();
        if (longPollingService != null) {
            ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{longPollingService});
        }
    }
}
