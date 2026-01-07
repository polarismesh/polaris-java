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

package com.tencent.polaris.plugins.router.lane;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;

public class LaneRouterConfig implements Verifier {

    @JsonProperty
    private BaseLaneMode baseLaneMode;

    @Override
    public void verify() {
        ConfigUtils.validateNull(baseLaneMode, "baseLaneMode");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof LaneRouterConfig) {
            LaneRouterConfig laneRouterConfig = (LaneRouterConfig) defaultObject;
            if (baseLaneMode == null) {
                setBaseLaneMode(laneRouterConfig.getBaseLaneMode());
            }
        }
    }

    public BaseLaneMode getBaseLaneMode() {
        return baseLaneMode;
    }

    public void setBaseLaneMode(BaseLaneMode baseLaneMode) {
        this.baseLaneMode = baseLaneMode;
    }

    @Override
    public String toString() {
        return "LaneRouterConfig{" +
                "baseLaneMode=" + baseLaneMode +
                '}';
    }
}
