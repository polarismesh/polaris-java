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
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

/**
 * @author fabian4 2022-03-08
 */
public class DefaultConfigFilePublishFactory implements ConfigFilePublishFactory {

    private static DefaultConfigFilePublishFactory instance;

    private final SDKContext sdkContext;

    private final ConfigFileConnector configFileConnector;

    public DefaultConfigFilePublishFactory(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
        String configFileConnectorType = sdkContext.getConfig().getConfigFile().getServerConnector()
                .getConnectorType();
        this.configFileConnector = (ConfigFileConnector) sdkContext.getExtensions().getPlugins()
                .getPlugin(PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType(), configFileConnectorType);
    }

    public static DefaultConfigFilePublishFactory getInstance(SDKContext sdkContext) {
        if (instance == null) {
            synchronized (DefaultConfigFileFactory.class) {
                if (instance == null) {
                    instance = new DefaultConfigFilePublishFactory(sdkContext);
                }
            }
        }
        return instance;
    }

    @Override
    public void createConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent(content);
        configFileConnector.createConfigFile(configFile);
    }

    @Override
    public void updateConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent(content);
        configFileConnector.updateConfigFile(configFile);
    }

    @Override
    public void upsertConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent(content);
        configFileConnector.upsertConfigFile(configFile);
    }

    @Override
    public void releaseConfigFile(ConfigFileMetadata configFileMetadata) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile = new ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFileConnector.releaseConfigFile(configFile);
    }
}
