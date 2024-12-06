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

package com.tencent.polaris.factory.config.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * @author lepdou 2022-03-02
 */
public class ConfigFileConfigImpl implements ConfigFileConfig {

    @JsonProperty
    private ConnectorConfigImpl serverConnector;
    @JsonProperty
    private ConfigFilterConfigImpl configFilter;
    @JsonProperty
    private int propertiesValueCacheSize;
    @JsonProperty
    private long propertiesValueExpireTime;

    @Override
    public void verify() {
        ConfigUtils.validateNull(serverConnector, "config server connector");
        serverConnector.verify();
        configFilter.verify();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject != null) {
            ConfigFileConfig sourceConfig = (ConfigFileConfig) defaultObject;
            if (serverConnector == null) {
                serverConnector = new ConnectorConfigImpl();
            }
            if (configFilter == null) {
                configFilter = new ConfigFilterConfigImpl();
            }
            serverConnector.setDefault(sourceConfig.getServerConnector());
            configFilter.setDefault(sourceConfig.getConfigFilterConfig());
            propertiesValueCacheSize = sourceConfig.getPropertiesValueCacheSize();
            propertiesValueExpireTime = sourceConfig.getPropertiesValueExpireTime();
        }
    }

    @Override
    public ConnectorConfigImpl getServerConnector() {
        return serverConnector;
    }

    @Override
    public ConfigFilterConfigImpl getConfigFilterConfig() {
        return configFilter;
    }

    @Override
    public int getPropertiesValueCacheSize() {
        return propertiesValueCacheSize;
    }

    @Override
    public long getPropertiesValueExpireTime() {
        return propertiesValueExpireTime;
    }

    public void setServerConnector(ConnectorConfigImpl serverConnector) {
        this.serverConnector = serverConnector;
    }

    public void setPropertiesValueCacheSize(int propertiesValueCacheSize) {
        this.propertiesValueCacheSize = propertiesValueCacheSize;
    }

    public void setPropertiesValueExpireTime(long propertiesValueExpireTime) {
        this.propertiesValueExpireTime = propertiesValueExpireTime;
    }
}
