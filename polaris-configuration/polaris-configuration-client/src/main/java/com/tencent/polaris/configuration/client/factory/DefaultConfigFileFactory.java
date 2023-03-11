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

package com.tencent.polaris.configuration.client.factory;

import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.ConfigFileLongPollingService;
import com.tencent.polaris.configuration.client.internal.ConfigFileRepo;
import com.tencent.polaris.configuration.client.internal.ConfigPropertiesFile;
import com.tencent.polaris.configuration.client.internal.ConfigYamlFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileLongPollingService;
import com.tencent.polaris.configuration.client.internal.RemoteConfigFileRepo;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileFactory implements ConfigFileFactory {

    private static DefaultConfigFileFactory instance;

    private final SDKContext sdkContext;

    private final ConfigFileConnector configFileConnector;

    private final ConfigFileLongPollingService configFileLongPollingService;

    private DefaultConfigFileFactory(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
        this.configFileLongPollingService = DefaultConfigFileLongPollingService.getInstance(sdkContext);
        String configFileConnectorType = sdkContext.getConfig().getConfigFile().getServerConnector()
                .getConnectorType();
        this.configFileConnector = (ConfigFileConnector) sdkContext.getExtensions().getPlugins()
                .getPlugin(PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType(), configFileConnectorType);
    }

    public static DefaultConfigFileFactory getInstance(SDKContext sdkContext) {
        if (instance == null) {
            synchronized (DefaultConfigFileFactory.class) {
                if (instance == null) {
                    instance = new DefaultConfigFileFactory(sdkContext);
                }
            }
        }
        return instance;
    }

    @Override
    public ConfigFile createConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileRepo configFileRepo = createConfigFileRepo(configFileMetadata);

        return new DefaultConfigFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                                     configFileMetadata.getFileName(), configFileRepo,
                                     sdkContext.getConfig().getConfigFile());
    }

    @Override
    public ConfigKVFile createConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat format) {
        ConfigFileRepo configFileRepo = createConfigFileRepo(configFileMetadata);
        switch (format) {
            case Properties: {
                return new ConfigPropertiesFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                                                configFileMetadata.getFileName(), configFileRepo,
                                                sdkContext.getConfig().getConfigFile());
            }
            case Yaml: {
                return new ConfigYamlFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                                          configFileMetadata.getFileName(), configFileRepo,
                                          sdkContext.getConfig().getConfigFile());
            }
            default:
                throw new IllegalArgumentException("KV file only support properties and yaml file.");
        }
    }

    private ConfigFileRepo createConfigFileRepo(ConfigFileMetadata configFileMetadata) {
        return new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, null, configFileMetadata);
    }
}
