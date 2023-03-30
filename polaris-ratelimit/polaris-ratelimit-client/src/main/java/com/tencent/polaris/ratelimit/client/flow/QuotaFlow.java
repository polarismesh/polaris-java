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

import com.tencent.polaris.api.config.provider.RateLimitConfig;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.ratelimiter.InitCriteria;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult.Code;
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
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.client.pojo.CommonQuotaRequest;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString.MatchStringType;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.MatchArgument;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.RateLimit;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public class QuotaFlow extends Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(QuotaFlow.class);

    private RateLimitExtension rateLimitExtension;

    private RateLimitConfig rateLimitConfig;

    /**
     * 客户端的唯一标识
     */
    private String clientId;

    private boolean enabled;

    private final Map<ServiceKey, RateLimitWindowSet> svcToWindowSet = new ConcurrentHashMap<>();

    public void init(Extensions extensions) throws PolarisException {
        clientId = extensions.getValueContext().getClientId();
        rateLimitExtension = new RateLimitExtension(extensions);
        rateLimitConfig = rateLimitExtension.getExtensions().getConfiguration().getProvider().getRateLimit();
        enabled = rateLimitConfig.isEnable();
        extensions.getLocalRegistry().registerResourceListener(new RateLimitRuleListener());
    }

    protected void doDestroy() {
        rateLimitExtension.destroy();
    }

    public QuotaResponse getQuota(CommonQuotaRequest request) throws PolarisException {
        if (!enabled) {
            return new QuotaResponse(
                    new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, RateLimitConstants.REASON_DISABLED));
        }
        List<RateLimitWindow> windows = lookupRateLimitWindow(request);
        if (CollectionUtils.isEmpty(windows)) {
            //没有限流规则，直接放通
            return new QuotaResponse(
                    new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, RateLimitConstants.REASON_RULE_NOT_EXISTS));
        }
        long maxWaitMs = 0;
        for (RateLimitWindow rateLimitWindow : windows) {
            rateLimitWindow.init();
            QuotaResponse quotaResponse = new QuotaResponse(rateLimitWindow.allocateQuota(request.getCount()));
            if (quotaResponse.getCode() == QuotaResultCode.QuotaResultLimited) {
                //一个限流则直接限流
                // 记录这个限流 RateLimitWindow 对应的限流规则信息，放到 QuotaResponse 中返回
                quotaResponse.setActiveRule(rateLimitWindow.getRule());
                return quotaResponse;
            }
            if (quotaResponse.getWaitMs() > maxWaitMs) {
                maxWaitMs = quotaResponse.getWaitMs();
            }
        }
        return new QuotaResponse(new QuotaResult(Code.QuotaResultOk, maxWaitMs, ""));
    }

    private List<RateLimitWindow> lookupRateLimitWindow(CommonQuotaRequest request) throws PolarisException {
        //1.获取限流规则
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(rateLimitExtension.getExtensions(), false, request, request.getFlowControlParam());
        ServiceRule serviceRule = resourcesResponse.getServiceRule(request.getSvcEventKey());
        //2.进行规则匹配
        List<RateLimitWindow> windows = new ArrayList<>();
        List<Rule> rules = lookupRules(serviceRule, request.getMethod(), request.getArguments());
        if (CollectionUtils.isEmpty(rules)) {
            return windows;
        }
        ServiceKey serviceKey = request.getSvcEventKey().getServiceKey();
        for (Rule rule : rules) {
            InitCriteria initCriteria = new InitCriteria();
            initCriteria.setRule(rule);
            String labelsStr = formatLabelsToStr(request, initCriteria);
            //3.获取已有的限流窗口
            RateLimitWindowSet rateLimitWindowSet = getRateLimitWindowSet(serviceKey);
            RateLimitWindow rateLimitWindow = rateLimitWindowSet.getRateLimitWindow(rule, labelsStr);
            if (null != rateLimitWindow) {
                windows.add(rateLimitWindow);
            } else {
                //3.创建限流窗口
                windows.add(rateLimitWindowSet.addRateLimitWindow(request, labelsStr, rateLimitConfig, initCriteria));
            }

        }
        return windows;
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

    private static String formatLabelsToStr(CommonQuotaRequest request, InitCriteria initCriteria) {
        Rule rule = initCriteria.getRule();
        MatchString method = rule.getMethod();
        boolean regexCombine = rule.getRegexCombine().getValue();
        String methodValue = "";
        if (null != method && !RuleUtils.isMatchAllValue(method)) {
            if (regexCombine && method.getType() != MatchStringType.EXACT) {
                methodValue = method.getValue().getValue();
            } else {
                methodValue = request.getMethod();
                if (method.getType() != MatchStringType.EXACT) {
                    //正则表达式扩散
                    initCriteria.setRegexSpread(true);
                }
            }
        }
        List<MatchArgument> argumentsList = rule.getArgumentsList();
        List<String> tmpList = new ArrayList<>();
        Map<Integer, Map<String, String>> arguments = request.getArguments();
        for (MatchArgument matchArgument : argumentsList) {
            String labelValue;
            MatchString matcher = matchArgument.getValue();
            if (regexCombine && matcher.getType() != MatchStringType.EXACT) {
                labelValue = matcher.getValue().getValue();
            } else {
                Map<String, String> stringStringMap = arguments.get(matchArgument.getType().ordinal());
                labelValue = getLabelValue(matchArgument, stringStringMap);
                if (matcher.getType() != MatchStringType.EXACT) {
                    //正则表达式扩散
                    initCriteria.setRegexSpread(true);
                }
            }
            String labelEntry = getLabelEntry(matchArgument, labelValue);
            if (StringUtils.isNotBlank(labelEntry)) {
                tmpList.add(labelEntry);
            }
        }
        Collections.sort(tmpList);
        return methodValue + RateLimitConstants.DEFAULT_ENTRY_SEPARATOR + String
                .join(RateLimitConstants.DEFAULT_ENTRY_SEPARATOR, tmpList);
    }

    private static String getLabelEntry(MatchArgument matchArgument, String labelValue) {
        switch (matchArgument.getType()) {
            case CUSTOM:
            case HEADER:
            case QUERY:
            case CALLER_SERVICE: {
                return matchArgument.getType().name() + RateLimitConstants.DEFAULT_KV_SEPARATOR + matchArgument.getKey()
                        + RateLimitConstants.DEFAULT_KV_SEPARATOR + labelValue;
            }
            case METHOD:
            case CALLER_IP: {
                return matchArgument.getType().name() + RateLimitConstants.DEFAULT_KV_SEPARATOR + labelValue;
            }
            default:
                return "";
        }
    }

    private static String getLabelValue(MatchArgument matchArgument,
            Map<String, String> stringStringMap) {
        switch (matchArgument.getType()) {
            case CUSTOM:
            case HEADER:
            case QUERY:
            case CALLER_SERVICE: {
                return stringStringMap.get(matchArgument.getKey());
            }
            case METHOD:
            case CALLER_IP: {
                return stringStringMap.values().iterator().next();
            }
            default:
                return stringStringMap.get(matchArgument.getKey());
        }
    }

    private List<Rule> lookupRules(ServiceRule serviceRule, String method,
            Map<Integer, Map<String, String>> arguments) {
        if (null == serviceRule || null == serviceRule.getRule()) {
            return null;
        }
        RateLimit rateLimitProto = (RateLimit) serviceRule.getRule();
        List<Rule> rulesList = rateLimitProto.getRulesList();
        if (CollectionUtils.isEmpty(rulesList)) {
            return null;
        }
        Function<String, Pattern> function = regex -> {
            FlowCache flowCache = rateLimitExtension.getExtensions().getFlowCache();
            return flowCache.loadOrStoreCompiledRegex(regex);
        };
        List<Rule> matchRules = new ArrayList<>();
        for (Rule rule : rulesList) {
            if (null != rule.getDisable() && rule.getDisable().getValue()) {
                continue;
            }
            if (rule.getAmountsCount() == 0) {
                //没有amount的规则就忽略
                continue;
            }
            //match method
            MatchString methodMatcher = rule.getMethod();
            if (null != methodMatcher) {
                boolean matchMethod = RuleUtils.matchStringValue(methodMatcher, method, function);
                if (!matchMethod) {
                    continue;
                }
            }
            List<MatchArgument> argumentsList = rule.getArgumentsList();
            boolean matched = true;
            if (CollectionUtils.isNotEmpty(argumentsList)) {
                for (MatchArgument matchArgument : argumentsList) {
                    Map<String, String> stringStringMap = arguments.get(matchArgument.getType().ordinal());
                    if (CollectionUtils.isEmpty(stringStringMap)) {
                        matched = false;
                        break;
                    }
                    String labelValue = getLabelValue(matchArgument, stringStringMap);
                    if (null == labelValue) {
                        matched = false;
                    } else {
                        matched = RuleUtils.matchStringValue(matchArgument.getValue(), labelValue, function);
                    }
                    if (!matched) {
                        break;
                    }
                }
            }
            if (matched) {
                matchRules.add(rule);
            }
        }
        return matchRules;
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


