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
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
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
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
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
import java.util.stream.Collectors;

public class LaneRouter extends AbstractServiceRouter {

    private static final Logger LOG = LoggerFactory.getLogger(LaneRouter.class);

    private static final String INTERNAL_INSTANCE_LANE_KEY = "lane";

    private static final String TRAFFIC_STAIN_LABEL = "service-lane";

    private static final String GATEWAY_SELECTOR = "polarismesh.cn/gateway/spring-cloud-gateway";

    private static final String SERVICE_SELECTOR = "polarismesh.cn/service";

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
        Map<String, String> trafficLabels = routeInfo.getRouterMetadata(ServiceRouterConfig.DEFAULT_ROUTER_LANE);
        LaneRuleContainer container = fetchLaneRules(routeInfo, instances);

        // 判断当前流量是否已存在染色
        String stainLabel = trafficLabels.get(TRAFFIC_STAIN_LABEL);
        boolean alreadyStain = StringUtils.isNotBlank(stainLabel);

        Optional<LaneProto.LaneRule> targetRule;
        if (alreadyStain) {
            targetRule = container.matchRule(stainLabel);
        } else {
            // 当前流量无染色，根据泳道规则进行匹配判断
            targetRule = container.matchRule(routeInfo, instances);
        }

        // 泳道规则不存在，转为基线路由
        if (!targetRule.isPresent()) {
            LOG.debug("not found target lane_rule, redirect to a non-lane instance, traffic_labels: {}", trafficLabels);
            return new RouteResult(redirectToBase(instances).getInstances(), RouteResult.State.Next);
        }
        LaneProto.LaneRule laneRule = targetRule.get();
        if (!alreadyStain) {
            // 尝试进行流量染色动作，该操作仅在当前 Caller 服务为泳道入口时操作
            tryStainCurrentTraffic(routeInfo.getSourceService().getServiceKey(), container, laneRule);
        } else {
            // TODO 传递染色标签, 将其塞入 LabelManager 中进行传递
        }

        List<Instance> resultInstances = tryRedirectToLane(container, laneRule, instances);
        if (CollectionUtils.isEmpty(resultInstances)) {
            switch (laneRule.getMatchMode()) {
                case STRICT:
                    // 严格模式，返回空实例列表
                    return new RouteResult(Collections.emptyList(), RouteResult.State.Next);
                case PERMISSIVE:
                    // 宽松模式，降级为返回基线实例
                    return new RouteResult(redirectToBase(instances).getInstances(), RouteResult.State.Next);
            }
        }
        return new RouteResult(resultInstances, RouteResult.State.Next);
    }

    private List<Instance> tryRedirectToLane(LaneRuleContainer container, LaneProto.LaneRule rule, ServiceInstances instances) {
        LaneProto.LaneGroup group = container.groups.get(rule.getGroupName());
        if (Objects.isNull(group)) {
            // 泳道组不存在，直接认为不需要过滤实例
            return instances.getInstances();
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

        // 不在泳道内的服务，不需要进行实例过滤
        if (!inLane) {
            return instances.getInstances();
        }

        return instances.getInstances().stream().filter(instance -> {
            Map<String, String> metadata = instance.getMetadata();
            if (CollectionUtils.isEmpty(metadata)) {
                return false;
            }
            // 元数据中没有携带 lane 标签的实例均认为是基线实例
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

    private void tryStainCurrentTraffic(ServiceKey caller, LaneRuleContainer container, LaneProto.LaneRule rule) {
        LaneProto.LaneGroup group = container.groups.get(rule.getGroupName());
        if (Objects.isNull(group)) {
            // 泳道规则存在，但是对应的泳道组却不存在，这种情况需要直接抛出异常
            LOG.debug("lane_group where lane_rule located not found, lane_rule: {}, lane_group: {}", rule.getName(), rule.getGroupName());
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
                // TODO 将染色标签塞入 LabelManager 中进行传递
            }
        } catch (InvalidProtocolBufferException e) {
            throw new PolarisException(ErrorCode.INVALID_RULE);
        }
    }

    private LaneRuleContainer fetchLaneRules(RouteInfo routeInfo, ServiceInstances instances) {
        List<LaneProto.LaneGroup> result = new ArrayList<>();
        // 获取泳道规则
        ServiceRule outbound = extensions.getLocalRegistry().getServiceRule(new ResourceFilter(ServiceEventKey.builder()
                .serviceKey(routeInfo.getSourceService().getServiceKey())
                .eventType(ServiceEventKey.EventType.LANE_RULE)
                .build(), true, true));

        Object rule = outbound.getRule();
        if (Objects.nonNull(rule)) {
            result.addAll((List<LaneProto.LaneGroup>) rule);
        }

        ServiceRule inbound = extensions.getLocalRegistry().getServiceRule(new ResourceFilter(ServiceEventKey.builder()
                .serviceKey(instances.getServiceKey())
                .eventType(ServiceEventKey.EventType.LANE_RULE)
                .build(), true, true));

        rule = inbound.getRule();
        if (Objects.nonNull(rule)) {
            result.addAll((List<LaneProto.LaneGroup>) rule);
        }

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
                    String name = laneGroup.getName() + "/" + laneRule.getName();
                    ruleMapping.put(name, laneRule);
                });
                rules.addAll(laneGroup.getRulesList());
            });

            // 数字越小，规则优先级越大
            rules.sort(Comparator.comparingInt(LaneProto.LaneRule::getPriority));
        }

        public Optional<LaneProto.LaneRule> matchRule(String labelValue) {
            LaneProto.LaneRule rule = ruleMapping.get(labelValue);
            return Optional.ofNullable(rule);
        }

        public Optional<LaneProto.LaneRule> matchRule(RouteInfo routeInfo, ServiceInstances instances) {
            Map<String, String> trafficLabels = routeInfo.getRouterMetadata(ServiceRouterConfig.DEFAULT_ROUTER_LANE);

            // 当前流量无染色，根据泳道规则进行匹配判断
            LaneProto.LaneRule targetRule = null;
            for (LaneProto.LaneRule rule : rules) {
                LaneProto.TrafficMatchRule matchRule = rule.getTrafficMatchRule();

                List<Boolean> booleans = new LinkedList<>();
                matchRule.getArgumentsList().forEach(sourceMatch -> {
                    String key = formatSourceMatchKey(sourceMatch);
                    switch (sourceMatch.getValue().getValueType()) {
                        case TEXT:
                            // 直接匹配
                            boolean a = RuleUtils.matchStringValue(sourceMatch.getValue().getType(), trafficLabels.get(key),
                                    sourceMatch.getValue().getValue().getValue());
                            booleans.add(a);
                        case VARIABLE:
                            boolean match = false;
                            String parameterKey = sourceMatch.getValue().getValue().getValue();
                            // 外部参数来源
                            Optional<String> parameter = routeInfo.getExternalParameterSupplier().search(parameterKey);
                            if (parameter.isPresent()) {
                                match = RuleUtils.matchStringValue(sourceMatch.getValue().getType(), trafficLabels.get(key),
                                        parameter.get());
                            }
                            if (!match) {
                                match = RuleUtils.matchStringValue(sourceMatch.getValue().getType(), trafficLabels.get(key),
                                        System.getenv(key));
                            }
                            booleans.add(match);
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

    private static String formatSourceMatchKey(RoutingProto.SourceMatch match) {
        switch (match.getType()) {
            case HEADER:
                return RouteArgument.ArgumentType.HEADER.key(match.getKey());
            case PATH:
                return RouteArgument.ArgumentType.PATH.key(match.getKey());
            case QUERY:
                return RouteArgument.ArgumentType.QUERY.key(match.getKey());
            case COOKIE:
                return RouteArgument.ArgumentType.COOKIE.key(match.getKey());
            case METHOD:
                return RouteArgument.ArgumentType.METHOD.key(match.getKey());
            case CALLER_IP:
                return RouteArgument.ArgumentType.CALLER_IP.key(match.getKey());
            case CUSTOM:
                return RouteArgument.ArgumentType.CUSTOM.key(match.getKey());
            default:
                return match.getKey();
        }
    }

}
