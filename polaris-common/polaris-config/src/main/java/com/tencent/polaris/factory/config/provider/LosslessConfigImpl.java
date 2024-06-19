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

package com.tencent.polaris.factory.config.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.provider.LosslessConfig;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

public class LosslessConfigImpl implements LosslessConfig {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private String host;

    @JsonProperty
    private Integer port;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long delayRegisterInterval;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long healthCheckInterval;

    @Override
    public boolean isEnable() {
        return enable;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public long getDelayRegisterInterval() {
        return delayRegisterInterval;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDelayRegisterInterval(long delayRegisterInterval) {
        this.delayRegisterInterval = delayRegisterInterval;
    }

    @Override
    public long getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(long healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public void verify() {
        ConfigUtils.validateString(host, "lossless.host or lossless[?].host");
        ConfigUtils.validateNull(enable,
                "lossless.enable or lossless[?].enable");
        ConfigUtils.validatePositiveInteger(port,
                "lossless.port or lossless[?].port");
        ConfigUtils.validateInterval(delayRegisterInterval,
                "lossless.delayRegisterMilli or lossless[?].delayRegisterMilli");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            LosslessConfig losslessConfig = (LosslessConfig) defaultObject;
            if (null == enable) {
                setEnable(losslessConfig.isEnable());
            }
            if (null == port || 0 == port) {
                setPort(losslessConfig.getPort());
            }
            if (StringUtils.isBlank(host)) {
                setHost(losslessConfig.getHost());
            }
            if (null == delayRegisterInterval) {
                setDelayRegisterInterval(losslessConfig.getDelayRegisterInterval());
            }
            if (null == healthCheckInterval || 0 == healthCheckInterval) {
                setHealthCheckInterval(losslessConfig.getHealthCheckInterval());
            }
        }
    }

    @Override
    public String toString() {
        return "LosslessConfigImpl{" +
                "enable=" + enable +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", delayRegisterInterval=" + delayRegisterInterval +
                ", healthCheckInterval=" + healthCheckInterval +
                '}';
    }
}
