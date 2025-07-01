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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.pojo.TrieNode;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.CompareUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.plugins.circuitbreaker.composite.utils.CircuitBreakerUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.tencent.polaris.plugins.circuitbreaker.composite.utils.MatchUtils.matchMethod;

public class CircuitBreakerRuleDictionary {

    private final Map<CircuitBreakerProto.Level, Cache<ServiceKey, List<CircuitBreakerProto.CircuitBreakerRule>>> allRules = new HashMap<>();

    private final Function<String, Pattern> regexToPattern;

    private final Function<String, TrieNode<String>> trieNodeFunction;

    private final Object updateLock = new Object();

    public CircuitBreakerRuleDictionary(Function<String, Pattern> regexToPattern, Function<String, TrieNode<String>> trieNodeFunction) {
        this.regexToPattern = regexToPattern;
        this.trieNodeFunction = trieNodeFunction;
        allRules.put(CircuitBreakerProto.Level.SERVICE, CacheBuilder.newBuilder().build());
        allRules.put(CircuitBreakerProto.Level.METHOD, CacheBuilder.newBuilder().build());
        allRules.put(CircuitBreakerProto.Level.INSTANCE, CacheBuilder.newBuilder().build());
    }

    public CircuitBreakerProto.CircuitBreakerRule lookupCircuitBreakerRule(Resource resource) {
        synchronized (updateLock) {
            Cache<ServiceKey, List<CircuitBreakerProto.CircuitBreakerRule>> serviceKeyListCache = allRules.get(resource.getLevel());
            if (null == serviceKeyListCache) {
                return null;
            }
            return selectRule(resource, serviceKeyListCache.getIfPresent(resource.getService()), regexToPattern, trieNodeFunction);
        }
    }

    private static CircuitBreakerProto.CircuitBreakerRule selectRule(Resource resource,
                                                                     List<CircuitBreakerProto.CircuitBreakerRule> sortedRules,
                                                                     Function<String, Pattern> regexToPattern,
                                                                     Function<String, TrieNode<String>> trieNodeFunction) {
        if (CollectionUtils.isEmpty(sortedRules)) {
            return null;
        }
        for (CircuitBreakerProto.CircuitBreakerRule cbRule : sortedRules) {
            CircuitBreakerProto.RuleMatcher ruleMatcher = cbRule.getRuleMatcher();
            CircuitBreakerProto.RuleMatcher.DestinationService destination = ruleMatcher.getDestination();
            if (!RuleUtils.matchService(resource.getService(), destination.getNamespace(), destination.getService())) {
                continue;
            }
            CircuitBreakerProto.RuleMatcher.SourceService source = ruleMatcher.getSource();
            if (!RuleUtils.matchService(resource.getCallerService(), source.getNamespace(), source.getService())) {
                continue;
            }
            List<CircuitBreakerProto.BlockConfig> blockConfigList = cbRule.getBlockConfigsList();
            for (CircuitBreakerProto.BlockConfig blockConfig : blockConfigList) {
                boolean methodMatched = matchMethod(resource, blockConfig.getApi(), regexToPattern, trieNodeFunction);
                if (methodMatched) {
                    return cbRule;
                }
            }
        }
        return null;
    }

    /**
     * rule on server has been changed, clear all caches to make it pull again
     *
     * @param serviceKey target service
     */
    public void onServiceChanged(ServiceKey serviceKey) {
        synchronized (updateLock) {
            clearRules(serviceKey);
        }
    }

    private void clearRules(ServiceKey serviceKey) {
        allRules.get(CircuitBreakerProto.Level.SERVICE).invalidate(serviceKey);
        allRules.get(CircuitBreakerProto.Level.METHOD).invalidate(serviceKey);
        allRules.get(CircuitBreakerProto.Level.INSTANCE).invalidate(serviceKey);
    }

    public void putServiceRule(ServiceKey serviceKey, ServiceRule serviceRule) {
        synchronized (updateLock) {
            if (null == serviceRule || null == serviceRule.getRule()) {
                clearRules(serviceKey);
                return;
            }
            CircuitBreakerProto.CircuitBreaker circuitBreaker = (CircuitBreakerProto.CircuitBreaker) serviceRule.getRule();
            List<CircuitBreakerProto.CircuitBreakerRule> rules = circuitBreaker.getRulesList();
            List<CircuitBreakerProto.CircuitBreakerRule> subServiceRules = new ArrayList<>();
            List<CircuitBreakerProto.CircuitBreakerRule> subMethodRules = new ArrayList<>();
            List<CircuitBreakerProto.CircuitBreakerRule> subInstanceRules = new ArrayList<>();
            for (CircuitBreakerProto.CircuitBreakerRule rule : rules) {
                if (!rule.getEnable() || !CircuitBreakerUtils.checkRule(rule)) {
                    continue;
                }
                CircuitBreakerProto.Level level = rule.getLevel();
                switch (level) {
                    case SERVICE:
                        subServiceRules.add(rule);
                        break;
                    case INSTANCE:
                        subInstanceRules.add(rule);
                        break;
                    case METHOD:
                        subMethodRules.add(rule);
                        break;
                }
            }
            subServiceRules = sortCircuitBreakerRules(subServiceRules);
            subMethodRules = sortCircuitBreakerRules(subMethodRules);
            subInstanceRules = sortCircuitBreakerRules(subInstanceRules);
            allRules.get(CircuitBreakerProto.Level.SERVICE).put(serviceKey, subServiceRules);
            allRules.get(CircuitBreakerProto.Level.METHOD).put(serviceKey, subMethodRules);
            allRules.get(CircuitBreakerProto.Level.INSTANCE).put(serviceKey, subInstanceRules);
        }
    }

    private static List<CircuitBreakerProto.CircuitBreakerRule> sortCircuitBreakerRules(List<CircuitBreakerProto.CircuitBreakerRule> rules) {
        if (CollectionUtils.isEmpty(rules)) {
            return rules;
        }
        List<CircuitBreakerProto.CircuitBreakerRule> outRules = new ArrayList<>(rules);
        outRules.sort((rule1, rule2) -> {
            // 1. compare priority
            int priorityResult = rule1.getPriority() - rule2.getPriority();
            if (priorityResult != 0) {
                return priorityResult;
            }

            // 2. compare destination service
            CircuitBreakerProto.RuleMatcher ruleMatcher1 = rule1.getRuleMatcher();
            String destNamespace1 = ruleMatcher1.getDestination().getNamespace();
            String destService1 = ruleMatcher1.getDestination().getService();

            CircuitBreakerProto.RuleMatcher ruleMatcher2 = rule2.getRuleMatcher();
            String destNamespace2 = ruleMatcher2.getDestination().getNamespace();
            String destService2 = ruleMatcher2.getDestination().getService();

            int svcResult = CompareUtils.compareService(destNamespace1, destService1, destNamespace2, destService2);
            if (svcResult != 0) {
                return svcResult;
            }

            // 3. compare rule ID
            String id1 = rule1.getId();
            String id2 = rule2.getId();
            return CompareUtils.compareSingleValue(id1, id2);
        });
        return outRules;
    }
}
