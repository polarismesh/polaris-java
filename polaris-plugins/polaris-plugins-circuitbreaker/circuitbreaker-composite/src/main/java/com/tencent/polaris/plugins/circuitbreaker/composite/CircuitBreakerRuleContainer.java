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
import com.tencent.polaris.api.pojo.ServiceKey;
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
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetector;
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

    private final Runnable pullCbRuleTask;

    private final Runnable pullFdRuleTask;

    public CircuitBreakerRuleContainer(Resource resource, PolarisCircuitBreaker polarisCircuitBreaker) {
        this.resource = resource;
        this.polarisCircuitBreaker = polarisCircuitBreaker;
        pullCbRuleTask = new Runnable() {
            @Override
            public void run() {
                ServiceEventKey cbEventKey = new ServiceEventKey(resource.getService(),
                        EventType.CIRCUIT_BREAKING);
                ServiceRule circuitBreakRule;
                try {
                    circuitBreakRule = polarisCircuitBreaker.getServiceRuleProvider().getServiceRule(cbEventKey);
                } catch (Throwable t) {
                    LOG.warn("fail to get resource for {}", cbEventKey, t);
                    polarisCircuitBreaker.getPullRulesExecutors().schedule(pullCbRuleTask, 5, TimeUnit.SECONDS);
                    return;
                }
                Map<Resource, ResourceCounters> resourceResourceCounters = polarisCircuitBreaker.getCountersCache()
                        .get(resource.getLevel());
                CircuitBreakerRule circuitBreakerRule = selectRule(circuitBreakRule);
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
        pullFdRuleTask = new Runnable() {
            @Override
            public void run() {
                ServiceEventKey fdEventKey = new ServiceEventKey(resource.getService(),
                        EventType.FAULT_DETECTING);
                ServiceRule faultDetectRule;
                try {
                    faultDetectRule = polarisCircuitBreaker.getServiceRuleProvider().getServiceRule(fdEventKey);
                } catch (Throwable t) {
                    LOG.warn("fail to get resource for {}", fdEventKey, t);
                    polarisCircuitBreaker.getPullRulesExecutors().schedule(pullFdRuleTask, 5, TimeUnit.SECONDS);
                    return;
                }
                FaultDetector faultDetector = selectFaultDetector(faultDetectRule);
                Map<Resource, ResourceHealthChecker> healthCheckCache = polarisCircuitBreaker.getHealthCheckCache();
                if (null != faultDetector) {
                    ResourceHealthChecker curChecker = healthCheckCache.get(resource);
                    if (null != curChecker) {
                        FaultDetector currentRule = curChecker.getFaultDetector();
                        if (StringUtils.equals(currentRule.getRevision(), currentRule.getRevision())) {
                            return;
                        }
                        curChecker.setStopped(true);
                    }
                    healthCheckCache
                            .put(resource, new ResourceHealthChecker(resource, faultDetector, polarisCircuitBreaker));
                } else {
                    ResourceHealthChecker preChecker = healthCheckCache.remove(resource);
                    if (null != preChecker) {
                        preChecker.setStopped(true);
                    }
                }
            }
        };
        schedule();
    }

    private FaultDetector selectFaultDetector(ServiceRule serviceRule) {
        if (null == serviceRule) {
            return null;
        }
        Object rule = serviceRule.getRule();
        if (null == rule) {
            return null;
        }
        return (FaultDetector) rule;
    }

    private CircuitBreakerRule selectRule(ServiceRule serviceRule) {
        if (null == serviceRule) {
            return null;
        }
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
            if (!cbRule.getEnable()){
                continue;
            }
            Level level = resource.getLevel();
            if (cbRule.getLevel() != level) {
                continue;
            }
            RuleMatcher ruleMatcher = cbRule.getRuleMatcher();
            if (null == ruleMatcher) {
                continue;
            }

            SourceService source = ruleMatcher.getSource();
            if (null != source && !matchService(resource.getCallerService(), source.getNamespace(),
                    source.getService())) {
                continue;
            }

            DestinationService destination = ruleMatcher.getDestination();
            if (isWildcardMatcher(destination.getService(), destination.getNamespace())) {
                wildcardRules.add(cbRule);
                continue;
            }
            if (!matchService(resource.getService(), destination.getNamespace(), destination.getService())) {
                continue;
            }
            boolean methodMatched = matchMethod(resource, destination.getMethod());
            if (methodMatched) {
                return cbRule;
            }
        }
        for (CircuitBreakerRule cbRule : wildcardRules) {
            return cbRule;
        }
        return null;
    }

    private boolean matchService(ServiceKey serviceKey, String namespace, String service) {
        String inputNamespace = "";
        String inputService = "";
        if (null != serviceKey) {
            inputNamespace = serviceKey.getNamespace();
            inputService = serviceKey.getService();
        }
        if (StringUtils.isNotBlank(namespace) && !StringUtils.equals(namespace, RuleUtils.MATCH_ALL) && !StringUtils
                .equals(inputNamespace, namespace)) {
            return false;
        }
        if (StringUtils.isNotBlank(service) && !StringUtils.equals(service, RuleUtils.MATCH_ALL) && !StringUtils
                .equals(inputService, service)) {
            return false;
        }
        return true;
    }


    private boolean matchMethod(Resource resource, MatchString matchString) {
        if (resource.getLevel() != Level.METHOD) {
            return true;
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
        polarisCircuitBreaker.getPullRulesExecutors().schedule(pullCbRuleTask, 50, TimeUnit.MILLISECONDS);
        polarisCircuitBreaker.getPullRulesExecutors().schedule(pullFdRuleTask, 100, TimeUnit.MILLISECONDS);
    }
}
