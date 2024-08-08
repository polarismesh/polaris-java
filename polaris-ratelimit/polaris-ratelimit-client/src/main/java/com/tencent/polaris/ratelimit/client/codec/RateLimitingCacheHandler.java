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

package com.tencent.polaris.ratelimit.client.codec;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.registry.AbstractCacheHandler;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.ratelimit.api.rpc.RateLimitConsts;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto.DiscoverResponse;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.MatchArgument;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.MatchArgument.Type;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.RateLimit;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RateLimitingCacheHandler extends AbstractCacheHandler {

    @Override
    public EventType getTargetEventType() {
        return EventType.RATE_LIMITING;
    }

    @Override
    protected String getRevision(DiscoverResponse discoverResponse) {
        RateLimit rateLimit = discoverResponse.getRateLimit();
        if (null == rateLimit) {
            return "";
        }
        return rateLimit.getRevision().getValue();
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded, FlowCache flowCache) {
        DiscoverResponse discoverResponse = (DiscoverResponse) newValue;
        RateLimit rateLimit = discoverResponse.getRateLimit();
        String revision = getRevision(discoverResponse);
        //需要做一次排序,PB中的数据不可变，需要单独构建一份
        List<Rule> sortedRules = unifiedRules(rateLimit.getRulesList());
        sortedRules.sort(RateLimitingCacheHandler::compareRule);
        Collections.reverse(sortedRules);
        RateLimit newRateLimit = RateLimit.newBuilder().addAllRules(sortedRules)
                .setRevision(StringValue.newBuilder().setValue(revision).build()).build();
        return new ServiceRuleByProto(newRateLimit, revision, isCacheLoaded, getTargetEventType());
    }

    private static final int RULE_SERVICE_LEVEL = 1;

    private static final int RULE_METHOD_LEVEL = 2;

    private static final int RULE_ARGUMENT_LEVEL = 3;

    private static int getRuleLevel(Rule rule) {
        MatchString method = rule.getMethod();
        List<MatchArgument> argumentsList = rule.getArgumentsList();
        if (CollectionUtils.isNotEmpty(argumentsList)) {
            return RULE_ARGUMENT_LEVEL + argumentsList.size();
        }
        if (null != method && !RuleUtils.isMatchAllValue(method)) {
            return RULE_METHOD_LEVEL;
        }
        return RULE_SERVICE_LEVEL;
    }

    private static int compareRule(Rule rule1, Rule rule2) {
        Rule.Type type1 = rule1.getType();
        Rule.Type type2 = rule2.getType();
        if (type1 != type2) {
            return type1.getNumber() - type2.getNumber();
        }
        return getRuleLevel(rule1) - getRuleLevel(rule2);
    }

    private List<Rule> unifiedRules(List<Rule> rules) {
        List<Rule> retRules = new ArrayList<>();
        if (CollectionUtils.isEmpty(rules)) {
            return rules;
        }
        for (Rule rule : rules) {
            if (CollectionUtils.isEmpty(rule.getLabelsMap())) {
                // not labels, nothing to convert
                retRules.add(rule);
                continue;
            }
            if (CollectionUtils.isNotEmpty(rule.getArgumentsList())) {
                // new server version, already transfer to arguments
                retRules.add(rule);
                continue;
            }
            // transfer the labels to arguments
            List<MatchArgument> arguments = new ArrayList<>();
            for (Map.Entry<String, MatchString> entry : rule.getLabelsMap().entrySet()) {
                String labelKey = StringUtils.defaultString(entry.getKey());
                if (StringUtils.equals(labelKey, RateLimitConsts.LABEL_KEY_METHOD)) {
                    arguments.add(MatchArgument.newBuilder().setType(Type.METHOD).setValue(entry.getValue()).build());
                } else if (StringUtils.equals(labelKey, RateLimitConsts.LABEL_KEY_CALLER_IP)) {
                    arguments
                            .add(MatchArgument.newBuilder().setType(Type.CALLER_IP).setValue(entry.getValue()).build());
                } else if (labelKey.startsWith(RateLimitConsts.LABEL_KEY_HEADER)) {
                    arguments
                            .add(MatchArgument.newBuilder().setType(Type.HEADER)
                                    .setKey(labelKey.substring(RateLimitConsts.LABEL_KEY_HEADER.length()))
                                    .setValue(entry.getValue()).build());
                } else if (labelKey.startsWith(RateLimitConsts.LABEL_KEY_QUERY)) {
                    arguments
                            .add(MatchArgument.newBuilder().setType(Type.QUERY)
                                    .setKey(labelKey.substring(RateLimitConsts.LABEL_KEY_QUERY.length()))
                                    .setValue(entry.getValue()).build());
                } else if (labelKey.startsWith(RateLimitConsts.LABEL_KEY_CALLER_SERVICE)) {
                    arguments
                            .add(MatchArgument.newBuilder().setType(Type.CALLER_SERVICE)
                                    .setKey(labelKey.substring(RateLimitConsts.LABEL_KEY_CALLER_SERVICE.length()))
                                    .setValue(entry.getValue()).build());
                } else {
                    arguments
                            .add(MatchArgument.newBuilder().setType(Type.CUSTOM)
                                    .setKey(labelKey).setValue(entry.getValue()).build());
                }
            }
            Rule.Builder retRule = Rule.newBuilder().mergeFrom(rule).addAllArguments(arguments);
            retRules.add(retRule.build());
        }
        return retRules;
    }
}
