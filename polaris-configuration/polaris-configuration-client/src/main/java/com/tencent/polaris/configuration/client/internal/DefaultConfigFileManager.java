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

import com.google.common.collect.Maps;

import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.factory.ConfigFileFactory;
import com.tencent.polaris.configuration.client.factory.ConfigFileFactoryManager;
import com.tencent.polaris.configuration.client.factory.ConfigFilePublishFactory;
import com.tencent.polaris.configuration.client.factory.DefaultConfigFileFactoryManager;

import java.util.Map;

/**
 *
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileManager implements ConfigFileManager {

    private static DefaultConfigFileManager instance;

    private ConfigFileFactoryManager configFileFactoryManager;

    private final Map<ConfigFileMetadata, ConfigFile>   configFileCache           = Maps.newConcurrentMap();
    private final Map<ConfigFileMetadata, ConfigKVFile> configPropertiesFileCache = Maps.newConcurrentMap();

    private DefaultConfigFileManager(SDKContext sdkContext) {
        configFileFactoryManager = DefaultConfigFileFactoryManager.getInstance(sdkContext);
    }

    public static DefaultConfigFileManager getInstance(SDKContext sdkContext) {
        if (instance == null) {
            synchronized (DefaultConfigFileManager.class) {
                if (instance == null) {
                    instance = new DefaultConfigFileManager(sdkContext);
                }
            }
        }
        return instance;
    }

    @Override
    public ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFile configFile = configFileCache.get(configFileMetadata);

        if (configFile == null) {
            synchronized (this) {
                configFile = configFileCache.get(configFileMetadata);
                if (configFile == null) {
                    ConfigFileFactory configFileFactory = configFileFactoryManager.getConfigFileFactory(configFileMetadata);

                    configFile = configFileFactory.createConfigFile(configFileMetadata);

                    configFileCache.put(configFileMetadata, configFile);
                }
            }
        }

        return configFile;
    }

    @Override
    public ConfigKVFile getConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat fileFormat) {
        ConfigKVFile configFile = configPropertiesFileCache.get(configFileMetadata);

        if (configFile == null) {
            synchronized (this) {
                configFile = configPropertiesFileCache.get(configFileMetadata);
                if (configFile == null) {
                    ConfigFileFactory configFileFactory = configFileFactoryManager.getConfigFileFactory(configFileMetadata);

                    configFile = configFileFactory.createConfigKVFile(configFileMetadata, fileFormat);

                    configPropertiesFileCache.put(configFileMetadata, configFile);
                }
            }
        }

        return configFile;
    }

    @Override
    public ConfigFileResponse createConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        ConfigFilePublishFactory configFilePublishFactory = configFileFactoryManager.getConfigFilePublishFactory(configFileMetadata);
        return configFilePublishFactory.createConfigFile(configFileMetadata, content);
    }

    @Override
    public ConfigFileResponse updateConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        ConfigFilePublishFactory configFilePublishFactory = configFileFactoryManager.getConfigFilePublishFactory(configFileMetadata);
        return configFilePublishFactory.updateConfigFile(configFileMetadata, content);
    }

    @Override
    public ConfigFileResponse releaseConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFilePublishFactory configFilePublishFactory = configFileFactoryManager.getConfigFilePublishFactory(configFileMetadata);
        return configFilePublishFactory.releaseConfigFile(configFileMetadata);
    }

    void setConfigFileFactoryManager(ConfigFileFactoryManager configFileFactoryManager) {
        this.configFileFactoryManager = configFileFactoryManager;
    }

}
