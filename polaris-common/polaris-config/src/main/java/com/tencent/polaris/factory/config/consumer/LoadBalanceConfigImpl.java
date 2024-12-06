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

package com.tencent.polaris.factory.config.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * 负载均衡相关配置项
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class LoadBalanceConfigImpl extends PluginConfigImpl implements LoadBalanceConfig {

    @JsonProperty
    private String type;

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void verify() throws IllegalArgumentException {
        ConfigUtils.validateString(type, "loadbalancer.type");
        verifyPluginConfig();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            LoadBalanceConfig loadBalanceConfig = (LoadBalanceConfig) defaultObject;
            if (null == type) {
                setType(loadBalanceConfig.getType());
            }
            setDefaultPluginConfig(loadBalanceConfig);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "LoadBalanceConfigImpl{" +
                "type='" + type + '\'' +
                "} " + super.toString();
    }
}
