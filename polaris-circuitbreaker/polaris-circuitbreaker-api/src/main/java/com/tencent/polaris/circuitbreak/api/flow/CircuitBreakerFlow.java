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

package com.tencent.polaris.circuitbreak.api.flow;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.circuitbreak.api.pojo.CheckResult;
import com.tencent.polaris.client.flow.AbstractFlow;
import java.util.ServiceLoader;

public interface CircuitBreakerFlow extends AbstractFlow {

    /**
     * check and acquire circuitbreaker
     *
     * @param resource
     * @return pass or not, and fallback config if needed
     */
    default CheckResult check(Resource resource) {
        return null;
    }

    /**
     * report the resource invoke result
     *
     * @param reportStat
     */
    default void report(ResourceStat reportStat) {

    }

    static CircuitBreakerFlow loadCircuitBreakerFlow(String name) {
        ServiceLoader<CircuitBreakerFlow> flows = ServiceLoader.load(CircuitBreakerFlow.class);
        for (CircuitBreakerFlow flow : flows) {
            if (StringUtils.equals(flow.getName(), name)) {
                return flow;
            }
        }
        throw new PolarisException(ErrorCode.INVALID_CONFIG,
                String.format("unknown flow name %s, type is %s", name, CircuitBreakerFlow.class.getCanonicalName()));
    }
}