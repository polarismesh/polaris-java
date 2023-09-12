/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileGroupConnector;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigFileGroupManager {
    private final Map<ConfigFileGroupMetadata, RevisableConfigFileGroup> configFileGroupCache =
            new ConcurrentHashMap<>();
    private RetryableConfigFileGroupConnector rpcConnector;
    private RevisableConfigFileGroupPullService configFileGroupPullService;

    private final boolean enabled;

    public ConfigFileGroupManager(SDKContext sdkContext) {
        String configFileConnectorType = sdkContext.getConfig().getConfigFile().getServerConnector()
                .getConnectorType();
        if (configFileConnectorType.equals("polaris")) {
            enabled = true;
            ConfigFileGroupConnector connector = (ConfigFileGroupConnector) sdkContext.getExtensions().getPlugins()
                    .getPlugin(PluginTypes.CONFIG_FILE_GROUP_CONNECTOR.getBaseType(), configFileConnectorType);
            this.rpcConnector = new RetryableConfigFileGroupConnector(connector, getCacheMissedRetryStrategy());
            this.configFileGroupPullService = new DefaultRevisableConfigFileGroupPullService(sdkContext,
                    configFileGroupCache, connector);
        } else {
            enabled = false;
        }
    }

    public RetryableConfigFileGroupConnector.RetryableValidator getCacheMissedRetryStrategy() {
        return response -> {
            switch (response.getCode()) {
                case ServerCodes.NOT_FOUND_RESOURCE:
                case ServerCodes.EXECUTE_SUCCESS:
                    return false;
                default:
                    return true;
            }
        };
    }

    public ConfigFileGroup getConfigFileGroup(ConfigFileGroupMetadata metadata) {
        if (!enabled) {
            throw new RuntimeException("Config file group manager is disabled.");
        }
        RevisableConfigFileGroup configFileGroup = configFileGroupCache.get(metadata);
        if (configFileGroup == null) {
            synchronized (this) {
                configFileGroup = configFileGroupCache.get(metadata);
                if (configFileGroup == null) {
                    configFileGroup = getConfigFileGroupFromRemote(metadata, "");
                    if (configFileGroup == null) {
                        return null;
                    }
                }
            }
        }
        return configFileGroup;
    }

    private RevisableConfigFileGroup getConfigFileGroupFromRemote(ConfigFileGroupMetadata metadata,
                                                                  String currentRevision) {
        com.tencent.polaris.api.plugin.configuration.ConfigFileGroupMetadata metadataRPCObj = new
                com.tencent.polaris.api.plugin.configuration.ConfigFileGroupMetadata();
        metadataRPCObj.setFileGroupName(metadata.getFileGroupName());
        metadataRPCObj.setNamespace(metadata.getNamespace());
        com.tencent.polaris.api.plugin.configuration.ConfigFileGroupResponse rpcResponse =
                rpcConnector.GetConfigFileMetadataList(metadataRPCObj, currentRevision);
        if (rpcResponse == null) {
            return null;
        }

        switch (rpcResponse.getCode()) {
            case ServerCodes.EXECUTE_SUCCESS: {
                com.tencent.polaris.api.plugin.configuration.ConfigFileGroup configFileGroupObj =
                        rpcResponse.getConfigFileGroup();
                String newRevision = rpcResponse.getRevision();
                List<com.tencent.polaris.api.plugin.configuration.ConfigFile> configFileList =
                        configFileGroupObj.getConfigFileList();

                List<ConfigFileMetadata> configFileMetadataList = new ArrayList<>();
                for (ConfigFile configFile : configFileList) {
                    ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata(configFile.getNamespace(),
                            configFile.getFileGroup(), configFile.getFileName());
                    configFileMetadataList.add(configFileMetadata);
                }
                ConfigFileGroup configFileGroup = new DefaultConfigFileGroup(configFileGroupObj.getNamespace(),
                        configFileGroupObj.getFileGroupName(), configFileMetadataList);
                RevisableConfigFileGroup revisableConfigFileGroup = new RevisableConfigFileGroup(configFileGroup,
                        newRevision);
                cache(metadata, revisableConfigFileGroup);
                return revisableConfigFileGroup;
            }
            case ServerCodes.NOT_FOUND_RESOURCE: {
                ConfigFileGroup emptyConfigFileGroup = new DefaultConfigFileGroup(metadata.getNamespace(),
                        metadata.getFileGroupName(), new ArrayList<>());
                RevisableConfigFileGroup emptyRevision = new RevisableConfigFileGroup(emptyConfigFileGroup, "");
                cache(metadata, emptyRevision);
                return emptyRevision;
            }
            default:
                return null;
        }
    }

    private void cache(ConfigFileGroupMetadata metadata, RevisableConfigFileGroup revisableConfigFileGroup) {
        configFileGroupCache.put(metadata, revisableConfigFileGroup);
        configFileGroupPullService.pullConfigFileGroup(revisableConfigFileGroup);
    }

    private void invalid(ConfigFileGroupMetadata metadata) {
        configFileGroupCache.remove(metadata);
    }

    public void setRpcConnector(RetryableConfigFileGroupConnector rpcConnector) {
        this.rpcConnector = rpcConnector;
    }

    public void setConfigFileGroupPullService(RevisableConfigFileGroupPullService configFileGroupPullService) {
        this.configFileGroupPullService = configFileGroupPullService;
    }
}
