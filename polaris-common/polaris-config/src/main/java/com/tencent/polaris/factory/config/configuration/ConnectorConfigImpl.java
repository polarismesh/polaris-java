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

package com.tencent.polaris.factory.config.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.configuration.ConnectorConfig;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.*;

/**
 * 配置中心连接器配置
 *
 * @author lepdou 2022-03-11
 */
public class ConnectorConfigImpl extends ServerConnectorConfigImpl implements ConnectorConfig {

    @JsonProperty
    private String connectorType;

    @JsonProperty
    private Boolean persistEnable = true;

    @JsonProperty
    private String persistDir;

    @JsonProperty
    private Integer persistMaxWriteRetry = 1;

    @JsonProperty
    private Integer persistMaxReadRetry = 0;

    @JsonProperty
    private Boolean fallbackToLocalCache = true;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long persistRetryInterval = 1000L;

    @JsonProperty
    private Integer configFileGroupThreadNum = 10;

    @JsonProperty
    private Boolean emptyProtectionEnable = true;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long emptyProtectionExpiredInterval = 7 * 24 * 3600 * 1000L;

    @Override
    public void verify() {
        ConfigUtils.validateString(connectorType, "configConnectorType");
        if (StringUtils.isBlank(persistDir)) {
            persistDir = DefaultValues.CONFIG_FILE_DEFAULT_CACHE_PERSIST_DIR;
        }
        if (!StringUtils.equals(connectorType, POLARIS_FILE_CONNECTOR_TYPE)
                && !StringUtils.equals(connectorType, LOCAL_FILE_CONNECTOR_TYPE)
                && !StringUtils.equals(connectorType, CONSUL_FILE_CONNECTOR_TYPE)) {
            throw new IllegalArgumentException(String.format("Unsupported config data source []%s", connectorType));
        }
        if (!LOCAL_FILE_CONNECTOR_TYPE.equals(connectorType)) {
            super.verify();
        }
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject == null) {
            return;
        }
        if (defaultObject instanceof ServerConnectorConfig) {
            ServerConnectorConfig serverConnectorConfig = (ServerConnectorConfig) defaultObject;
            super.setDefault(serverConnectorConfig);
        }
        if (defaultObject instanceof ConnectorConfig) {
            ConnectorConfig connectorConfig = (ConnectorConfig) defaultObject;
            if (connectorType == null) {
                this.connectorType = connectorConfig.getConnectorType();
            }
            if (emptyProtectionEnable == null) {
                this.emptyProtectionEnable = connectorConfig.isEmptyProtectionEnable();
            }
            if (emptyProtectionExpiredInterval == null) {
                this.emptyProtectionExpiredInterval = connectorConfig.getEmptyProtectionExpiredInterval();
            }
        }
    }

    @Override
    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }

    public Boolean getPersistEnable() {
        return persistEnable;
    }

    public void setPersistEnable(Boolean persistEnable) {
        this.persistEnable = persistEnable;
    }

    public String getPersistDir() {
        return persistDir;
    }

    public void setPersistDir(String persistDir) {
        this.persistDir = persistDir;
    }

    public Integer getPersistMaxWriteRetry() {
        return persistMaxWriteRetry;
    }

    public void setPersistMaxWriteRetry(Integer persistMaxWriteRetry) {
        this.persistMaxWriteRetry = persistMaxWriteRetry;
    }

    public Integer getPersistMaxReadRetry() {
        return persistMaxReadRetry;
    }

    public void setPersistMaxReadRetry(Integer persistMaxReadRetry) {
        this.persistMaxReadRetry = persistMaxReadRetry;
    }

    public Long getPersistRetryInterval() {
        return persistRetryInterval;
    }

    public void setPersistRetryInterval(Long persistRetryInterval) {
        this.persistRetryInterval = persistRetryInterval;
    }

    public Boolean getFallbackToLocalCache() {
        return fallbackToLocalCache;
    }

    public void setFallbackToLocalCache(Boolean fallbackToLocalCache) {
        this.fallbackToLocalCache = fallbackToLocalCache;
    }

    public Integer getConfigFileGroupThreadNum() {
        return configFileGroupThreadNum;
    }

    public void setConfigFileGroupThreadNum(Integer configFileGroupThreadNum) {
        this.configFileGroupThreadNum = configFileGroupThreadNum;
    }

    @Override
    public Boolean isEmptyProtectionEnable() {
        return emptyProtectionEnable;
    }

    public void setEmptyProtectionEnable(Boolean emptyProtectionEnable) {
        this.emptyProtectionEnable = emptyProtectionEnable;
    }

    @Override
    public Long getEmptyProtectionExpiredInterval() {
        return emptyProtectionExpiredInterval;
    }

    public void setEmptyProtectionExpiredInterval(Long emptyProtectionExpiredInterval) {
        this.emptyProtectionExpiredInterval = emptyProtectionExpiredInterval;
    }
}
