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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.provider.LosslessConfig;
import com.tencent.polaris.api.config.provider.ProviderConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.util.ConfigUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 被调端配置对象
 *
 * @author andrewshan
 */
public class ProviderConfigImpl implements ProviderConfig {

    /**
     * 默认最小注册重试间隔, 30秒, 单位: 毫秒
     */
    private final static long DEFAULT_MIN_REGISTER_INTERVAL = 30 * 1000;

    private final static int DEFAULT_HEARTBEAT_WORKER_SIZE = 4;

    @JsonProperty
    private RateLimitConfigImpl rateLimit;

    @JsonProperty
    private List<RegisterConfigImpl> registers;

    @JsonProperty
    private ServiceConfigImpl service;

    @JsonIgnore
    private final Map<String, RegisterConfigImpl> registerConfigMap = new ConcurrentHashMap<>();

    @JsonProperty
    private long minRegisterInterval;

    @JsonProperty
    private int heartbeatWorkerSize;

    @JsonProperty
    private LosslessConfigImpl lossless;

    @Override
    public RateLimitConfigImpl getRateLimit() {
        return rateLimit;
    }

    @Override
    public long getMinRegisterInterval() {
        return minRegisterInterval;
    }

    @Override
    public int getHeartbeatWorkerSize() {
        return heartbeatWorkerSize;
    }

    @Override
    public LosslessConfig getLossless() {
        return lossless;
    }

    @Override
    public List<RegisterConfigImpl> getRegisters() {
        if (CollectionUtils.isEmpty(registers)) {
            registers = new ArrayList<>();
        }
        return registers;
    }

    private void setRegisterConfigMap(List<RegisterConfigImpl> registers) {
        if (CollectionUtils.isNotEmpty(registers)) {
            for (RegisterConfigImpl registerConfig : registers) {
                if (registerConfigMap.containsKey(registerConfig.getServerConnectorId())) {
                    throw new IllegalArgumentException(String.format("Register config of [%s] is already exist.",
                            registerConfig.getServerConnectorId()));
                } else {
                    registerConfigMap.put(registerConfig.getServerConnectorId(), registerConfig);
                }
            }
        }
    }

    @Override
    public Map<String, RegisterConfigImpl> getRegisterConfigMap() {
        return registerConfigMap;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(rateLimit, "rateLimitConfig");
        ConfigUtils.validateNull(lossless, "losslessConfig");

        rateLimit.verify();
        if (CollectionUtils.isNotEmpty(registers)) {
            for (RegisterConfigImpl registerConfig : registers) {
                registerConfig.verify();
            }
        }
        setRegisterConfigMap(registers);
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null == rateLimit) {
            rateLimit = new RateLimitConfigImpl();
        }
        if (null == lossless) {
            lossless = new LosslessConfigImpl();
        }
        if (null == service) {
            service = new ServiceConfigImpl();
        }
        if (minRegisterInterval == 0) {
            minRegisterInterval = DEFAULT_MIN_REGISTER_INTERVAL;
        }
        if (heartbeatWorkerSize == 0) {
            heartbeatWorkerSize = DEFAULT_HEARTBEAT_WORKER_SIZE;
        }
        if (null != defaultObject) {
            ProviderConfig providerConfig = (ProviderConfig) defaultObject;
            rateLimit.setDefault(providerConfig.getRateLimit());
            lossless.setDefault(providerConfig.getLossless());
            if (CollectionUtils.isNotEmpty(registers)) {
                for (RegisterConfigImpl registerConfig : registers) {
                    registerConfig.setDefault(providerConfig.getRegisters().get(0));
                }
            } else {
                registers = new ArrayList<>();
            }
            service.setDefault(providerConfig.getService());
        }

    }

    @Override
    public ServiceConfigImpl getService() {
        return service;
    }

    public void setService(ServiceConfigImpl service) {
        this.service = service;
    }

    @Override
    public String toString() {
        return "ProviderConfigImpl{" +
                "rateLimit=" + rateLimit +
                ", registers=" + registers +
                ", service=" + service +
                ", registerConfigMap=" + registerConfigMap +
                ", minRegisterInterval=" + minRegisterInterval +
                ", heartbeatWorkerSize=" + heartbeatWorkerSize +
                ", lossless=" + lossless +
                '}';
    }
}
