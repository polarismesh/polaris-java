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

package com.tencent.polaris.plugins.connector.grpc.codec;

import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.cache.FlowCacheUtils;
import com.tencent.polaris.api.plugin.registry.AbstractCacheHandler;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.CircuitBreaker;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto.DiscoverResponse;

import java.util.ArrayList;
import java.util.List;

public class CircuitBreakCacheHandler extends AbstractCacheHandler {

    @Override
    public EventType getTargetEventType() {
        return EventType.CIRCUIT_BREAKING;
    }

    @Override
    protected String getRevision(DiscoverResponse discoverResponse) {
        CircuitBreaker circuitBreaker = discoverResponse.getCircuitBreaker();
        if (null == circuitBreaker) {
            return "";
        }
        return circuitBreaker.getRevision().getValue();
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded, FlowCache flowCache) {
        DiscoverResponse discoverResponse = (DiscoverResponse) newValue;
        CircuitBreaker circuitBreaker = discoverResponse.getCircuitBreaker();
        String revision = "";
        if (null != circuitBreaker) {
            revision = circuitBreaker.getRevision().getValue();
        }

        // 兼容老版本服务端
        CircuitBreaker.Builder newCircuitBreakerBuilder = CircuitBreaker.newBuilder().mergeFrom(circuitBreaker);
        List<CircuitBreakerProto.CircuitBreakerRule> circuitBreakerRuleList = newCircuitBreakerBuilder.getRulesList();
        if (CollectionUtils.isNotEmpty(circuitBreakerRuleList)) {
            List<CircuitBreakerProto.CircuitBreakerRule> newCircuitBreakerRuleList = new ArrayList<>();
            boolean needUpdate = false;
            for (CircuitBreakerProto.CircuitBreakerRule rule : circuitBreakerRuleList) {
                if (CollectionUtils.isNotEmpty(rule.getErrorConditionsList())
                        && CollectionUtils.isNotEmpty(rule.getTriggerConditionList())
                        && CollectionUtils.isEmpty(rule.getBlockConfigsList())) {
                    needUpdate = true;
                    CircuitBreakerProto.CircuitBreakerRule.Builder ruleBuilder = CircuitBreakerProto.CircuitBreakerRule.newBuilder().mergeFrom(rule);
                    CircuitBreakerProto.BlockConfig.Builder blockConfigBuilder = CircuitBreakerProto.BlockConfig.newBuilder();
                    blockConfigBuilder.addAllErrorConditions(rule.getErrorConditionsList());
                    blockConfigBuilder.addAllTriggerConditions(rule.getTriggerConditionList());
                    ModelProto.API.Builder apiBuilder = ModelProto.API.newBuilder();
                    apiBuilder.setProtocol("*");
                    apiBuilder.setMethod("*");
                    ModelProto.MatchString matchString = rule.getRuleMatcher().getDestination().getMethod();
                    apiBuilder.setPath(matchString);
                    FlowCacheUtils.saveApiTrie(matchString, flowCache);
                    blockConfigBuilder.setApi(apiBuilder.build());
                    ruleBuilder.addBlockConfigs(blockConfigBuilder.build());
                    newCircuitBreakerRuleList.add(ruleBuilder.build());
                } else {
                    // 缓存 CircuitBreakerRule 中的 api 树
                    List<CircuitBreakerProto.BlockConfig> blockConfigList = rule.getBlockConfigsList();
                    for (CircuitBreakerProto.BlockConfig blockConfig : blockConfigList) {
                        ModelProto.MatchString matchString = blockConfig.getApi().getPath();
                        FlowCacheUtils.saveApiTrie(matchString, flowCache);
                    }
                }
            }
            if (needUpdate) {
                newCircuitBreakerBuilder.clearRules();
                newCircuitBreakerBuilder.addAllRules(newCircuitBreakerRuleList);
                circuitBreaker = newCircuitBreakerBuilder.build();
            }
        }

        return new ServiceRuleByProto(circuitBreaker, revision, isCacheLoaded, getTargetEventType());
    }
}
