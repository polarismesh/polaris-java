/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.factory.config.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.global.FlowCacheConfig;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

public class FlowCacheConfigImpl implements FlowCacheConfig {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long expireInterval;

    @JsonProperty
    private String name;

    @Override
    public boolean isEnable() {
        if (null == enable) {
            return false;
        }
        return enable;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long getExpireInterval() {
        if (null == expireInterval) {
            return 0;
        }
        return expireInterval;
    }

    public void setExpireInterval(long expireInterval) {
        this.expireInterval = expireInterval;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "flowCache.enable");
        ConfigUtils.validateInterval(expireInterval, "flowCache.expireInterval");
        ConfigUtils.validateString(name, "flowCache.name");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            FlowCacheConfig flowCacheConfig = (FlowCacheConfig) defaultObject;
            if (null == enable) {
                setEnable(flowCacheConfig.isEnable());
            }
            if (null == name) {
                setName(flowCacheConfig.getName());
            }
            if (null == expireInterval) {
                setExpireInterval(flowCacheConfig.getExpireInterval());
            }
        }
    }

    @Override
    public String toString() {
        return "FlowCacheConfigImpl{" +
                "enable=" + enable +
                ", expireInterval=" + expireInterval +
                ", name='" + name + '\'' +
                '}';
    }
}
