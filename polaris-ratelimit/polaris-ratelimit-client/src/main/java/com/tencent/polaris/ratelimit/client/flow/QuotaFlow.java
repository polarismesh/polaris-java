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

package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.api.plugin.registry.AbstractResourceEventListener;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.client.pb.ModelProto.MatchString;
import com.tencent.polaris.client.pb.ModelProto.MatchString.MatchStringType;
import com.tencent.polaris.client.pb.RateLimitProto.RateLimit;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.client.pojo.CommonQuotaRequest;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuotaFlow extends Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(QuotaFlow.class);

    private RateLimitExtension rateLimitExtension;
    /**
     * 客户端的唯一标识
     */
    private String clientId;

    private final Map<ServiceKey, RateLimitWindowSet> svcToWindowSet = new ConcurrentHashMap<>();

    public void init(Extensions extensions) throws PolarisException {
        clientId = extensions.getValueContext().getClientId();
        rateLimitExtension = new RateLimitExtension(extensions);
        extensions.getLocalRegistry().registerResourceListener(new RateLimitRuleListener());
        rateLimitExtension.submitExpireJob(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<ServiceKey, RateLimitWindowSet> entry : svcToWindowSet.entrySet()) {
                    entry.getValue().cleanupContainers();
                }
            }
        });
    }

    protected void doDestroy() {
        rateLimitExtension.destroy();
    }

    public QuotaResponse getQuota(CommonQuotaRequest request) throws PolarisException {
        RateLimitWindow rateLimitWindow = lookupRateLimitWindow(request);
        if (null == rateLimitWindow) {
            //没有限流规则，直接放通
            return new QuotaResponse(
                    new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, RateLimitConstants.RULE_NOT_EXISTS));
        }
        rateLimitWindow.init();
        return new QuotaResponse(rateLimitWindow.allocateQuota(request.getCount()));
    }

    private RateLimitWindow lookupRateLimitWindow(CommonQuotaRequest request) throws PolarisException {
        //1.获取限流规则
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(rateLimitExtension.getExtensions(), false, request, request.getFlowControlParam());
        ServiceRule serviceRule = resourcesResponse.getServiceRule(request.getSvcEventKey());
        //2.进行规则匹配
        Rule rule = lookupRule(serviceRule, request.getLabels());
        if (null == rule) {
            return null;
        }
        request.setTargetRule(rule);
        //3.获取已有的限流窗口
        ServiceKey serviceKey = request.getSvcEventKey().getServiceKey();
        String labelsStr = formatLabelsToStr(request);
        RateLimitWindowSet rateLimitWindowSet = getRateLimitWindowSet(serviceKey);
        RateLimitWindow rateLimitWindow = rateLimitWindowSet.getRateLimitWindow(rule, labelsStr);
        if (null != rateLimitWindow) {
            return rateLimitWindow;
        }
        //3.创建限流窗口
        return rateLimitWindowSet.addRateLimitWindow(request, labelsStr);
    }

    private RateLimitWindowSet getRateLimitWindowSet(ServiceKey serviceKey) {
        RateLimitWindowSet rateLimitWindowSet = svcToWindowSet.get(serviceKey);
        if (null != rateLimitWindowSet) {
            return rateLimitWindowSet;
        }
        return svcToWindowSet.computeIfAbsent(serviceKey, new Function<ServiceKey, RateLimitWindowSet>() {
            @Override
            public RateLimitWindowSet apply(ServiceKey serviceKey) {
                return new RateLimitWindowSet(serviceKey, rateLimitExtension, clientId);
            }
        });
    }

    private static String formatLabelsToStr(CommonQuotaRequest request) {
        Rule rule = request.getInitCriteria().getRule();
        Map<String, String> labels = request.getLabels();
        if (rule.getLabelsCount() == 0 || MapUtils.isEmpty(labels)) {
            return "";
        }
        List<String> tmpList = new ArrayList<>();
        boolean regexCombine = rule.getRegexCombine().getValue();
        Map<String, MatchString> labelsMap = rule.getLabelsMap();
        for (Map.Entry<String, MatchString> entry : labelsMap.entrySet()) {
            MatchString matcher = entry.getValue();
            String labelEntry;
            if (matcher.getType() == MatchStringType.REGEX && regexCombine) {
                labelEntry = entry.getKey() + RateLimitConstants.DEFAULT_KV_SEPARATOR + matcher.getValue().getValue();
            } else {
                labelEntry = entry.getKey() + RateLimitConstants.DEFAULT_KV_SEPARATOR + labels.get(entry.getKey());
                if (matcher.getType() == MatchStringType.REGEX) {
                    //正则表达式扩散
                    request.setRegexSpread(true);
                }
            }
            tmpList.add(labelEntry);
        }
        Collections.sort(tmpList);
        return String.join(RateLimitConstants.DEFAULT_ENTRY_SEPARATOR, tmpList);
    }

    private Rule lookupRule(ServiceRule serviceRule, Map<String, String> labels) {
        if (null == serviceRule.getRule()) {
            return null;
        }
        RateLimit rateLimitProto = (RateLimit) serviceRule.getRule();
        List<Rule> rulesList = rateLimitProto.getRulesList();
        if (CollectionUtils.isEmpty(rulesList)) {
            return null;
        }
        for (Rule rule : rulesList) {
            if (null != rule.getDisable() && rule.getDisable().getValue()) {
                continue;
            }
            if (rule.getAmountsCount() == 0) {
                //没有amount的规则就忽略
                continue;
            }
            if (rule.getLabelsCount() == 0) {
                return rule;
            }
            boolean allMatchLabels = true;
            Map<String, MatchString> labelsMap = rule.getLabelsMap();
            for (Map.Entry<String, MatchString> entry : labelsMap.entrySet()) {
                if (!matchLabels(entry.getKey(), entry.getValue(), labels)) {
                    allMatchLabels = false;
                    break;
                }
            }
            if (allMatchLabels) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchLabels(String ruleLabelKey, MatchString ruleLabelMatch, Map<String, String> labels) {
        //设置了MatchAllValue，相当于这个规则就无效了
        if (RuleUtils.isMatchAllValue(ruleLabelMatch)) {
            return true;
        }
        if (MapUtils.isEmpty(labels)) {
            return false;
        }
        //集成的路由规则不包含这个key，就不匹配
        if (!labels.containsKey(ruleLabelKey)) {
            return false;
        }
        String labelValue = labels.get(ruleLabelKey);
        FlowCache flowCache = rateLimitExtension.getExtensions().getFlowCache();
        MatchStringType matchType = ruleLabelMatch.getType();
        String matchValue = ruleLabelMatch.getValue().getValue();
        if (matchType == MatchStringType.REGEX) {
            //正则表达式匹配
            Pattern pattern = flowCache.loadOrStoreCompiledRegex(matchValue);
            return pattern.matcher(labelValue).find();
        }
        return StringUtils.equals(labelValue, matchValue);
    }

    private static Map<String, Rule> parseRules(RegistryCacheValue oldValue) {
        if (null == oldValue || !oldValue.isInitialized()) {
            return null;
        }
        ServiceRule serviceRule = (ServiceRule) oldValue;
        if (null == serviceRule.getRule()) {
            return null;
        }
        Map<String, Rule> ruleMap = new HashMap<>();
        RateLimit rateLimit = (RateLimit) serviceRule.getRule();
        for (Rule rule : rateLimit.getRulesList()) {
            ruleMap.put(rule.getRevision().getValue(), rule);
        }
        return ruleMap;
    }

    private void deleteRules(ServiceKey serviceKey, Set<String> deletedRules) {
        LOG.info("[RateLimit]start to delete rules {} for service {}", deletedRules, serviceKey);
        RateLimitWindowSet rateLimitWindowSet = svcToWindowSet.get(serviceKey);
        if (null == rateLimitWindowSet) {
            return;
        }
        rateLimitWindowSet.deleteRules(deletedRules);
    }

    private class RateLimitRuleListener extends AbstractResourceEventListener {


        @Override
        public void onResourceUpdated(ServiceEventKey svcEventKey, RegistryCacheValue oldValue,
                                      RegistryCacheValue newValue) {
            EventType eventType = svcEventKey.getEventType();
            if (eventType != EventType.RATE_LIMITING) {
                return;
            }
            Map<String, Rule> oldRules = parseRules(oldValue);
            Map<String, Rule> newRules = parseRules(newValue);
            if (MapUtils.isEmpty(oldRules)) {
                return;
            }
            Set<String> deletedRules = new HashSet<>();
            for (Map.Entry<String, Rule> entry : oldRules.entrySet()) {
                if (MapUtils.isEmpty(newRules) || !newRules.containsKey(entry.getKey())) {
                    deletedRules.add(entry.getKey());
                }
            }
            if (CollectionUtils.isNotEmpty(deletedRules)) {
                deleteRules(svcEventKey.getServiceKey(), deletedRules);
            }
        }

        @Override
        public void onResourceDeleted(ServiceEventKey svcEventKey, RegistryCacheValue oldValue) {
            EventType eventType = svcEventKey.getEventType();
            if (eventType != EventType.RATE_LIMITING) {
                return;
            }
            Map<String, Rule> oldRules = parseRules(oldValue);
            if (MapUtils.isEmpty(oldRules)) {
                return;
            }
            deleteRules(svcEventKey.getServiceKey(), oldRules.keySet());
        }
    }

}


