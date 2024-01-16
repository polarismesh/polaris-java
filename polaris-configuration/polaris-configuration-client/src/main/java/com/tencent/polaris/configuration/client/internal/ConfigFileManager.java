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

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.plugin.configuration.ConfigPublishFile;
import com.tencent.polaris.api.plugin.filter.ConfigFileFilterChain;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.configuration.api.rpc.ConfigPublishRequest;
import com.tencent.polaris.configuration.api.rpc.CreateConfigFileRequest;
import com.tencent.polaris.configuration.api.rpc.ReleaseConfigFileRequest;
import com.tencent.polaris.configuration.api.rpc.UpdateConfigFileRequest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.polaris.configuration.client.internal.AbstractConfigFileRepo.LOGGER;

/**
 * @author lepdou 2022-03-01
 */
public class ConfigFileManager {

    private SDKContext context;

    private ConfigFileConnector connector;

    private ConfigFileFilterChain configFileFilterChain;

    private final Map<ConfigFileMetadata, ConfigFile> configFileCache = new ConcurrentHashMap<>();

    private final Map<ConfigFileMetadata, ConfigKVFile> configPropertiesFileCache = new ConcurrentHashMap<>();

    private ConfigFileLongPullService longPullService;

    private ConfigFilePersistentHandler persistentHandler;

    @JustForTest
    public ConfigFileManager() {

    }

    @JustForTest
    public ConfigFileManager(ConfigFileConnector connector) {
        this.connector = connector;
    }

    public ConfigFileManager(SDKContext sdkContext) {
        String configFileConnectorType = sdkContext.getConfig().getConfigFile().getServerConnector()
                .getConnectorType();
        this.context = sdkContext;
        this.configFileFilterChain = new ConfigFileFilterChain(sdkContext.getExtensions().getPlugins(),
                sdkContext.getConfig().getConfigFile().getConfigFilterConfig());
        this.connector = (ConfigFileConnector) sdkContext.getExtensions().getPlugins()
                .getPlugin(PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType(), configFileConnectorType);
        this.longPullService = new ConfigFileLongPullService(context, connector);
        try {
            this.persistentHandler = new ConfigFilePersistentHandler(sdkContext);
        } catch (IOException e) {
            LOGGER.warn("config file persist handler init fail:" + e.getMessage(), e);
        }
        //deal with the situation when the thread can't exit
        registerDestroyHook(context);
    }

    public ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFile configFile = configFileCache.get(configFileMetadata);
        if (configFile == null) {
            synchronized (this) {
                configFile = configFileCache.get(configFileMetadata);
                if (configFile == null) {
                    configFile = createConfigFile(configFileMetadata);
                    configFileCache.put(configFileMetadata, configFile);
                }
            }
        }
        return configFile;
    }

    public ConfigKVFile getConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat fileFormat) {
        ConfigKVFile configFile = configPropertiesFileCache.get(configFileMetadata);
        if (configFile == null) {
            synchronized (this) {
                configFile = configPropertiesFileCache.get(configFileMetadata);
                if (configFile == null) {
                    configFile = createConfigKVFile(configFileMetadata, fileFormat);
                    configPropertiesFileCache.put(configFileMetadata, configFile);
                }
            }
        }

        return configFile;
    }

    public ConfigFileResponse createConfigFile(CreateConfigFileRequest request) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile =
                new com.tencent.polaris.api.plugin.configuration.ConfigFile(request.getNamespace(),
                        request.getGroup(),
                        request.getFilename());
        configFile.setContent(request.getContent());
        return connector.createConfigFile(configFile);
    }

    public ConfigFileResponse updateConfigFile(UpdateConfigFileRequest request) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile =
                new com.tencent.polaris.api.plugin.configuration.ConfigFile(request.getNamespace(),
                        request.getGroup(),
                        request.getFilename());
        configFile.setContent(request.getContent());
        return connector.updateConfigFile(configFile);
    }

    public ConfigFileResponse releaseConfigFile(ReleaseConfigFileRequest request) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile =
                new com.tencent.polaris.api.plugin.configuration.ConfigFile(request.getNamespace(),
                        request.getGroup(),
                        request.getFilename());
        return connector.releaseConfigFile(configFile);
    }

    public ConfigFileResponse upsertAndPublish(ConfigPublishRequest request) {
        ConfigPublishFile publishFile = new ConfigPublishFile();
        publishFile.setReleaseName(request.getReleaseName());
        publishFile.setNamespace(request.getNamespace());
        publishFile.setFileGroup(request.getGroup());
        publishFile.setFileName(request.getFilename());
        publishFile.setMd5(request.getCasMd5());
        publishFile.setContent(request.getContent());
        publishFile.setLabels(request.getLabels());
        return connector.upsertAndPublishConfigFile(publishFile);
    }


    public ConfigFile createConfigFile(ConfigFileMetadata configFileMetadata) {

        ConfigFileRepo configFileRepo = new RemoteConfigFileRepo(context, longPullService, configFileFilterChain,
                connector, configFileMetadata, persistentHandler);

        return new DefaultConfigFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName(), configFileRepo,
                context.getConfig().getConfigFile());
    }

    public ConfigKVFile createConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat format) {
        ConfigFileRepo configFileRepo = new RemoteConfigFileRepo(context, longPullService, configFileFilterChain,
                connector, configFileMetadata, persistentHandler);
        switch (format) {
            case Properties: {
                return new ConfigPropertiesFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                        configFileMetadata.getFileName(), configFileRepo,
                        context.getConfig().getConfigFile());
            }
            case Yaml: {
                return new ConfigYamlFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                        configFileMetadata.getFileName(), configFileRepo,
                        context.getConfig().getConfigFile());
            }
            default:
                throw new IllegalArgumentException("KV file only support properties and yaml file.");
        }
    }

    private void registerDestroyHook(SDKContext context) {
        context.registerDestroyHook(new Destroyable() {
            @Override
            protected void doDestroy() {
                longPullService.doLongPullingDestroy();
                persistentHandler.doDestroy();
            }
        });
    }
}
