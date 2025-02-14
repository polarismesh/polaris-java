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

package com.tencent.polaris.plugins.circuitbreaker.composite.utils;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.event.FlowEvent;
import com.tencent.polaris.api.plugin.event.FlowEventConstants;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;

import java.time.LocalDateTime;

import static com.tencent.polaris.api.plugin.event.tsf.TsfEventDataConstants.*;

/**
 * @author Haotian Zhang
 */
public class CircuitBreakerEventUtils {

    public static void reportEvent(Extensions extensions, Resource resource,
                                   CircuitBreakerProto.CircuitBreakerRule currentActiveRule,
                                   CircuitBreakerStatus.Status previousStatus, CircuitBreakerStatus.Status currentStatus,
                                   String ruleName, String reason) {
        if (extensions == null) {
            return;
        }

        FlowEventConstants.Status currentFlowEventStatus = CircuitBreakerUtils.parseFlowEventStatus(currentStatus);
        FlowEventConstants.Status previousFlowEventStatus = CircuitBreakerUtils.parseFlowEventStatus(previousStatus);

        FlowEvent.Builder flowEventBuilder = new FlowEvent.Builder()
                .withEventType(ServiceEventKey.EventType.CIRCUIT_BREAKING)
                .withEventName(CircuitBreakerUtils.parseFlowEventName(currentFlowEventStatus, previousFlowEventStatus))
                .withTimestamp(LocalDateTime.now())
                .withClientId(extensions.getValueContext().getClientId())
                .withClientIp(extensions.getValueContext().getHost())
                .withNamespace(resource.getService().getNamespace())
                .withService(resource.getService().getService())
                .withSourceNamespace(resource.getCallerService().getNamespace())
                .withSourceService(resource.getService().getService())
                .withCurrentStatus(currentFlowEventStatus)
                .withPreviousStatus(previousFlowEventStatus)
                .withRuleName(ruleName);
        if (StringUtils.isNotBlank(reason)) {
            flowEventBuilder.withReason(reason);
        }
        String isolationObject = "";
        switch (resource.getLevel()) {
            case SERVICE:
                flowEventBuilder = flowEventBuilder.withResourceType(FlowEventConstants.ResourceType.SERVICE);
                isolationObject = CircuitBreakerUtils.getServiceCircuitBreakerName(
                        resource.getService().getNamespace(), resource.getService().getService());
                break;
            case METHOD:
                MethodResource methodResource = (MethodResource) resource;
                flowEventBuilder = flowEventBuilder.withResourceType(FlowEventConstants.ResourceType.METHOD)
                        .withApiProtocol(methodResource.getProtocol())
                        .withApiPath(methodResource.getPath())
                        .withApiMethod(methodResource.getMethod());
                isolationObject = CircuitBreakerUtils.getApiCircuitBreakerName(
                        methodResource.getService().getNamespace(), methodResource.getService().getService(),
                        methodResource.getPath(), methodResource.getMethod());
                break;
            case INSTANCE:
                InstanceResource instanceResource = (InstanceResource) resource;
                flowEventBuilder = flowEventBuilder.withResourceType(FlowEventConstants.ResourceType.INSTANCE)
                        .withHost(instanceResource.getHost())
                        .withPort(instanceResource.getPort());
                isolationObject = CircuitBreakerUtils.getInstanceCircuitBreakerName(
                        instanceResource.getHost(), instanceResource.getPort());
                break;
        }

        FlowEvent flowEvent = flowEventBuilder.build();

        String failureRate = "";
        String slowCallRate = "";
        if (CollectionUtils.isNotEmpty(currentActiveRule.getBlockConfigsList())) {
            for (CircuitBreakerProto.BlockConfig blockConfig : currentActiveRule.getBlockConfigsList()) {
                if (CollectionUtils.isNotEmpty(blockConfig.getTriggerConditionsList())) {
                    if (StringUtils.equals(blockConfig.getName(), "failure")) {
                        failureRate = String.valueOf(blockConfig.getTriggerConditions(0).getErrorPercent());
                    } else if (StringUtils.equals(blockConfig.getName(), "slow")) {
                        slowCallRate = String.valueOf(blockConfig.getTriggerConditions(0).getErrorPercent());
                    }
                }
            }
        }
        if (StringUtils.isNotBlank(isolationObject)) {
            flowEvent.getAdditionalParams().put(ISOLATION_OBJECT_KEY, isolationObject);
        }
        if (StringUtils.isNotBlank(failureRate)) {
            flowEvent.getAdditionalParams().put(FAILURE_RATE_KEY, failureRate);
        }
        if (StringUtils.isNotBlank(slowCallRate)) {
            flowEvent.getAdditionalParams().put(SLOW_CALL_DURATION_KEY, slowCallRate);
        }
        BaseFlow.reportFlowEvent(extensions, flowEvent);
    }
}
