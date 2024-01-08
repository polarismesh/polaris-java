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
import com.tencent.polaris.api.config.consumer.ZeroProtectionConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

import java.util.Objects;

/**
 * 零实例保护的配置项
 *
 * @author Haotian Zhang
 */
public class ZeroProtectionConfigImpl implements ZeroProtectionConfig {

    /**
     * 是否开启零实例保护
     */
    @JsonProperty
    private Boolean enable;

    /**
     * 零实例保护下是否探测网络连通性
     */
    @JsonProperty
    private Boolean needTestConnectivity;


    /**
     * 探测结果的过期时间（ms）
     */
    @JsonProperty
    private Integer testConnectivityExpiration;

    /**
     * 探测请求的超时时间（ms）
     */
    @JsonProperty
    private Integer testConnectivityTimeout;

    /**
     * 探测请求的并行数
     */
    @JsonProperty
    private Integer testConnectivityParallel;

    @Override
    public boolean isEnable() {
        if (Objects.isNull(enable)) {
            enable = false;
        }
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public boolean isNeedTestConnectivity() {
        if (Objects.isNull(needTestConnectivity)) {
            needTestConnectivity = false;
        }
        return needTestConnectivity;
    }


    public void setNeedTestConnectivity(boolean needTestConnectivity) {
        this.needTestConnectivity = needTestConnectivity;
    }

    @Override
    public int getTestConnectivityExpiration() {
        if (Objects.isNull(testConnectivityExpiration)) {
            testConnectivityExpiration = 60000;
        }
        return testConnectivityExpiration;
    }

    public void setTestConnectivityExpiration(Integer testConnectivityExpiration) {
        this.testConnectivityExpiration = testConnectivityExpiration;
    }

    @Override
    public int getTestConnectivityTimeout() {
        if (Objects.isNull(testConnectivityTimeout)) {
            testConnectivityTimeout = 1000;
        }
        return testConnectivityTimeout;
    }

    public void setTestConnectivityTimeout(Integer testConnectivityTimeout) {
        this.testConnectivityTimeout = testConnectivityTimeout;
    }

    @Override
    public int getTestConnectivityParallel() {
        if (Objects.isNull(testConnectivityParallel)) {
            testConnectivityParallel = 1;
        }
        return testConnectivityParallel;
    }

    public void setTestConnectivityParallel(Integer testConnectivityParallel) {
        this.testConnectivityParallel = testConnectivityParallel;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "zeroProtection.enable");
        if (!enable) {
            return;
        }
        ConfigUtils.validateNull(needTestConnectivity, "zeroProtection.needTestConnectivity");
        ConfigUtils.validateNull(testConnectivityExpiration, "zeroProtection.testConnectivityExpiration");
        ConfigUtils.validateNull(testConnectivityTimeout, "zeroProtection.testConnectivityTimeout");
        ConfigUtils.validateNull(testConnectivityParallel, "zeroProtection.testConnectivityParallel");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof ZeroProtectionConfig) {
            ZeroProtectionConfig zeroProtectionConfig = (ZeroProtectionConfig) defaultObject;
            if (null == enable) {
                setEnable(zeroProtectionConfig.isEnable());
            }
            if (null == needTestConnectivity) {
                setNeedTestConnectivity(zeroProtectionConfig.isNeedTestConnectivity());
            }
            if (Objects.isNull(testConnectivityExpiration)) {
                setTestConnectivityExpiration(zeroProtectionConfig.getTestConnectivityExpiration());
            }
            if (Objects.isNull(testConnectivityTimeout)) {
                setTestConnectivityTimeout(zeroProtectionConfig.getTestConnectivityTimeout());
            }
            if (Objects.isNull(testConnectivityParallel)) {
                setTestConnectivityParallel(zeroProtectionConfig.getTestConnectivityParallel());
            }
        }
    }

    @Override
    public String toString() {
        return "ZeroProtectionConfigImpl{" +
                "enable=" + enable +
                ", needTestConnectivity=" + needTestConnectivity +
                ", testConnectivityTimeout=" + testConnectivityTimeout +
                ", testConnectivityParallel=" + testConnectivityParallel +
                '}';
    }
}
