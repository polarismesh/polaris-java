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

package com.tencent.polaris.plugins.circuitbreaker.errcount;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * 基于错误次数熔断插件配置结构
 *
 * @author andrewshan
 * @date 2019/8/26
 */
public class Config implements Verifier {


    @JsonProperty
    private Integer continuousErrorThreshold;

    public Integer getContinuousErrorThreshold() {
        return continuousErrorThreshold;
    }

    public void setContinuousErrorThreshold(Integer continuousErrorThreshold) {
        this.continuousErrorThreshold = continuousErrorThreshold;
    }

    @Override
    public void verify() {
        ConfigUtils.validatePositive(continuousErrorThreshold, "continuousErrorThreshold");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            Config config = (Config) defaultObject;
            if (null == continuousErrorThreshold) {
                setContinuousErrorThreshold(config.getContinuousErrorThreshold());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "Config{" +
                "continuousErrorThreshold=" + continuousErrorThreshold +
                '}';
    }
}
