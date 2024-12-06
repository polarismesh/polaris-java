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

package com.tencent.polaris.configuration.client;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.flow.ConfigFileFlow;
import com.tencent.polaris.configuration.api.flow.ConfigFileGroupFlow;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileGroupMetadata;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import com.tencent.polaris.configuration.client.util.ConfigFileUtils;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileService extends BaseEngine implements ConfigFileService {

    private ConfigFileFlow configFileFlow;

    private ConfigFileGroupFlow configFileGroupFlow;

    public DefaultConfigFileService(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() throws PolarisException {
        if (configFileFlow == null) {
            configFileFlow = sdkContext.getOrInitFlow(ConfigFileFlow.class);
            configFileGroupFlow = sdkContext.getOrInitFlow(ConfigFileGroupFlow.class);
        }
    }

    @Override
    public ConfigKVFile getConfigPropertiesFile(String namespace, String fileGroup, String fileName) {
        return getConfigPropertiesFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName));
    }

    @Override
    public ConfigKVFile getConfigPropertiesFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        return configFileFlow.getConfigPropertiesFile(configFileMetadata);
    }

    @Override
    public ConfigKVFile getConfigYamlFile(String namespace, String fileGroup, String fileName) {
        return getConfigYamlFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName));
    }

    @Override
    public ConfigKVFile getConfigYamlFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        return configFileFlow.getConfigYamlFile(configFileMetadata);
    }

    @Override
    public ConfigFile getConfigFile(String namespace, String fileGroup, String fileName) {
        return getConfigFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName));
    }

    @Override
    public ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        return configFileFlow.getConfigTextFile(configFileMetadata);
    }

    @Override
    public ConfigFileGroup getConfigFileGroup(String namespace, String fileGroup) {
        return getConfigFileGroup(new DefaultConfigFileGroupMetadata(namespace, fileGroup));
    }

    @Override
    public ConfigFileGroup getConfigFileGroup(ConfigFileGroupMetadata configFileGroupMetadata) {
        return configFileGroupFlow.getConfigFileGroup(configFileGroupMetadata);
    }

    @JustForTest
    void setConfigFileFlow(ConfigFileFlow configFileFlow) {
        this.configFileFlow = configFileFlow;
    }
}
