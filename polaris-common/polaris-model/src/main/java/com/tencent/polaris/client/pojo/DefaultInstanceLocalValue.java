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

package com.tencent.polaris.client.pojo;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.InstanceLocalValue;
import com.tencent.polaris.api.pojo.StatusDimension;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 客户端服务数据统计对象，包含熔断数据、动态权重等信息
 *
 * @author andrewshan
 * @date 2019/8/27
 */
public class DefaultInstanceLocalValue implements InstanceLocalValue {

    private final Map<StatusDimension, AtomicReference<CircuitBreakerStatus>>
            circuitBreakerStatus = new ConcurrentHashMap<>();

    private final AtomicReference<DetectResult> detectResult = new AtomicReference<>();

    private final Map<Integer, Object> pluginValues = new ConcurrentHashMap<>();

    @Override
    public Collection<StatusDimension> getStatusDimensions() {
        return circuitBreakerStatus.keySet();
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus(StatusDimension statusDimension) {
        AtomicReference<CircuitBreakerStatus> value = circuitBreakerStatus
                .get(statusDimension);
        if (null == value) {
            return null;
        }
        return value.get();
    }

    @Override
    public DetectResult getDetectResult() {
        return detectResult.get();
    }

    @Override
    public void setCircuitBreakerStatus(StatusDimension statusDimension, CircuitBreakerStatus status) {
        AtomicReference<CircuitBreakerStatus> value = circuitBreakerStatus
                .computeIfAbsent(statusDimension,
                        new Function<StatusDimension, AtomicReference<CircuitBreakerStatus>>() {
                            @Override
                            public AtomicReference<CircuitBreakerStatus> apply(StatusDimension statusDimension) {
                                return new AtomicReference<>();
                            }
                        });
        value.set(status);
    }

    public void setDetectResult(DetectResult detectResult) {
        this.detectResult.set(detectResult);
    }

    @Override
    public Object getPluginValue(int pluginId, Function<Integer, Object> create) {
        if (null == create) {
            return pluginValues.get(pluginId);
        }
        return pluginValues.computeIfAbsent(pluginId, create);
    }


}
