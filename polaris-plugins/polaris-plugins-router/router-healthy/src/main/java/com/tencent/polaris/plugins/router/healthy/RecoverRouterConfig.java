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

package com.tencent.polaris.plugins.router.healthy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;

/**
 * 过滤不健康的路由插件配置
 *
 * @author lepdou 2022-04-24
 */
public class RecoverRouterConfig implements Verifier {

    @JsonProperty
    private Boolean excludeCircuitBreakInstances;

    @Override
    public void verify() {

    }

    @Override
    public void setDefault(Object defaultConfig) {
        if (defaultConfig != null) {
            RecoverRouterConfig recoverRouterConfig = (RecoverRouterConfig) defaultConfig;
            if (excludeCircuitBreakInstances == null) {
                setExcludeCircuitBreakInstances(recoverRouterConfig.isExcludeCircuitBreakInstances());
            }
        }
    }

    public Boolean isExcludeCircuitBreakInstances() {
        return excludeCircuitBreakInstances;
    }

    public void setExcludeCircuitBreakInstances(Boolean excludeCircuitBreakInstances) {
        this.excludeCircuitBreakInstances = excludeCircuitBreakInstances;
    }
}
