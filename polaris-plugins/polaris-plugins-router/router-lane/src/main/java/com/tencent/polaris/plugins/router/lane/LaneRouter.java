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

package com.tencent.polaris.plugins.router.lane;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.registry.EventCompleteNotifier;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.metadata.core.manager.MetadataContextHolder;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LaneRouter extends AbstractServiceRouter {

    private static final Logger LOG = LoggerFactory.getLogger(LaneRouter.class);

    /**
     * 处于泳道内的实例标签
     */
    public static final String INTERNAL_INSTANCE_LANE_KEY = "lane";

    /**
     * 泳道染色标签
     */
    public static final String TRAFFIC_STAIN_LABEL = "service-lane";

    private static final String GATEWAY_SELECTOR = "polarismesh.cn/gateway/spring-cloud-gateway";

    private static final String SERVICE_SELECTOR = "polarismesh.cn/service";

    private Function<ServiceKey, List<LaneProto.LaneGroup>> ruleGetter = serviceKey -> {
        if (Objects.isNull(serviceKey)) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(serviceKey.getService()) || StringUtils.isBlank(serviceKey.getNamespace())) {
            return Collections.emptyList();
        }

        ServiceEventKey eventKey = ServiceEventKey.builder()
                .serviceKey(serviceKey)
                .eventType(ServiceEventKey.EventType.LANE_RULE)
                .build();

        AtomicBoolean finish = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        ServiceRule outbound = extensions.getLocalRegistry().getServiceRule(new ResourceFilter(eventKey, true, false));

        // 之前已经 Load 成功了，就直接用
        if (outbound.isInitialized()) {
            Object rule = outbound.getRule();
            if (Objects.nonNull(rule)) {
                return ((ResponseProto.DiscoverResponse) rule).getLanesList();
            }
            return Collections.emptyList();
        }
        extensions.getLocalRegistry().loadServiceRule(eventKey, new EventCompleteNotifier() {
            @Override
            public void complete(ServiceEventKey svcEventKey) {
                if (finish.compareAndSet(false, true)) {
                    latch.countDown();
                }
            }

            @Override
            public void completeExceptionally(ServiceEventKey svcEventKey, Throwable throwable) {
                LOG.error("fetch lane group fail: {}", eventKey, throwable);
                if (finish.compareAndSet(false, true)) {
                    latch.countDown();
                }
            }
        });

        try {
            boolean ok = latch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }

        outbound = extensions.getLocalRegistry().getServiceRule(new ResourceFilter(eventKey, true, true));
        Object rule = outbound.getRule();
        if (Objects.nonNull(rule)) {
            return ((ResponseProto.DiscoverResponse) rule).getLanesList();
        }
        return Collections.emptyList();
    };

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public String getName() {
        return ServiceRouterConfig.DEFAULT_ROUTER_LANE;
    }


    @Override
    public Aspect getAspect() {
        return Aspect.MIDDLE;
    }

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances instances) throws PolarisException {
        MetadataContext manager = MetadataContextHolder.getOrCreate(MetadataContext::new);
        MessageMetadataContainer callerMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);
        MessageMetadataContainer calleeMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);

        LaneRuleContainer container = fetchLaneRules(routeInfo, instances);

        // 判断当前流量是否已存在染色
        String stainLabel = callerMsgContainer.getHeader(TRAFFIC_STAIN_LABEL);
        boolean alreadyStain = StringUtils.isNotBlank(stainLabel);

        Optional<LaneProto.LaneRule> targetRule;
        if (alreadyStain) {
            targetRule = container.matchRule(stainLabel);
        } else {
            // 当前流量无染色，根据泳道规则进行匹配判断
            targetRule = container.matchRule(routeInfo, manager);
        }

        // 泳道规则不存在，转为基线路由
        if (!targetRule.isPresent()) {
            return new RouteResult(redirectToBase(instances).getInstances(), RouteResult.State.Next);
        }
        LaneProto.LaneRule laneRule = targetRule.get();
        // 尝试进行流量染色动作，该操作仅在当前 Caller 服务为泳道入口时操作
        if (!tryStainCurrentTraffic(manager, routeInfo.getSourceService().getServiceKey(), container, laneRule)) {
            // 如果染色失败，即当前 Caller 不是泳道入口，不需要进行染色，只需要将已有的泳道标签进行透传
            if (alreadyStain) {
                calleeMsgContainer.setHeader(TRAFFIC_STAIN_LABEL, stainLabel, TransitiveType.PASS_THROUGH);
            }
        }

        List<Instance> resultInstances = tryRedirectToLane(container, laneRule, instances);
        if (CollectionUtils.isNotEmpty(resultInstances)) {
            return new RouteResult(resultInstances, RouteResult.State.Next);
        }
        // 严格模式，返回空实例列表
        if (laneRule.getMatchMode() == LaneProto.LaneRule.LaneMatchMode.STRICT) {
            return new RouteResult(Collections.emptyList(), RouteResult.State.Next);
        }
        // 宽松模式，降级为返回基线实例
        return new RouteResult(redirectToBase(instances).getInstances(), RouteResult.State.Next);
    }

    private List<Instance> tryRedirectToLane(LaneRuleContainer container, LaneProto.LaneRule rule, ServiceInstances instances) {
        LaneProto.LaneGroup group = container.groups.get(rule.getGroupName());
        if (Objects.isNull(group)) {
            // 泳道组不存在，直接认为不需要过滤实例, 默认转发至基线实例
            return redirectToBase(instances).getInstances();
        }
        // 判断目标服务是否属于泳道内服务
        boolean inLane = false;
        ServiceKey callee = instances.getServiceKey();
        for (RoutingProto.DestinationGroup destination : group.getDestinationsList()) {
            if (Objects.equals(callee.getNamespace(), destination.getNamespace()) && Objects.equals(callee.getService(), destination.getService())) {
                inLane = true;
                break;
            }
        }

        // 不在泳道内的服务，不需要进行实例过滤, 默认转发至基线实例
        if (!inLane) {
            return redirectToBase(instances).getInstances();
        }

        return instances.getInstances().stream().filter(instance -> {
            Map<String, String> metadata = instance.getMetadata();
            if (CollectionUtils.isEmpty(metadata)) {
                return false;
            }
            String val = metadata.get(INTERNAL_INSTANCE_LANE_KEY);
            return StringUtils.equals(val, rule.getDefaultLabelValue());
        }).collect(Collectors.toList());
    }

    private ServiceInstances redirectToBase(ServiceInstances instances) {
        List<Instance> result = instances.getInstances().stream().filter(instance -> {
            Map<String, String> metadata = instance.getMetadata();
            if (CollectionUtils.isEmpty(metadata)) {
                return true;
            }
            // 元数据中没有携带 lane 标签的实例均认为是基线实例
            return !metadata.containsKey(INTERNAL_INSTANCE_LANE_KEY);
        }).collect(Collectors.toList());
        return new DefaultServiceInstances(instances.getServiceKey(), result);
    }

    private boolean tryStainCurrentTraffic(MetadataContext manager, ServiceKey caller, LaneRuleContainer container, LaneProto.LaneRule rule) {
        LaneProto.LaneGroup group = container.groups.get(rule.getGroupName());
        if (Objects.isNull(group)) {
            // 泳道规则存在，但是对应的泳道组却不存在，这种情况需要直接抛出异常
            LOG.error("lane_group where lane_rule located not found, lane_rule: {}, lane_group: {}", rule.getName(), rule.getGroupName());
            throw new PolarisException(ErrorCode.INVALID_STATE, "lane_group where lane_rule located not found");
        }

        try {
            boolean needStain = false;
            for (LaneProto.TrafficEntry entry : group.getEntriesList()) {
                switch (entry.getType()) {
                    case GATEWAY_SELECTOR:
                        LaneProto.ServiceGatewaySelector gatewaySelector = entry.getSelector().unpack(LaneProto.ServiceGatewaySelector.class);
                        if (Objects.equals(caller.getNamespace(), gatewaySelector.getNamespace()) && Objects.equals(caller.getService(), gatewaySelector.getService())) {
                            needStain = true;
                        }
                        break;
                    case SERVICE_SELECTOR:
                        LaneProto.ServiceSelector serviceSelector = entry.getSelector().unpack(LaneProto.ServiceSelector.class);
                        if (Objects.equals(caller.getNamespace(), serviceSelector.getNamespace()) && Objects.equals(caller.getService(), serviceSelector.getService())) {
                            needStain = true;
                        }
                        break;
                }
            }

            if (needStain) {
                MessageMetadataContainer metadataContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
                metadataContainer.setHeader(TRAFFIC_STAIN_LABEL, buildStainLabel(rule), TransitiveType.PASS_THROUGH);
            }
            return needStain;
        } catch (InvalidProtocolBufferException e) {
            LOG.error("lane_rule: {}, lane_group: {} unpark traffic entry selector fail", rule.getName(), rule.getGroupName(), e);
            throw new PolarisException(ErrorCode.INVALID_RULE);
        }
    }

    private LaneRuleContainer fetchLaneRules(RouteInfo routeInfo, ServiceInstances instances) {
        List<LaneProto.LaneGroup> result = new ArrayList<>();
        // 获取泳道规则
        if (Objects.nonNull(routeInfo.getSourceService())) {
            result.addAll(ruleGetter.apply(routeInfo.getSourceService().getServiceKey()));
        }
        result.addAll(ruleGetter.apply(instances.getServiceKey()));
        return new LaneRuleContainer(result);
    }

    private static class LaneRuleContainer {
        private final Map<String, LaneProto.LaneGroup> groups = new HashMap<>();

        private final List<LaneProto.LaneRule> rules = new LinkedList<>();

        private final Map<String, LaneProto.LaneRule> ruleMapping = new HashMap<>();

        LaneRuleContainer(List<LaneProto.LaneGroup> list) {
            list.forEach(laneGroup -> {
                if (groups.containsKey(laneGroup.getName())) {
                    return;
                }
                groups.put(laneGroup.getName(), laneGroup);
                laneGroup.getRulesList().forEach(laneRule -> {
                    if (!laneRule.getEnable()) {
                        return;
                    }
                    String name = buildStainLabel(laneRule);
                    ruleMapping.put(name, laneRule);
                    rules.add(laneRule);
                });
            });

            // 数字越小，规则优先级越大
            rules.sort(Comparator.comparingInt(LaneProto.LaneRule::getPriority));
        }

        public Optional<LaneProto.LaneRule> matchRule(String labelValue) {
            LaneProto.LaneRule rule = ruleMapping.get(labelValue);
            return Optional.ofNullable(rule);
        }

        public Optional<LaneProto.LaneRule> matchRule(RouteInfo routeInfo, MetadataContext manager) {
            // 当前流量无染色，根据泳道规则进行匹配判断
            LaneProto.LaneRule targetRule = null;
            for (LaneProto.LaneRule rule : rules) {
                if (!rule.getEnable()) {
                    continue;
                }

                LaneProto.TrafficMatchRule matchRule = rule.getTrafficMatchRule();

                List<Boolean> booleans = new LinkedList<>();
                matchRule.getArgumentsList().forEach(sourceMatch -> {
                    String trafficValue = findTrafficValue(routeInfo, sourceMatch, manager);
                    switch (sourceMatch.getValue().getValueType()) {
                        case TEXT:
                            // 直接匹配
                            boolean a = RuleUtils.matchStringValue(sourceMatch.getValue().getType(), trafficValue,
                                    sourceMatch.getValue().getValue().getValue());
                            booleans.add(a);
                            break;
                        case VARIABLE:
                            boolean match = false;
                            String parameterKey = sourceMatch.getValue().getValue().getValue();
                            // 外部参数来源
                            Optional<String> parameter = routeInfo.getExternalParameterSupplier().apply(parameterKey);
                            if (parameter.isPresent()) {
                                match = RuleUtils.matchStringValue(sourceMatch.getValue().getType(), trafficValue,
                                        parameter.get());
                            }
                            if (!match) {
                                match = RuleUtils.matchStringValue(sourceMatch.getValue().getType(), trafficValue,
                                        System.getenv(parameterKey));
                            }
                            booleans.add(match);
                            break;
                    }
                });

                boolean isMatched = false;
                switch (matchRule.getMatchMode()) {
                    case OR:
                        for (Boolean a : booleans) {
                            isMatched = isMatched || a;
                        }
                        break;
                    case AND:
                        isMatched = true;
                        for (Boolean a : booleans) {
                            isMatched = isMatched && a;
                        }
                        break;
                }

                if (!isMatched) {
                    continue;
                }
                targetRule = rule;
                break;
            }

            return Optional.ofNullable(targetRule);
        }
    }

    private static String findTrafficValue(RouteInfo routeInfo, RoutingProto.SourceMatch sourceMatch, MetadataContext manager) {
        Map<String, String> trafficLabels = routeInfo.getRouterMetadata(ServiceRouterConfig.DEFAULT_ROUTER_LANE);

        MessageMetadataContainer calleeContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
        MessageMetadataContainer callerContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);

        String trafficValue = "";
        switch (sourceMatch.getType()) {
            case HEADER:
                String headerKey = RouteArgument.ArgumentType.HEADER.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(headerKey)) {
                    return trafficLabels.get(headerKey);
                }
                trafficValue = Optional.ofNullable(calleeContainer.getHeader(sourceMatch.getKey())).orElse(callerContainer.getHeader(sourceMatch.getKey()));
                break;
            case CUSTOM:
                String customKey = RouteArgument.ArgumentType.CUSTOM.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(customKey)) {
                    return trafficLabels.get(customKey);
                }
                trafficValue = Optional.ofNullable(calleeContainer.getRawMetadataStringValue(sourceMatch.getKey())).orElse(callerContainer.getRawMetadataStringValue(sourceMatch.getKey()));
                break;
            case METHOD:
                String methodKey = RouteArgument.ArgumentType.METHOD.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(methodKey)) {
                    return trafficLabels.get(methodKey);
                }
                trafficValue = Optional.ofNullable(calleeContainer.getMethod()).orElse(callerContainer.getMethod());
                break;
            case CALLER_IP:
                String callerIpKey = RouteArgument.ArgumentType.CALLER_IP.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(callerIpKey)) {
                    return trafficLabels.get(callerIpKey);
                }
                trafficValue = Optional.ofNullable(calleeContainer.getCallerIP()).orElse(callerContainer.getCallerIP());
                break;
            case COOKIE:
                String cookieKey = RouteArgument.ArgumentType.COOKIE.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(cookieKey)) {
                    return trafficLabels.get(cookieKey);
                }
                trafficValue = Optional.ofNullable(calleeContainer.getCookie(sourceMatch.getKey())).orElse(callerContainer.getCookie(sourceMatch.getKey()));
                break;
            case QUERY:
                String queryKey = RouteArgument.ArgumentType.QUERY.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(queryKey)) {
                    return trafficLabels.get(queryKey);
                }
                trafficValue = Optional.ofNullable(calleeContainer.getQuery(sourceMatch.getKey())).orElse(callerContainer.getQuery(sourceMatch.getKey()));
                break;
            case PATH:
                String pathKey = RouteArgument.ArgumentType.PATH.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(pathKey)) {
                    return trafficLabels.get(pathKey);
                }
                trafficValue = Optional.ofNullable(calleeContainer.getPath()).orElse(callerContainer.getPath());
                break;
        }
        return trafficValue;
    }

    private static String buildStainLabel(LaneProto.LaneRule rule) {
        return rule.getGroupName() + "/" + rule.getName();
    }

    @JustForTest
    public void setRuleGetter(Function<ServiceKey, List<LaneProto.LaneGroup>> ruleGetter) {
        this.ruleGetter = ruleGetter;
    }
}
