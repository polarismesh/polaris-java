/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileGroupConnector;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultRevisableConfigFileGroupPullService implements RevisableConfigFileGroupPullService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRevisableConfigFileGroupPullService.class);

    private final AtomicReference<Boolean> started;
    private final Map<ConfigFileGroupMetadata, RevisableConfigFileGroup> configFileGroupCache;
    private final ExecutorService pullExecutorPool;
    private final ExecutorService taskStarter;
    private final RetryableConfigFileGroupConnector rpcConnector;

    public DefaultRevisableConfigFileGroupPullService(SDKContext sdkContext,
                                                      Map<ConfigFileGroupMetadata, RevisableConfigFileGroup> configFileGroupCache, ConfigFileGroupConnector connector) {
        this.started = new AtomicReference<>(false);
        this.configFileGroupCache = configFileGroupCache;
        this.rpcConnector = new RetryableConfigFileGroupConnector(connector, getPullFailedRetryStrategy());

        NamedThreadFactory threadFactory = new NamedThreadFactory(getClass().getSimpleName());
        int threadNum = sdkContext.getConfig().getConfigFile().getServerConnector().getConfigFileGroupThreadNum();
        this.pullExecutorPool = Executors.newFixedThreadPool(threadNum, threadFactory);
        this.taskStarter = Executors.newSingleThreadExecutor(threadFactory);
    }

    public RetryableConfigFileGroupConnector.RetryableValidator getPullFailedRetryStrategy() {
        return response -> {
            switch (response.getCode()) {
                case ServerCodes.DATA_NO_CHANGE:
                case ServerCodes.EXECUTE_SUCCESS:
                case ServerCodes.NOT_FOUND_RESOURCE:
                    return false;
                default:
                    return true;
            }
        };
    }

    public void pullConfigFileGroup(RevisableConfigFileGroup fileGroup) {
        if (!started.get()) {
            startPullingTask();
        }
    }

    private void startPullingTask() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        try {
            taskStarter.submit(() -> {
                LOGGER.debug("[Config] config file group pulling task start");

                Random random = new Random();
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        TimeUnit.SECONDS.sleep(5 + random.nextInt(5));
                    } catch (InterruptedException e) {
                        //ignore
                    }

                    pullAll();
                }
            });
        } catch (Throwable t) {
            started.set(false);
        }
    }

    private void pullAll() {
        for (Map.Entry<ConfigFileGroupMetadata, RevisableConfigFileGroup> entry :
                configFileGroupCache.entrySet()) {
            ConfigFileGroupMetadata metadata = entry.getKey();
            RevisableConfigFileGroup oldRevisableConfigFileGroup = entry.getValue();
            String oldRevision = oldRevisableConfigFileGroup.getRevision();

            pullExecutorPool.submit(() -> {
                com.tencent.polaris.api.plugin.configuration.ConfigFileGroupMetadata metadataRPCObj = new
                        com.tencent.polaris.api.plugin.configuration.ConfigFileGroupMetadata();
                metadataRPCObj.setFileGroupName(metadata.getFileGroupName());
                metadataRPCObj.setNamespace(metadata.getNamespace());
                com.tencent.polaris.api.plugin.configuration.ConfigFileGroupResponse response =
                        rpcConnector.GetConfigFileMetadataList(metadataRPCObj, oldRevision);
                if (response == null) {
                    LOGGER.debug("[Config] pull empty response. namespace = {}, fileGroupName = {}, oldRevision = {}",
                            metadata.getNamespace(), metadata.getFileGroupName(), oldRevision);
                    return;
                }

                LOGGER.debug("[Config] pull response. namespace = {}, fileGroupName = {}, oldRevision = {}, responseCode = {}",
                        metadata.getNamespace(), metadata.getFileGroupName(), oldRevision, response.getCode());
                switch (response.getCode()) {
                    case ServerCodes.EXECUTE_SUCCESS:
                        com.tencent.polaris.api.plugin.configuration.ConfigFileGroup configFileGroupObj =
                                response.getConfigFileGroup();
                        String newRevision = response.getRevision();
                        List<com.tencent.polaris.api.plugin.configuration.ConfigFile> configFileList =
                                configFileGroupObj.getConfigFileList();

                        List<ConfigFileMetadata> configFileMetadataList = new ArrayList<>();
                        for (ConfigFile configFile : configFileList) {
                            ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata(configFile.getNamespace(),
                                    configFile.getFileGroup(), configFile.getFileName(), configFile.getName());
                            configFileMetadataList.add(configFileMetadata);
                        }
                        if (configFileGroupCache.containsKey(metadata)) {
                            oldRevisableConfigFileGroup.updateConfigFileList(configFileMetadataList, newRevision);
                        }

                        LOGGER.info("[Config] pull result: success. namespace = {}, fileGroupName = {}, oldRevision = {}, newRevision = {}",
                                metadata.getNamespace(), metadata.getFileGroupName(), oldRevision, newRevision);
                        return;
                    case ServerCodes.DATA_NO_CHANGE:
                        LOGGER.debug("[Config] pull result: data no change. namespace = {}, fileGroupName = {}",
                                metadata.getNamespace(), metadata.getFileGroupName());
                        return;
                    case ServerCodes.NOT_FOUND_RESOURCE:
                        LOGGER.warn("[Config] pull result: resource is empty. namespace = {}, fileGroupName = {}",
                                metadata.getNamespace(), metadata.getFileGroupName());
                        if (configFileGroupCache.containsKey(metadata)) {
                            oldRevisableConfigFileGroup.updateConfigFileList(new ArrayList<>(), "");
                        }
                        return;
                    default:
                        // Unreachable
                        return;
                }
            });
        }
    }

}
