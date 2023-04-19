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

package com.tencent.polaris.configuration.client.flow;

import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.api.flow.ConfigFileFlow;
import com.tencent.polaris.configuration.client.internal.ConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileManager;

public class DefaultConfigFileFlow implements ConfigFileFlow {

    private ConfigFileManager configFileManager;


    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        setConfigFileManager(DefaultConfigFileManager.getInstance(sdkContext));
    }

    public void setConfigFileManager(ConfigFileManager configFileManager) {
        this.configFileManager = configFileManager;
    }

    @Override
    public ConfigFile getConfigTextFile(ConfigFileMetadata configFileMetadata) {
        return configFileManager.getConfigFile(configFileMetadata);
    }

    @Override
    public ConfigKVFile getConfigPropertiesFile(ConfigFileMetadata configFileMetadata) {
        return configFileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
    }

    @Override
    public ConfigKVFile getConfigYamlFile(ConfigFileMetadata configFileMetadata) {
        return configFileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Yaml);
    }
}
