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

package com.tencent.polaris.plugins.circuitbreaker.composite;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import java.util.Map;

public class CircuitBreakerRuleListener extends AbstractResourceEventListener {

    private final PolarisCircuitBreaker polarisCircuitBreaker;

    public CircuitBreakerRuleListener(
            PolarisCircuitBreaker polarisCircuitBreaker) {
        this.polarisCircuitBreaker = polarisCircuitBreaker;
    }

    @Override
    public void onResourceAdd(ServiceEventKey svcEventKey, RegistryCacheValue newValue) {
        if (svcEventKey.getEventType() != EventType.CIRCUIT_BREAKING) {
            return;
        }
        for (Map.Entry<Resource, CircuitBreakerRuleContainer> entry : polarisCircuitBreaker.getContainers()
                .entrySet()) {
            if (entry.getKey().getService().equals(svcEventKey.getServiceKey())) {
                entry.getValue().schedule();
            }
        }
    }

    @Override
    public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue,
            RegistryCacheValue newValue) {
        if (svcEventKey.getEventType() != EventType.CIRCUIT_BREAKING) {
            return;
        }
        for (Map.Entry<Resource, CircuitBreakerRuleContainer> entry : polarisCircuitBreaker.getContainers()
                .entrySet()) {
            if (entry.getKey().getService().equals(svcEventKey.getServiceKey())) {
                entry.getValue().schedule();
            }
        }
    }

    @Override
    public void onResourceDeleted(ServiceEventKey svcEventKey, RegistryCacheValue oldValue) {
        if (svcEventKey.getEventType() != EventType.CIRCUIT_BREAKING) {
            return;
        }
        for (Map.Entry<Resource, CircuitBreakerRuleContainer> entry : polarisCircuitBreaker.getContainers()
                .entrySet()) {
            if (entry.getKey().getService().equals(svcEventKey.getServiceKey())) {
                entry.getValue().schedule();
            }
        }
    }

}
