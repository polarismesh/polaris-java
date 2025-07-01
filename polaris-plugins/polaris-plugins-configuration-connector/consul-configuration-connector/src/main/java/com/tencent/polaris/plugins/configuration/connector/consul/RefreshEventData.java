/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugins.configuration.connector.consul;

import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;

import java.util.Objects;

/**
 * @author Haotian Zhang
 */
public class RefreshEventData {
    private final String keyPrefix;

    private final ConfigFile configFile;

    private final ConfigFileResponse configFileResponse;

    public RefreshEventData(String keyPrefix, ConfigFile configFile, ConfigFileResponse configFileResponse) {
        this.keyPrefix = keyPrefix;
        this.configFile = configFile;
        this.configFileResponse = configFileResponse;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public ConfigFile getConfigFile() {
        return configFile;
    }

    public ConfigFileResponse getConfigFileResponse() {
        return configFileResponse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshEventData that = (RefreshEventData) o;
        return Objects.equals(keyPrefix, that.keyPrefix) && Objects.equals(configFile, that.configFile) && Objects.equals(configFileResponse, that.configFileResponse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyPrefix, configFile, configFileResponse);
    }

    @Override
    public String toString() {
        return "RefreshEventData{" +
                "keyPrefix='" + keyPrefix + '\'' +
                ", configFile=" + configFile +
                ", configFileResponse=" + configFileResponse +
                '}';
    }
}
