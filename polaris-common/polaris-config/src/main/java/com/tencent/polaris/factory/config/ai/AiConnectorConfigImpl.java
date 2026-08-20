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

package com.tencent.polaris.factory.config.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.ai.AiConnectorConfig;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

/**
 * Implementation of Skill connector configuration.
 */
public class AiConnectorConfigImpl extends ServerConnectorConfigImpl implements AiConnectorConfig {

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
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long persistRetryInterval = 1000L;

    @JsonProperty
    private Boolean fallbackToLocalCache = true;

    @Override
    public void verify() {
        ConfigUtils.validateString(connectorType, "aiConnectorType");
        if (StringUtils.isBlank(persistDir)) {
            persistDir = DefaultValues.SKILL_DEFAULT_CACHE_PERSIST_DIR;
        }
        if (!StringUtils.equals(connectorType, DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE)) {
            throw new IllegalArgumentException(String.format("Unsupported skill data source [%s]", connectorType));
        }
        super.verify();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject == null) {
            return;
        }
        if (defaultObject instanceof ServerConnectorConfig) {
            super.setDefault(defaultObject);
        }
        if (defaultObject instanceof AiConnectorConfig) {
            AiConnectorConfig connectorConfig = (AiConnectorConfig) defaultObject;
            if (connectorType == null) {
                this.connectorType = connectorConfig.getConnectorType();
            }
            if (persistEnable == null) {
                this.persistEnable = connectorConfig.getPersistEnable();
            }
            if (StringUtils.isBlank(persistDir)) {
                this.persistDir = connectorConfig.getPersistDir();
            }
            if (persistMaxWriteRetry == null) {
                this.persistMaxWriteRetry = connectorConfig.getPersistMaxWriteRetry();
            }
            if (persistMaxReadRetry == null) {
                this.persistMaxReadRetry = connectorConfig.getPersistMaxReadRetry();
            }
            if (persistRetryInterval == null) {
                this.persistRetryInterval = connectorConfig.getPersistRetryInterval();
            }
            if (fallbackToLocalCache == null) {
                this.fallbackToLocalCache = connectorConfig.getFallbackToLocalCache();
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

    @Override
    public Boolean getPersistEnable() {
        return persistEnable;
    }

    public void setPersistEnable(Boolean persistEnable) {
        this.persistEnable = persistEnable;
    }

    @Override
    public String getPersistDir() {
        return persistDir;
    }

    public void setPersistDir(String persistDir) {
        this.persistDir = persistDir;
    }

    @Override
    public Integer getPersistMaxWriteRetry() {
        return persistMaxWriteRetry;
    }

    public void setPersistMaxWriteRetry(Integer persistMaxWriteRetry) {
        this.persistMaxWriteRetry = persistMaxWriteRetry;
    }

    @Override
    public Integer getPersistMaxReadRetry() {
        return persistMaxReadRetry;
    }

    public void setPersistMaxReadRetry(Integer persistMaxReadRetry) {
        this.persistMaxReadRetry = persistMaxReadRetry;
    }

    @Override
    public Long getPersistRetryInterval() {
        return persistRetryInterval;
    }

    public void setPersistRetryInterval(Long persistRetryInterval) {
        this.persistRetryInterval = persistRetryInterval;
    }

    @Override
    public Boolean getFallbackToLocalCache() {
        return fallbackToLocalCache;
    }

    public void setFallbackToLocalCache(Boolean fallbackToLocalCache) {
        this.fallbackToLocalCache = fallbackToLocalCache;
    }
}
