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
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;

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
    // plugin name, such as default
    @JsonProperty
    private String type;

    @JsonProperty
    private LosslessProto.DelayRegister.DelayStrategy strategy;

    @JsonProperty
    private String healthCheckProtocol;

    @JsonProperty
    private String healthCheckPath;

    @JsonProperty
    private String healthCheckMethod = "GET";

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

    @Override
    public String getType() {
        return type;
    }

    @Override
    public LosslessProto.DelayRegister.DelayStrategy getStrategy() {
        return strategy;
    }

    @Override
    public String getHealthCheckProtocol() {
        return healthCheckProtocol;
    }

    @Override
    public String getHealthCheckPath() {
        return healthCheckPath;
    }

    @Override
    public String getHealthCheckMethod() {
        return healthCheckMethod;
    }

    public void setHealthCheckInterval(long healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setHealthCheckProtocol(String healthCheckProtocol) {
        this.healthCheckProtocol = healthCheckProtocol;
    }

    public void setHealthCheckPath(String healthCheckPath) {
        this.healthCheckPath = healthCheckPath;
    }

    public void setHealthCheckMethod(String healthCheckMethod) {
        this.healthCheckMethod = healthCheckMethod;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setStrategy(LosslessProto.DelayRegister.DelayStrategy strategy) {
        this.strategy = strategy;
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
        ConfigUtils.validateNull(strategy, "lossless.strategy or lossless[?].strategy");
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
            if (StringUtils.isBlank(type)) {
                setType(losslessConfig.getType());
            }
            if (null == strategy) {
                setStrategy(losslessConfig.getStrategy());
            }
            if (StringUtils.isBlank(healthCheckProtocol)) {
                setHealthCheckProtocol(losslessConfig.getHealthCheckProtocol());
            }
            if (StringUtils.isBlank(healthCheckPath)) {
                setHealthCheckPath(losslessConfig.getHealthCheckPath());
            }
            if (StringUtils.isBlank(healthCheckMethod)) {
                setHealthCheckMethod(losslessConfig.getHealthCheckMethod());
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
                ", type='" + type + '\'' +
                ", strategy='" + strategy + '\'' +
                ", healthCheckProtocol='" + healthCheckProtocol + '\'' +
                ", healthCheckPath='" + healthCheckPath + '\'' +
                ", healthCheckMethod='" + healthCheckMethod + '\'' +
                '}';
    }
}
