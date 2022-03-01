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

package com.tencent.polaris.plugins.circuitbreaker.common;

import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.Status;
import com.tencent.polaris.api.pojo.DefaultServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.Service;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.FlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.client.pb.CircuitBreakerProto;
import com.tencent.polaris.client.pb.CircuitBreakerProto.CbRule;
import com.tencent.polaris.client.pb.CircuitBreakerProto.DestinationSet;
import com.tencent.polaris.client.pb.CircuitBreakerProto.SourceMatcher;
import com.tencent.polaris.client.pb.ModelProto.MatchString;
import com.tencent.polaris.client.pb.ModelProto.MatchString.MatchStringType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class CircuitBreakUtils {

    public static boolean instanceHalfOpen(Instance instance, StatusDimension statusDimension) {
        CircuitBreakerStatus circuitBreakerStatus = instance.getCircuitBreakerStatus(statusDimension);
        if (null == circuitBreakerStatus) {
            return false;
        }
        return circuitBreakerStatus.getStatus() == Status.HALF_OPEN;
    }

    public static boolean instanceClose(Instance instance, StatusDimension statusDimension) {
        CircuitBreakerStatus circuitBreakerStatus = instance.getCircuitBreakerStatus(statusDimension);
        return null == circuitBreakerStatus || circuitBreakerStatus.getStatus() == Status.CLOSE;
    }

    private static CircuitBreakerProto.CircuitBreaker getCircuitBreakerRule(ServiceRule serviceRule) {
        if (null == serviceRule) {
            return null;
        }
        if (!serviceRule.isInitialized()) {
            return null;
        }
        return (CircuitBreakerProto.CircuitBreaker) serviceRule.getRule();
    }

    private static List<CbRule> getRules(CircuitBreakerProto.CircuitBreaker dstRule,
            CircuitBreakerProto.CircuitBreaker srcRule) {
        if (null != dstRule && dstRule.getInboundsCount() > 0) {
            return dstRule.getInboundsList();
        }
        if (null != srcRule && srcRule.getOutboundsCount() > 0) {
            return srcRule.getOutboundsList();
        }
        return null;
    }

    private static final String matchAll = "*";


    private static class MatchSourceResult {

        final boolean matched;
        final boolean allSourcesMatched;

        public MatchSourceResult(boolean matched, boolean allSourcesMatched) {
            this.matched = matched;
            this.allSourcesMatched = allSourcesMatched;
        }
    }

    private static MatchSourceResult matchSource(CbRule rule, RuleIdentifier ruleIdentifier) {
        if (rule.getSourcesCount() == 0) {
            return new MatchSourceResult(true, true);
        }
        Service callerService = ruleIdentifier.getCallerService();
        for (SourceMatcher sourceMatcher : rule.getSourcesList()) {
            boolean matchAllNamespace = sourceMatcher.getNamespace().getValue().equals(matchAll);
            boolean matchAllService = sourceMatcher.getService().getValue().equals(matchAll);
            if (matchAllNamespace && matchAllService) {
                return new MatchSourceResult(true, true);
            }
            if (null == callerService) {
                continue;
            }
            boolean namespaceMatch = matchAllNamespace;
            boolean serviceMatch = matchAllService;
            if (!namespaceMatch) {
                namespaceMatch = sourceMatcher.getNamespace().getValue().equals(callerService.getNamespace());
            }
            if (!serviceMatch) {
                serviceMatch = sourceMatcher.getService().getValue().equals(callerService.getService());
            }
            if (namespaceMatch && serviceMatch) {
                return new MatchSourceResult(true, false);
            }
        }
        return new MatchSourceResult(false, false);
    }

    private static class MatchDestResult {

        final DestinationSet destinationSet;
        final boolean allMethod;

        public MatchDestResult(DestinationSet destinationSet, boolean allMethod) {
            this.destinationSet = destinationSet;
            this.allMethod = allMethod;
        }
    }

    private static MatchDestResult matchDestination(CbRule rule, RuleIdentifier ruleIdentifier, FlowCache flowCache) {
        if (rule.getDestinationsCount() == 0) {
            return new MatchDestResult(null, false);
        }
        for (DestinationSet destinationSet : rule.getDestinationsList()) {
            boolean namespaceMatch = destinationSet.getNamespace().getValue().equals(matchAll);
            boolean serviceMatch = destinationSet.getService().getValue().equals(matchAll);
            if (!namespaceMatch) {
                namespaceMatch = destinationSet.getNamespace().getValue().equals(ruleIdentifier.getNamespace());
            }
            if (!serviceMatch) {
                serviceMatch = destinationSet.getService().getValue().equals(ruleIdentifier.getService());
            }
            if (!namespaceMatch || !serviceMatch) {
                continue;
            }
            MatchString methodMatcher = destinationSet.getMethod();
            if (null == methodMatcher) {
                return new MatchDestResult(destinationSet, true);
            }
            if (RuleUtils.isMatchAllValue(methodMatcher)) {
                return new MatchDestResult(destinationSet, true);
            }
            String method = ruleIdentifier.getMethod();
            if (methodMatcher.getType() == MatchStringType.EXACT) {
                if (StringUtils.equals(methodMatcher.getValue().getValue(), method)) {
                    return new MatchDestResult(destinationSet, false);
                }
            }
            Pattern pattern = flowCache.loadOrStoreCompiledRegex(methodMatcher.getValue().getValue());
            if (pattern.matcher(method).find()) {
                return new MatchDestResult(destinationSet, false);
            }
        }
        return new MatchDestResult(null, false);
    }

    public static class RuleDestinationResult {

        private final DestinationSet destinationSet;
        private final boolean matchAllSource;
        private final boolean matchAllMethod;

        public RuleDestinationResult(DestinationSet destinationSet, boolean matchAllSource, boolean matchAllMethod) {
            this.destinationSet = destinationSet;
            this.matchAllSource = matchAllSource;
            this.matchAllMethod = matchAllMethod;
        }

        public DestinationSet getDestinationSet() {
            return destinationSet;
        }

        public StatusDimension.Level getMatchLevel() {
            StatusDimension.Level level;
            if (matchAllSource && matchAllMethod) {
                level = StatusDimension.Level.SERVICE;
            } else if (matchAllMethod) {
                level = StatusDimension.Level.ALL_METHOD;
            } else if (matchAllSource) {
                level = StatusDimension.Level.ALL_CALLER;
            } else {
                level = StatusDimension.Level.CALLER_METHOD;
            }
            return level;
        }
    }

    /**
     * 通过配置获取远程规则
     *
     * @param ruleIdentifier 规则标识
     * @param extensions 插件集合
     * @param controlParam 控制参数
     * @return 过滤信息
     */
    public static RuleDestinationResult getRuleDestinationSet(RuleIdentifier ruleIdentifier, Extensions extensions,
            FlowControlParam controlParam) {
        Set<ServiceEventKey> svcEventKeys = new HashSet<>();
        ServiceEventKey dstSvcEventKey = new ServiceEventKey(
                new ServiceKey(ruleIdentifier.getNamespace(), ruleIdentifier.getService()),
                EventType.CIRCUIT_BREAKING);
        svcEventKeys.add(dstSvcEventKey);
        ServiceEventKey srcSvcEventKey = null;
        Service callerService = ruleIdentifier.getCallerService();
        if (null != callerService && StringUtils.isNotBlank(callerService.getNamespace()) && StringUtils
                .isNotBlank(callerService.getService())) {
            srcSvcEventKey = new ServiceEventKey(
                    new ServiceKey(callerService.getNamespace(), callerService.getService()),
                    EventType.CIRCUIT_BREAKING);
            svcEventKeys.add(srcSvcEventKey);
        }
        DefaultServiceEventKeysProvider serviceEventKeysProvider = new DefaultServiceEventKeysProvider();
        serviceEventKeysProvider.setSvcEventKeys(svcEventKeys);
        serviceEventKeysProvider.setUseCache(true);
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(extensions, false, serviceEventKeysProvider, controlParam);
        CircuitBreakerProto.CircuitBreaker dstRule = getCircuitBreakerRule(
                resourcesResponse.getServiceRule(dstSvcEventKey));
        CircuitBreakerProto.CircuitBreaker srcRule = null;
        if (null != srcSvcEventKey) {
            srcRule = getCircuitBreakerRule(resourcesResponse.getServiceRule(srcSvcEventKey));
        }
        List<CbRule> rules = getRules(dstRule, srcRule);
        if (CollectionUtils.isEmpty(rules)) {
            return new RuleDestinationResult(null, false, false);
        }
        for (CbRule rule : rules) {
            MatchSourceResult matchSourceResult = matchSource(rule, ruleIdentifier);
            if (!matchSourceResult.matched) {
                continue;
            }

            MatchDestResult matchDestResult = matchDestination(rule, ruleIdentifier, extensions.getFlowCache());
            if (null == matchDestResult.destinationSet) {
                continue;
            }
            return new RuleDestinationResult(matchDestResult.destinationSet, matchSourceResult.allSourcesMatched,
                    matchDestResult.allMethod);
        }
        return new RuleDestinationResult(null, false, false);
    }

    public static <T extends Verifier> ConfigSet<T> getConfigSet(InstanceGauge gauge, ConfigSetLocator<T> locator) {
        RuleIdentifier ruleIdentifier = new RuleIdentifier(gauge.getNamespace(), gauge.getService(),
                gauge.getCallerService(), gauge.getMethod());
        return locator.getConfigSet(ruleIdentifier);
    }

}
