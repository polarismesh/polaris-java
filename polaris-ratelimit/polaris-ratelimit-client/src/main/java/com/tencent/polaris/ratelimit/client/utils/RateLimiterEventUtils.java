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

package com.tencent.polaris.ratelimit.client.utils;

import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.event.EventConstants;
import com.tencent.polaris.api.plugin.event.FlowEvent;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;

import java.time.LocalDateTime;

import static com.tencent.polaris.api.plugin.event.tsf.TsfEventDataConstants.RULE_DETAIL_KEY;
import static com.tencent.polaris.api.plugin.event.tsf.TsfEventDataConstants.RULE_ID_KEY;

/**
 * @author Haotian Zhang
 */
public class RateLimiterEventUtils {

    public static void reportEvent(Extensions extensions, ServiceKey serviceKey, RateLimitProto.Rule rule,
                                   QuotaResult.Code previousCode, QuotaResult.Code currentCode,
                                   String sourceNamespace, String sourceService, String labels, String reason) {
        if (extensions == null) {
            return;
        }

        EventConstants.Status currentFlowEventStatus = parseFlowEventStatus(currentCode);
        EventConstants.Status previousFlowEventStatus = parseFlowEventStatus(previousCode);

        FlowEvent.Builder flowEventBuilder = new FlowEvent.Builder()
                .withEventType(EventConstants.EventType.RATE_LIMITING)
                .withEventName(parseFlowEventName(currentFlowEventStatus, previousFlowEventStatus))
                .withTimestamp(LocalDateTime.now())
                .withClientId(extensions.getValueContext().getClientId())
                .withClientIp(extensions.getValueContext().getHost())
                .withNamespace(serviceKey.getNamespace())
                .withService(serviceKey.getService())
                .withApiPath(rule.getMethod().getValue().getValue())
                .withHost(extensions.getValueContext().getHost())
                .withSourceNamespace(sourceNamespace)
                .withSourceService(sourceService)
                .withLabels(labels)
                .withCurrentStatus(currentFlowEventStatus)
                .withPreviousStatus(previousFlowEventStatus)
                .withResourceType(parseFlowEventResourceType(rule.getResource()))
                .withRuleName(rule.getName().getValue());
        if (StringUtils.isNotBlank(reason)) {
            flowEventBuilder.withReason(reason);
        }

        FlowEvent flowEvent = flowEventBuilder.build();

        if (StringUtils.isNotBlank(rule.getId().getValue())) {
            flowEvent.getAdditionalParams().put(RULE_ID_KEY, rule.getId().getValue());
        } else {
            flowEvent.getAdditionalParams().put(RULE_ID_KEY, rule.getName().getValue());
        }

        flowEvent.getAdditionalParams().put(RULE_DETAIL_KEY, "{}");
        // 老的SDK并没有实现
        // if (rule.getMetadataMap().containsKey("original")) {
        // flowEvent.getAdditionalParams().put(RULE_DETAIL_KEY, rule.getMetadataMap().get("original"));
        // }

        BaseFlow.reportFlowEvent(extensions, flowEvent);
    }

    private static EventConstants.Status parseFlowEventStatus(QuotaResult.Code code) {
        switch (code) {
            case QuotaResultOk:
                return EventConstants.Status.UNLIMITED;
            case QuotaResultLimited:
                return EventConstants.Status.LIMITED;
            default:
                return EventConstants.Status.UNKNOWN;
        }
    }

    private static EventConstants.EventName parseFlowEventName(EventConstants.Status currentStatus, EventConstants.Status previousStatus) {
        if (currentStatus == EventConstants.Status.LIMITED && previousStatus == EventConstants.Status.UNLIMITED) {
            return EventConstants.EventName.RateLimitStart;
        } else if (currentStatus == EventConstants.Status.UNLIMITED && previousStatus == EventConstants.Status.LIMITED) {
            return EventConstants.EventName.RateLimitEnd;
        } else {
            return EventConstants.EventName.UNKNOWN;
        }
    }

    private static EventConstants.ResourceType parseFlowEventResourceType(RateLimitProto.Rule.Resource resource) {
        switch (resource) {
            case QPS:
                return EventConstants.ResourceType.QPS;
            case CONCURRENCY:
                return EventConstants.ResourceType.CONCURRENCY;
            default:
                return EventConstants.ResourceType.UNKNOWN;
        }
    }
}
