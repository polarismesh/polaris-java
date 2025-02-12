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

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.event.FlowEventConstants;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class CircuitBreakerUtils {

    public static long DEFAULT_ERROR_RATE_INTERVAL_MS = 60 * 1000;

    public static long MIN_CLEANUP_INTERVAL = 60 * 1000;

    public static boolean checkRule(CircuitBreakerProto.CircuitBreakerRule rule) {
        return checkLevel(rule.getLevel());
    }

    public static boolean checkLevel(CircuitBreakerProto.Level level) {
        return level == CircuitBreakerProto.Level.SERVICE
                || level == CircuitBreakerProto.Level.METHOD
                || level == CircuitBreakerProto.Level.INSTANCE;
    }

    public static long getSleepWindowMilli(CircuitBreakerProto.CircuitBreakerRule currentActiveRule,
                                           CircuitBreakerConfig circuitBreakerConfig) {
        long sleepWindow = currentActiveRule.getRecoverCondition().getSleepWindow() * 1000L;
        if (sleepWindow == 0) {
            sleepWindow = circuitBreakerConfig.getSleepWindow();
        }
        return sleepWindow;
    }

    public static long getErrorRateIntervalSec(CircuitBreakerProto.TriggerCondition triggerCondition) {
        long interval = triggerCondition.getInterval();
        if (interval == 0) {
            interval = DEFAULT_ERROR_RATE_INTERVAL_MS / 1000;
        }
        return interval;
    }

    public static RetStatus parseRetStatus(ResourceStat resourceStat,
                                           List<CircuitBreakerProto.ErrorCondition> errorConditionsList,
                                           Function<String, Pattern> regexFunction) {
        if (CollectionUtils.isEmpty(errorConditionsList)) {
            return resourceStat.getRetStatus();
        }
        for (CircuitBreakerProto.ErrorCondition errorCondition : errorConditionsList) {
            ModelProto.MatchString condition = errorCondition.getCondition();
            switch (errorCondition.getInputType()) {
                case RET_CODE:
                    boolean codeMatched = RuleUtils
                            .matchStringValue(condition, String.valueOf(resourceStat.getRetCode()), regexFunction);
                    if (codeMatched) {
                        return RetStatus.RetFail;
                    }
                    break;
                case DELAY:
                    String value = condition.getValue().getValue();
                    int delayValue = Integer.parseInt(value);
                    if (resourceStat.getDelay() >= delayValue) {
                        return RetStatus.RetTimeout;
                    }
                    break;
                default:
                    break;
            }
        }
        return RetStatus.RetSuccess;
    }

    public static FlowEventConstants.Status parseFlowEventStatus(CircuitBreakerStatus.Status status) {
        switch (status) {
            case OPEN:
                return FlowEventConstants.Status.OPEN;
            case CLOSE:
                return FlowEventConstants.Status.CLOSE;
            case HALF_OPEN:
                return FlowEventConstants.Status.HALF_OPEN;
            case DESTROY:
                return FlowEventConstants.Status.DESTROY;
            default:
                return FlowEventConstants.Status.UNKNOWN;
        }
    }

    public static FlowEventConstants.EventName parseFlowEventName(FlowEventConstants.Status currentStatus, FlowEventConstants.Status previousStatus) {
        if (currentStatus == FlowEventConstants.Status.OPEN) {
            return FlowEventConstants.EventName.CircuitBreakerOpen;
        } else if (currentStatus == FlowEventConstants.Status.HALF_OPEN) {
            return FlowEventConstants.EventName.CircuitBreakerHalfOpen;
        } else if (currentStatus == FlowEventConstants.Status.CLOSE) {
            return FlowEventConstants.EventName.CircuitBreakerClose;
        } else if (currentStatus == FlowEventConstants.Status.DESTROY) {
            return FlowEventConstants.EventName.CircuitBreakerDestroy;
        } else {
            return FlowEventConstants.EventName.UNKNOWN;
        }
    }

    public static String getApiCircuitBreakerName(String targetNamespaceId, String serviceName, String path, String method) {
        return targetNamespaceId + "#" + serviceName + "#" + getFormatApi(path, method);
    }

    public static String getFormatApi(String path, String method) {
        return "(" +
                "path='" + path + '\'' +
                ", method='" + method + '\'' +
                ')';
    }

    public static String getInstanceCircuitBreakerName(String host, int port) {
        return IPAddressUtils.getIpCompatible(host) + ":" + port;
    }

    public static String getServiceCircuitBreakerName(String targetNamespaceId, String serviceName) {
        return targetNamespaceId + "#" + serviceName;
    }
}
