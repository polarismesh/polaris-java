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

package com.tencent.polaris.plugins.circuitbreaker.composite;

import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

public class CircuitBreakerRuleListener extends AbstractResourceEventListener {

    private final PolarisCircuitBreaker polarisCircuitBreaker;

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerRuleListener.class);

    public CircuitBreakerRuleListener(
            PolarisCircuitBreaker polarisCircuitBreaker) {
        this.polarisCircuitBreaker = polarisCircuitBreaker;
    }

    @Override
    public void onResourceAdd(ServiceEventKey svcEventKey, RegistryCacheValue newValue) {
        if (svcEventKey.getEventType() != EventType.CIRCUIT_BREAKING
                && svcEventKey.getEventType() != EventType.FAULT_DETECTING) {
            return;
        }
        LOG.info("[CircuitBreaker] onResourceAdd {}", svcEventKey);
        if (svcEventKey.getEventType() == EventType.CIRCUIT_BREAKING) {
            polarisCircuitBreaker.onCircuitBreakerRuleAdded(svcEventKey.getServiceKey());
        } else if (svcEventKey.getEventType() == EventType.FAULT_DETECTING) {
            polarisCircuitBreaker.onFaultDetectRuleChanged(svcEventKey.getServiceKey(), newValue);
        }
    }

    @Override
    public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue,
            RegistryCacheValue newValue) {
        if (svcEventKey.getEventType() != EventType.CIRCUIT_BREAKING
                && svcEventKey.getEventType() != EventType.FAULT_DETECTING) {
            return;
        }
        LOG.info("[CircuitBreaker] onResourceUpdated {}", svcEventKey);
        onChanged(svcEventKey);
        if (svcEventKey.getEventType() == EventType.FAULT_DETECTING) {
            polarisCircuitBreaker.onFaultDetectRuleChanged(svcEventKey.getServiceKey(), newValue);
        }
    }

    @Override
    public void onResourceDeleted(ServiceEventKey svcEventKey, RegistryCacheValue oldValue) {
        if (svcEventKey.getEventType() != EventType.CIRCUIT_BREAKING
                && svcEventKey.getEventType() != EventType.FAULT_DETECTING) {
            return;
        }
        LOG.info("[CircuitBreaker] onResourceDeleted {}", svcEventKey);
        onChanged(svcEventKey);
        if (svcEventKey.getEventType() == EventType.FAULT_DETECTING) {
            polarisCircuitBreaker.onFaultDetectRuleDeleted(svcEventKey.getServiceKey(), oldValue);
        }
    }

    private void onChanged(ServiceEventKey svcEventKey) {
        if (svcEventKey.getEventType() == EventType.CIRCUIT_BREAKING) {
            polarisCircuitBreaker.onCircuitBreakerRuleChanged(svcEventKey.getServiceKey());
        }
    }



}
