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

package com.tencent.polaris.plugins.loadbalancer.shortestresponsetime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

public class ShortestResponseTimeLoadBalanceConfig implements Verifier {

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private long slidePeriod;

    public long getSlidePeriod() {
        return slidePeriod;
    }

    public void setSlidePeriod(long slidePeriod) {
        this.slidePeriod = slidePeriod;
    }

    @Override
    public void verify() {
        ConfigUtils.validateInterval(slidePeriod, "slidePeriod");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof ShortestResponseTimeLoadBalanceConfig) {
            ShortestResponseTimeLoadBalanceConfig defaultConfig = (ShortestResponseTimeLoadBalanceConfig) defaultObject;
            if (slidePeriod == 0) {
                slidePeriod = defaultConfig.getSlidePeriod();
            }
        }
    }
}
