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

package com.tencent.polaris.configuration.api.flow;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.AbstractFlow;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import java.util.ServiceLoader;

public interface ConfigFileFlow extends AbstractFlow {

    default ConfigFile getConfigTextFile(ConfigFileMetadata configFileMetadata) {
        return null;
    }

    default ConfigKVFile getConfigPropertiesFile(ConfigFileMetadata configFileMetadata) {
        return null;
    }

    default ConfigKVFile getConfigYamlFile(ConfigFileMetadata configFileMetadata) {
        return null;
    }

    static ConfigFileFlow loadConfigFileFlow(String name) {
        ServiceLoader<ConfigFileFlow> flows = ServiceLoader.load(ConfigFileFlow.class);
        for (ConfigFileFlow flow : flows) {
            if (StringUtils.equals(flow.getName(), name)) {
                return flow;
            }
        }
        throw new PolarisException(ErrorCode.INVALID_CONFIG,
                String.format("unknown flow name %s, type %s", name, ConfigFileFlow.class.getCanonicalName()));
    }
}
