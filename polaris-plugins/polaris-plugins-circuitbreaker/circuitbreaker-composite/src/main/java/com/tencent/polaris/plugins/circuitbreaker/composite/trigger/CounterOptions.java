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

package com.tencent.polaris.plugins.circuitbreaker.composite.trigger;

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.plugins.circuitbreaker.composite.StatusChangeHandler;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.TriggerCondition;
import java.util.concurrent.ScheduledExecutorService;

public class CounterOptions {

    private CircuitBreakerConfig circuitBreakerConfig;

    private Resource resource;

    private TriggerCondition triggerCondition;

    private ScheduledExecutorService executorService;

    private StatusChangeHandler statusChangeHandler;

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public TriggerCondition getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(
            TriggerCondition triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public StatusChangeHandler getStatusChangeHandler() {
        return statusChangeHandler;
    }

    public void setStatusChangeHandler(
            StatusChangeHandler statusChangeHandler) {
        this.statusChangeHandler = statusChangeHandler;
    }

    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return circuitBreakerConfig;
    }

    public void setCircuitBreakerConfig(CircuitBreakerConfig circuitBreakerConfig) {
        this.circuitBreakerConfig = circuitBreakerConfig;
    }
}
