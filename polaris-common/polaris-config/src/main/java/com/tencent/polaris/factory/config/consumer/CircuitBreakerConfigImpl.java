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

package com.tencent.polaris.factory.config.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;
import java.util.List;

/**
 * 熔断相关的配置项
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class CircuitBreakerConfigImpl extends PluginConfigImpl implements CircuitBreakerConfig {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private List<String> chain;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long checkPeriod;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long errorRateInterval;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long sleepWindow;

    @JsonProperty
    private Integer requestCountAfterHalfOpen;

    @JsonProperty
    private Integer successCountAfterHalfOpen;

    @JsonProperty
    private Boolean enableRemotePull;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long countersExpireInterval;

    @Override
    public boolean isEnable() {
        if (null == enable) {
            return false;
        }
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public List<String> getChain() {
        return chain;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }

    public void setRequestCountAfterHalfOpen(int requestCountAfterHalfOpen) {
        this.requestCountAfterHalfOpen = requestCountAfterHalfOpen;
    }

    public void setSuccessCountAfterHalfOpen(int successCountAfterHalfOpen) {
        this.successCountAfterHalfOpen = successCountAfterHalfOpen;
    }

    @Override
    public int getRequestCountAfterHalfOpen() {
        return requestCountAfterHalfOpen;
    }

    @Override
    public int getSuccessCountAfterHalfOpen() {
        return successCountAfterHalfOpen;
    }

    @Override
    public long getErrorRateInterval() {
        if (null == errorRateInterval) {
            return 0;
        }
        return errorRateInterval;
    }

    void setErrorRateInterval(long errorRateInterval) {
        this.errorRateInterval = errorRateInterval;
    }

    @Override
    public long getCheckPeriod() {
        if (null == checkPeriod) {
            return 0;
        }
        return checkPeriod;
    }

    public void setCheckPeriod(long checkPeriod) {
        this.checkPeriod = checkPeriod;
    }

    @Override
    public long getSleepWindow() {
        if (null == sleepWindow) {
            return 0;
        }
        return sleepWindow;
    }

    public void setSleepWindow(long sleepWindow) {
        this.sleepWindow = sleepWindow;
    }

    @Override
    public boolean isEnableRemotePull() {
        if (enableRemotePull == null) {
            return false;
        }
        return enableRemotePull;
    }

    @Override
    public long getCountersExpireInterval() {
        if (null == countersExpireInterval) {
            return 0;
        }
        return countersExpireInterval;
    }

    public void setCountersExpireInterval(long countersExpireInterval) {
        this.countersExpireInterval = countersExpireInterval;
    }

    public void setEnableRemotePull(boolean enableRemotePull) {
        this.enableRemotePull = enableRemotePull;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "circuitBreaker.enable");
        if (!enable) {
            return;
        }
        if (CollectionUtils.isEmpty(chain)) {
            throw new IllegalArgumentException("circuitBreaker.chain cannot be empty");
        }
        ConfigUtils.validateInterval(checkPeriod, "circuitBreaker.checkPeriod");
        ConfigUtils.validateInterval(sleepWindow, "circuitBreaker.sleepWindow");
        ConfigUtils.validateInterval(countersExpireInterval, "circuitBreaker.countersExpireInterval");
        ConfigUtils.validateInterval(errorRateInterval, "circuitBreaker.errorRateInterval");
        ConfigUtils.validatePositive(requestCountAfterHalfOpen, "circuitBreaker.requestCountAfterHalfOpen");
        ConfigUtils.validatePositive(successCountAfterHalfOpen, "circuitBreaker.successCountAfterHalfOpen");
        ConfigUtils.validateNull(enableRemotePull, "circuitBreaker.enableRemotePull");
        verifyPluginConfig();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            CircuitBreakerConfig circuitBreakerConfig = (CircuitBreakerConfig) defaultObject;
            if (null == enable) {
                setEnable(circuitBreakerConfig.isEnable());
            }
            if (CollectionUtils.isEmpty(chain)) {
                setChain(circuitBreakerConfig.getChain());
            }
            if (null == checkPeriod) {
                setCheckPeriod(circuitBreakerConfig.getCheckPeriod());
            }
            if (null == sleepWindow) {
                setSleepWindow(circuitBreakerConfig.getSleepWindow());
            }
            if (null == countersExpireInterval) {
                setCountersExpireInterval(circuitBreakerConfig.getCountersExpireInterval());
            }
            if (null == errorRateInterval) {
                setErrorRateInterval(circuitBreakerConfig.getErrorRateInterval());
            }
            if (null == requestCountAfterHalfOpen) {
                setRequestCountAfterHalfOpen(circuitBreakerConfig.getRequestCountAfterHalfOpen());
            }
            if (null == successCountAfterHalfOpen) {
                setSuccessCountAfterHalfOpen(circuitBreakerConfig.getSuccessCountAfterHalfOpen());
            }
            if (null == enableRemotePull) {
                setEnableRemotePull(circuitBreakerConfig.isEnableRemotePull());
            }
            if (enable) {
                setDefaultPluginConfig(circuitBreakerConfig);
            }
        }
    }

    @Override
    public String toString() {
        return "CircuitBreakerConfigImpl{" +
                "enable=" + enable +
                ", chain=" + chain +
                ", checkPeriod=" + checkPeriod +
                ", sleepWindow=" + sleepWindow +
                ", requestCountAfterHalfOpen=" + requestCountAfterHalfOpen +
                ", successCountAfterHalfOpen=" + successCountAfterHalfOpen +
                ", enableRemotePull=" + enableRemotePull +
                ", countersExpireInterval=" + countersExpireInterval +
                '}';
    }
}
