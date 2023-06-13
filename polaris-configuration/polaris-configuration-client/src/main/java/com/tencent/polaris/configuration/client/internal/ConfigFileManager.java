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

import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.plugin.crypto.ConfigFilterCrypto;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.JustForTest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.tencent.polaris.configuration.client.internal.AbstractConfigFileRepo.LOGGER;

/**
 * @author lepdou 2022-03-01
 */
public class ConfigFileManager {

    private SDKContext context;

    private ConfigFilterCrypto crypto;

    private ConfigFileConnector connector;

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
        this.crypto = (ConfigFilterCrypto) sdkContext.getExtensions().getPlugins()
                .getPlugin(PluginTypes.CONFIG_FILTER_CRYPTO.getBaseType(), configFileConnectorType);
        this.connector = (ConfigFileConnector) sdkContext.getExtensions().getPlugins()
                .getPlugin(PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType(), configFileConnectorType);
        this.longPullService = new ConfigFileLongPullService(context, connector);
        try {
            this.persistentHandler = new ConfigFilePersistentHandler(sdkContext);
        } catch (IOException e) {
            LOGGER.warn("config file persist handler init fail:" + e.getMessage(), e);
        }
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

    public ConfigFileResponse createConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile =
                new com.tencent.polaris.api.plugin.configuration.ConfigFile(configFileMetadata.getNamespace(),
                        configFileMetadata.getFileGroup(),
                        configFileMetadata.getFileName());
        configFile.setContent(content);
        return connector.createConfigFile(configFile);
    }

    public ConfigFileResponse updateConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile =
                new com.tencent.polaris.api.plugin.configuration.ConfigFile(configFileMetadata.getNamespace(),
                        configFileMetadata.getFileGroup(),
                        configFileMetadata.getFileName());
        configFile.setContent(content);
        return connector.updateConfigFile(configFile);
    }

    public ConfigFileResponse releaseConfigFile(ConfigFileMetadata configFileMetadata) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile =
                new com.tencent.polaris.api.plugin.configuration.ConfigFile(configFileMetadata.getNamespace(),
                        configFileMetadata.getFileGroup(),
                        configFileMetadata.getFileName());
        return connector.releaseConfigFile(configFile);
    }

    public ConfigFile createConfigFile(ConfigFileMetadata configFileMetadata) {

        ConfigFileRepo configFileRepo = new RemoteConfigFileRepo(context, longPullService, connector, configFileMetadata, persistentHandler);

        return new DefaultConfigFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName(), configFileRepo,
                context.getConfig().getConfigFile());
    }

    public ConfigKVFile createConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat format) {
        ConfigFileRepo configFileRepo = new RemoteConfigFileRepo(context, longPullService, connector, configFileMetadata, persistentHandler);
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

}
