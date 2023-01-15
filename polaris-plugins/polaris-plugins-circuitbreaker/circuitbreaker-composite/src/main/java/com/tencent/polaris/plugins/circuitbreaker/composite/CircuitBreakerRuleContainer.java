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

import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.CircuitBreaker;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.CircuitBreakerRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher.DestinationService;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher.SourceService;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public class CircuitBreakerRuleContainer {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerRuleContainer.class);

    private final Resource resource;

    private final PolarisCircuitBreaker polarisCircuitBreaker;

    private final Runnable pullTask;

    public CircuitBreakerRuleContainer(Resource resource, PolarisCircuitBreaker polarisCircuitBreaker) {
        this.resource = resource;
        this.polarisCircuitBreaker = polarisCircuitBreaker;
        pullTask = new Runnable() {
            @Override
            public void run() {
                ServiceEventKey serviceEventKey = new ServiceEventKey(resource.getService(),
                        EventType.CIRCUIT_BREAKING);
                ServiceRule serviceRule;
                try {
                    serviceRule = polarisCircuitBreaker.getServiceRuleProvider().getServiceRule(serviceEventKey);
                } catch (Throwable t) {
                    LOG.warn("fail to get resource for {}", serviceEventKey, t);
                    polarisCircuitBreaker.getPullRulesExecutors().schedule(pullTask, 5, TimeUnit.SECONDS);
                    return;
                }
                Map<Resource, ResourceCounters> resourceResourceCounters = polarisCircuitBreaker.getCountersCache()
                        .get(resource.getLevel());
                CircuitBreakerRule circuitBreakerRule = selectRule(serviceRule);
                if (null != circuitBreakerRule) {
                    ResourceCounters resourceCounters = resourceResourceCounters.get(resource);
                    if (null != resourceCounters) {
                        CircuitBreakerRule currentActiveRule = resourceCounters.getCurrentActiveRule();
                        if (StringUtils.equals(currentActiveRule.getId(), circuitBreakerRule.getId()) && StringUtils
                                .equals(currentActiveRule.getRevision(), circuitBreakerRule.getRevision())) {
                            return;
                        }
                    }
                    resourceResourceCounters.put(resource, new ResourceCounters(resource, circuitBreakerRule,
                            polarisCircuitBreaker.getStateChangeExecutors()));
                } else {
                    resourceResourceCounters.remove(resource);
                }
            }
        };
        schedule();
    }

    private CircuitBreakerRule selectRule(ServiceRule serviceRule) {
        Object rule = serviceRule.getRule();
        if (null == rule) {
            return null;
        }
        CircuitBreaker circuitBreaker = (CircuitBreaker) rule;
        List<CircuitBreakerRule> rules = circuitBreaker.getRulesList();
        if (rules.isEmpty()) {
            return null;
        }
        List<CircuitBreakerRule> wildcardRules = new ArrayList<>();
        for (CircuitBreakerRule cbRule : rules) {
            Level level = resource.getLevel();
            if (cbRule.getLevel() != level) {
                continue;
            }
            RuleMatcher ruleMatcher = cbRule.getRuleMatcher();
            if (null == ruleMatcher) {
                continue;
            }
            DestinationService destination = ruleMatcher.getDestination();
            if (isWildcardMatcher(destination.getService(), destination.getNamespace())) {
                wildcardRules.add(cbRule);
                continue;
            }
            boolean methodMatched = matchMethod(resource, destination.getMethod());
            if (methodMatched) {
                return cbRule;
            }
        }
        for (CircuitBreakerRule cbRule : wildcardRules) {
            RuleMatcher ruleMatcher = cbRule.getRuleMatcher();
            SourceService source = ruleMatcher.getSource();
            if (!isWildcardMatcher(source.getService(), source.getNamespace())) {
                continue;
            }
            return cbRule;
        }
        return null;
    }

    private boolean matchMethod(Resource resource, MatchString matchString) {
        if (resource.getLevel() != Level.METHOD) {
            return null == matchString || RuleUtils.isMatchAllValue(matchString);
        }
        String method = ((MethodResource) resource).getMethod();
        return RuleUtils.matchStringValue(matchString, method, regex -> {
            FlowCache flowCache = polarisCircuitBreaker.getExtensions().getFlowCache();
            return flowCache.loadOrStoreCompiledRegex(regex);
        });
    }

    private static boolean isWildcardMatcher(String service, String namespace) {
        return service.equals(RuleUtils.MATCH_ALL) || namespace.equals(RuleUtils.MATCH_ALL);
    }

    public void schedule() {
        polarisCircuitBreaker.getPullRulesExecutors().schedule(pullTask, 50, TimeUnit.MILLISECONDS);
    }
}
