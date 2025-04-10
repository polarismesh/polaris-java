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

import com.google.common.collect.Lists;
import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.event.EventConstants;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import org.slf4j.Logger;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CircuitBreakerUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerUtils.class);

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

    public static EventConstants.Status parseFlowEventStatus(CircuitBreakerStatus.Status status) {
        switch (status) {
            case OPEN:
                return EventConstants.Status.OPEN;
            case CLOSE:
                return EventConstants.Status.CLOSE;
            case HALF_OPEN:
                return EventConstants.Status.HALF_OPEN;
            case DESTROY:
                return EventConstants.Status.DESTROY;
            default:
                return EventConstants.Status.UNKNOWN;
        }
    }

    public static EventConstants.EventName parseFlowEventName(EventConstants.Status currentStatus, EventConstants.Status previousStatus) {
        if (currentStatus == EventConstants.Status.OPEN) {
            return EventConstants.EventName.CircuitBreakerOpen;
        } else if (currentStatus == EventConstants.Status.HALF_OPEN) {
            return EventConstants.EventName.CircuitBreakerHalfOpen;
        } else if (currentStatus == EventConstants.Status.CLOSE) {
            return EventConstants.EventName.CircuitBreakerClose;
        } else if (currentStatus == EventConstants.Status.DESTROY) {
            return EventConstants.EventName.CircuitBreakerDestroy;
        } else {
            return EventConstants.EventName.UNKNOWN;
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

    public static ServiceRule fillDefaultCircuitBreakerRuleInNeeded(Resource resource, ServiceRule serviceRule, CircuitBreakerConfig circuitBreakerConfig) {
        if (serviceRule instanceof ServiceRuleByProto && serviceRule.getRule() instanceof CircuitBreakerProto.CircuitBreaker) {
            List<CircuitBreakerProto.CircuitBreakerRule> rules = ((CircuitBreakerProto.CircuitBreaker) serviceRule.getRule()).getRulesList();
            if (shouldFilled(circuitBreakerConfig, rules)) {
                CircuitBreakerProto.CircuitBreaker.Builder newCircuitBreakerBuilder = CircuitBreakerProto.CircuitBreaker.newBuilder().mergeFrom((CircuitBreakerProto.CircuitBreaker) serviceRule.getRule());
                CircuitBreakerProto.CircuitBreakerRule.Builder defaultCircuitBreakerRuleBuilder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();
                // set name
                defaultCircuitBreakerRuleBuilder.setName("default-polaris-instance-circuit-breaker");
                // set enable
                defaultCircuitBreakerRuleBuilder.setEnable(true);
                // set level
                defaultCircuitBreakerRuleBuilder.setLevel(CircuitBreakerProto.Level.INSTANCE);
                // build ruleMatcher
                CircuitBreakerProto.RuleMatcher.Builder ruleMatcher = CircuitBreakerProto.RuleMatcher.newBuilder();
                CircuitBreakerProto.RuleMatcher.SourceService.Builder sourceServiceBuilder = CircuitBreakerProto.RuleMatcher.SourceService.newBuilder();
                sourceServiceBuilder.setNamespace(resource.getCallerService().getNamespace());
                sourceServiceBuilder.setService(resource.getCallerService().getService());
                ruleMatcher.setSource(sourceServiceBuilder);
                CircuitBreakerProto.RuleMatcher.DestinationService.Builder destinationServiceBuilder = CircuitBreakerProto.RuleMatcher.DestinationService.newBuilder();
                destinationServiceBuilder.setNamespace(resource.getService().getNamespace());
                destinationServiceBuilder.setService(resource.getService().getService());
                ruleMatcher.setDestination(destinationServiceBuilder);
                defaultCircuitBreakerRuleBuilder.setRuleMatcher(ruleMatcher);
                // build blockConfigs
                List<CircuitBreakerProto.BlockConfig> blockConfigList = Lists.newArrayList();
                // build failure block config
                CircuitBreakerProto.BlockConfig.Builder failureBlockConfigBuilder = CircuitBreakerProto.BlockConfig.newBuilder();
                failureBlockConfigBuilder.setName("failure-block-config");
                // build ret code error condition
                CircuitBreakerProto.ErrorCondition.Builder errorConditionBuilder = CircuitBreakerProto.ErrorCondition.newBuilder();
                errorConditionBuilder.setInputType(CircuitBreakerProto.ErrorCondition.InputType.RET_CODE);
                ModelProto.MatchString.Builder codeMatchStringBuilder = ModelProto.MatchString.newBuilder();
                codeMatchStringBuilder.setType(ModelProto.MatchString.MatchStringType.IN);
                String statusCodes = IntStream.range(500, 600)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(","));
                codeMatchStringBuilder.setValue(StringValue.of(statusCodes));
                errorConditionBuilder.setCondition(codeMatchStringBuilder);
                failureBlockConfigBuilder.addErrorConditions(errorConditionBuilder);
                // build failure trigger conditions
                // build error rate trigger condition
                CircuitBreakerProto.TriggerCondition.Builder errorRateTriggerConditionBuilder = CircuitBreakerProto.TriggerCondition.newBuilder();
                errorRateTriggerConditionBuilder.setTriggerType(CircuitBreakerProto.TriggerCondition.TriggerType.ERROR_RATE);
                errorRateTriggerConditionBuilder.setErrorPercent(circuitBreakerConfig.getDefaultErrorPercent());
                errorRateTriggerConditionBuilder.setInterval(circuitBreakerConfig.getDefaultInterval() / 1000);
                errorRateTriggerConditionBuilder.setMinimumRequest(circuitBreakerConfig.getDefaultMinimumRequest());
                failureBlockConfigBuilder.addTriggerConditions(errorRateTriggerConditionBuilder);
                // build consecutive error trigger condition
                CircuitBreakerProto.TriggerCondition.Builder consecutiveErrorTriggerConditionBuilder = CircuitBreakerProto.TriggerCondition.newBuilder();
                consecutiveErrorTriggerConditionBuilder.setTriggerType(CircuitBreakerProto.TriggerCondition.TriggerType.CONSECUTIVE_ERROR);
                consecutiveErrorTriggerConditionBuilder.setErrorCount(circuitBreakerConfig.getDefaultErrorCount());
                failureBlockConfigBuilder.addTriggerConditions(consecutiveErrorTriggerConditionBuilder);
                blockConfigList.add(failureBlockConfigBuilder.build());
                defaultCircuitBreakerRuleBuilder.addAllBlockConfigs(blockConfigList);
                // build recoverCondition
                CircuitBreakerProto.RecoverCondition.Builder recoverConditionBuilder = CircuitBreakerProto.RecoverCondition.newBuilder();
                recoverConditionBuilder.setSleepWindow((int) circuitBreakerConfig.getSleepWindow() / 1000);
                recoverConditionBuilder.setConsecutiveSuccess(circuitBreakerConfig.getSuccessCountAfterHalfOpen());
                defaultCircuitBreakerRuleBuilder.setRecoverCondition(recoverConditionBuilder);
                newCircuitBreakerBuilder.addRules(defaultCircuitBreakerRuleBuilder);
                CircuitBreakerProto.CircuitBreaker newCircuitBreaker = newCircuitBreakerBuilder.build();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Resource {} set default circuit breaker rule {}", resource, newCircuitBreaker);
                } else {
                    LOG.info("Resource {} set default circuit breaker rule with DefaultErrorCount:{}, " +
                                    "DefaultErrorPercent:{}, DefaultInterval:{}, DefaultMinimumRequest:{}",
                            resource, circuitBreakerConfig.getDefaultErrorCount(), circuitBreakerConfig.getDefaultErrorPercent(),
                            circuitBreakerConfig.getDefaultInterval(), circuitBreakerConfig.getDefaultMinimumRequest());
                }
                return new ServiceRuleByProto(newCircuitBreaker, serviceRule.getRevision(), ((ServiceRuleByProto) serviceRule).isLoadedFromFile(), ((ServiceRuleByProto) serviceRule).getEventType());
            }
        }
        return serviceRule;
    }

    private static boolean shouldFilled(CircuitBreakerConfig circuitBreakerConfig, List<CircuitBreakerProto.CircuitBreakerRule> rules) {
        if (!circuitBreakerConfig.isDefaultRuleEnable()) {
            return false;
        }
        if (CollectionUtils.isEmpty(rules)) {
            return true;
        }
        for (CircuitBreakerProto.CircuitBreakerRule rule : rules) {
            if (rule.getEnable() && CircuitBreakerUtils.checkRule(rule)) {
                return false;
            }
        }
        return true;
    }
}
