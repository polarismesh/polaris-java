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

import com.google.common.collect.Maps;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.client.JustForTest;

import java.util.Map;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileFactoryManager implements ConfigFileFactoryManager {

    private final SDKContext sdkContext;
    private ConfigFileFactory defaultConfigFileFactory;
    private ConfigFilePublishFactory defaultConfigFilePublishFactory;

    private final Map<ConfigFileMetadata, ConfigFileFactory> configFileFactories = Maps.newConcurrentMap();

    private final Map<ConfigFileMetadata, ConfigFilePublishFactory> configFilePublishFactories = Maps.newConcurrentMap();

    private static DefaultConfigFileFactoryManager instance;

    private DefaultConfigFileFactoryManager(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
    }

    public static DefaultConfigFileFactoryManager getInstance(SDKContext sdkContext) {
        if (instance == null) {
            synchronized (DefaultConfigFileFactoryManager.class) {
                if (instance == null) {
                    instance = new DefaultConfigFileFactoryManager(sdkContext);
                }
            }
        }
        return instance;
    }

    @Override
    public ConfigFileFactory getConfigFileFactory(ConfigFileMetadata configFileMetadata) {
        ConfigFileFactory factory = configFileFactories.get(configFileMetadata);

        if (factory != null) {
            return factory;
        }

        if (defaultConfigFileFactory == null) {
            defaultConfigFileFactory = DefaultConfigFileFactory.getInstance(sdkContext);
        }

        configFileFactories.put(configFileMetadata, defaultConfigFileFactory);

        return defaultConfigFileFactory;
    }

    @Override
    public ConfigFilePublishFactory getConfigFilePublishFactory(ConfigFileMetadata configFileMetadata) {
        ConfigFilePublishFactory factory = configFilePublishFactories.get(configFileMetadata);

        if (factory != null) {
            return factory;
        }

        if (defaultConfigFilePublishFactory == null) {
            defaultConfigFilePublishFactory = DefaultConfigFilePublishFactory.getInstance(sdkContext);
        }

        configFilePublishFactories.put(configFileMetadata, defaultConfigFilePublishFactory);

        return defaultConfigFilePublishFactory;
    }

    @JustForTest
    void setDefaultConfigFileFactory(ConfigFileFactory factory) {
        this.defaultConfigFileFactory = factory;
    }

}
