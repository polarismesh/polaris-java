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

package com.tencent.polaris.factory.config.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.global.FlowCacheConfig;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

public class FlowConfigImpl implements FlowConfig {

    String DEFAULT_FLOW_NAME = "default";

    @JsonProperty
    private String name;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void verify() {
        ConfigUtils.validateString(name, "flowCache.name");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            FlowCacheConfig flowCacheConfig = (FlowCacheConfig) defaultObject;
            if (null == name) {
                setName(flowCacheConfig.getName());
            }
        }
    }

    @Override
    public String toString() {
        return "FlowConfigImpl{" +
                "name='" + name + '\'' +
                '}';
    }
}
