/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
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
import com.tencent.polaris.api.plugin.event.FlowEvent;
import com.tencent.polaris.api.plugin.event.FlowEventConstants;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;

import java.time.Instant;

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
        FlowEvent.Builder flowEventBuilder = new FlowEvent.Builder()
                .withEventType(ServiceEventKey.EventType.RATE_LIMITING)
                .withTimestamp(Instant.now())
                .withClientId(extensions.getValueContext().getClientId())
                .withClientIp(extensions.getValueContext().getHost())
                .withNamespace(serviceKey.getNamespace())
                .withService(serviceKey.getService())
                .withApiPath(rule.getMethod().getValue().getValue())
                .withHost(extensions.getValueContext().getHost())
                .withSourceNamespace(sourceNamespace)
                .withSourceService(sourceService)
                .withLabels(labels)
                .withCurrentStatus(parseFlowEventStatus(currentCode))
                .withPreviousStatus(parseFlowEventStatus(previousCode))
                .withResourceType(parseFlowEventResourceType(rule.getResource()))
                .withRuleName(rule.getName().getValue())
                .withReason(reason);
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

    private static FlowEventConstants.Status parseFlowEventStatus(QuotaResult.Code code) {
        switch (code) {
            case QuotaResultOk:
                return FlowEventConstants.Status.UNLIMITED;
            case QuotaResultLimited:
                return FlowEventConstants.Status.LIMITED;
            default:
                return FlowEventConstants.Status.UNKNOWN;
        }
    }

    private static FlowEventConstants.ResourceType parseFlowEventResourceType(RateLimitProto.Rule.Resource resource) {
        switch (resource) {
            case QPS:
                return FlowEventConstants.ResourceType.QPS;
            case CONCURRENCY:
                return FlowEventConstants.ResourceType.CONCURRENCY;
            default:
                return FlowEventConstants.ResourceType.UNKNOWN;
        }
    }
}
