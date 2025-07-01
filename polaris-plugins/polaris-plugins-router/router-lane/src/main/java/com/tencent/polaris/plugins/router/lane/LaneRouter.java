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

package com.tencent.polaris.plugins.router.lane;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.CompareUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.metadata.core.manager.MetadataContextHolder;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

import java.util.*;
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

    private final Function<ServiceKey, List<LaneProto.LaneGroup>> ruleGetter = serviceKey -> {
        if (Objects.isNull(serviceKey)) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(serviceKey.getService()) || StringUtils.isBlank(serviceKey.getNamespace())) {
            return Collections.emptyList();
        }

        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(new RequestBaseEntity(), extensions.getConfiguration(), engineFlowControlParam);
        Set<ServiceEventKey> routerKeys = new HashSet<>();
        ServiceEventKey dstSvcEventKey = ServiceEventKey.builder().serviceKey(serviceKey).eventType(ServiceEventKey.EventType.LANE_RULE).build();
        routerKeys.add(dstSvcEventKey);
        DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
        svcKeysProvider.setSvcEventKeys(routerKeys);
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(extensions, false, svcKeysProvider, engineFlowControlParam);
        ServiceRule outbound = resourcesResponse.getServiceRule(dstSvcEventKey);
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
        MetadataContext manager = MetadataContextHolder.getOrCreate();
        MessageMetadataContainer callerMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);
        MessageMetadataContainer calleeMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
        ServiceKey caller = routeInfo.getSourceService() == null ? null : routeInfo.getSourceService().getServiceKey();
        ServiceKey callee = instances.getServiceKey() == null ? null : instances.getServiceKey();

        LaneRuleContainer container = fetchLaneRules(manager, caller, callee);

        // get callee lane group list
        List<LaneProto.LaneGroup> laneGroupList = container.getGroupListByCalleeNamespaceAndService(callee);

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
            return new RouteResult(redirectToBase(laneGroupList, instances), RouteResult.State.Next);
        }

        LaneProto.LaneRule laneRule = targetRule.get();
        // 尝试进行流量染色动作，该操作仅在当前 Caller 服务为泳道入口时操作
        boolean stainOK = tryStainCurrentTraffic(manager, caller, container, laneRule);
        if (!stainOK) {
            // 如果染色失败，即当前 Caller 不是泳道入口，不需要进行染色，只需要将已有的泳道标签进行透传
            if (alreadyStain) {
                calleeMsgContainer.setHeader(TRAFFIC_STAIN_LABEL, stainLabel, TransitiveType.PASS_THROUGH);
            } else {
                LOG.debug("current traffic not in lane, redirect to base, caller: {} callee: {}", caller, instances.getServiceKey());
                // 如果当前自己不是泳道入口，并且没有发现已经染色的标签，不能走泳道路由，
                return new RouteResult(redirectToBase(laneGroupList, instances), RouteResult.State.Next);
            }
        }

        List<Instance> resultInstances = tryRedirectToLane(container, laneRule, laneGroupList, instances);
        if (CollectionUtils.isNotEmpty(resultInstances)) {
            return new RouteResult(resultInstances, RouteResult.State.Next);
        }
        // 严格模式，返回空实例列表
        if (laneRule.getMatchMode() == LaneProto.LaneRule.LaneMatchMode.STRICT) {
            return new RouteResult(Collections.emptyList(), RouteResult.State.Next);
        }
        // 宽松模式，降级为返回基线实例
        return new RouteResult(redirectToBase(laneGroupList, instances), RouteResult.State.Next);
    }

    private List<Instance> tryRedirectToLane(LaneRuleContainer container, LaneProto.LaneRule rule,
                                             List<LaneProto.LaneGroup> laneGroupList, ServiceInstances instances) {
        LaneProto.LaneGroup group = container.groups.get(rule.getGroupName());
        if (Objects.isNull(group)) {
            LOG.debug("not found lane_group, redirect to base, lane_rule: {}, lane_group: {}, callee: {}", rule.getName(), rule.getGroupName(), instances.getServiceKey());
            // 泳道组不存在，直接认为不需要过滤实例, 默认转发至基线实例
            return redirectToBase(laneGroupList, instances);
        }
        // 判断目标服务是否属于泳道内服务
        boolean inLane = false;
        ServiceKey callee = instances.getServiceKey();
        for (RoutingProto.DestinationGroup destination : group.getDestinationsList()) {
            if (RuleUtils.matchService(callee, destination.getNamespace(), destination.getService())) {
                inLane = true;
                break;
            }
        }

        // 不在泳道内的服务，不需要进行实例过滤, 默认转发至基线实例
        if (!inLane) {
            LOG.debug("current traffic not in lane, redirect to base, lane_rule: {}, lane_group: {}, callee: {}", rule.getName(), rule.getGroupName(), instances.getServiceKey());
            return redirectToBase(laneGroupList, instances);
        }

        return instances.getInstances().stream().filter(instance -> {
            Map<String, String> metadata = instance.getMetadata();
            if (CollectionUtils.isEmpty(metadata)) {
                return false;
            }
            String labelKey = StringUtils.isNotBlank(rule.getLabelKey()) ? rule.getLabelKey() : INTERNAL_INSTANCE_LANE_KEY;
            String val = metadata.get(labelKey);
            String defaultLabelValue = rule.getDefaultLabelValue();
            Set<String> defaultLabelValues = new HashSet<>(Arrays.asList(defaultLabelValue.split(",")));
            return defaultLabelValues.contains(val);
        }).collect(Collectors.toList());
    }

    private List<Instance> redirectToBase(List<LaneProto.LaneGroup> laneGroupList, ServiceInstances instances) {
        return instances.getInstances().stream().filter(instance -> {
            Map<String, String> metadata = instance.getMetadata();
            if (CollectionUtils.isEmpty(metadata)) {
                return true;
            }

            boolean inBase = true;
            for (LaneProto.LaneGroup laneGroup : laneGroupList) {
                Map<String, Set<String>> laneKeyValueMap = new HashMap<>();
                for (LaneProto.LaneRule laneRule : laneGroup.getRulesList()) {
                    String labelKey = StringUtils.isNotBlank(laneRule.getLabelKey()) ? laneRule.getLabelKey() : INTERNAL_INSTANCE_LANE_KEY;
                    if (!laneKeyValueMap.containsKey(labelKey)) {
                        laneKeyValueMap.put(labelKey, new HashSet<>());
                    }
                    String defaultLabelValue = laneRule.getDefaultLabelValue();
                    String[] split = defaultLabelValue.split(",");
                    laneKeyValueMap.get(labelKey).addAll(Arrays.asList(split));
                }
                if (CollectionUtils.isNotEmpty(laneKeyValueMap)) {
                    for (Map.Entry<String, String> entry : metadata.entrySet()) {
                        if (laneKeyValueMap.containsKey(entry.getKey()) && laneKeyValueMap.get(entry.getKey()).contains(entry.getValue())) {
                            inBase = false;
                            break;
                        }
                    }
                }
                if (!inBase) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    private boolean tryStainCurrentTraffic(MetadataContext manager, ServiceKey caller, LaneRuleContainer container, LaneProto.LaneRule rule) {
        if (Objects.isNull(caller)) {
            LOG.debug("caller is null, stain current traffic ignore, lane_rule: {}, lane_group: {}", rule.getName(), rule.getGroupName());
            return false;
        }

        LaneProto.LaneGroup group = container.groups.get(rule.getGroupName());
        if (Objects.isNull(group)) {
            // 泳道规则存在，但是对应的泳道组却不存在，这种情况需要直接抛出异常
            LOG.error("lane_group where lane_rule located not found, lane_rule: {}, lane_group: {}", rule.getName(), rule.getGroupName());
            throw new PolarisException(ErrorCode.INVALID_STATE, "lane_group where lane_rule located not found");
        }

        boolean needStain = isTrafficEntry(group, manager, caller);
        if (needStain) {
            MessageMetadataContainer metadataContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
            metadataContainer.setHeader(TRAFFIC_STAIN_LABEL, buildStainLabel(rule), TransitiveType.PASS_THROUGH);
        }
        LOG.debug("stain current traffic: {}, lane_rule: {}, lane_group: {}, caller: {}", needStain, rule.getName(), rule.getGroupName(), caller);
        return needStain;
    }

    private LaneRuleContainer fetchLaneRules(MetadataContext manager, ServiceKey caller, ServiceKey callee) {
        // 获取泳道规则
        List<LaneProto.LaneGroup> result = new ArrayList<>();
        if (Objects.nonNull(caller)) {
            result.addAll(ruleGetter.apply(caller));
        }
        if (Objects.nonNull(callee)) {
            result.addAll(ruleGetter.apply(callee));
        }
        return new LaneRuleContainer(manager, caller, result);
    }

    private static boolean isTrafficEntry(LaneProto.LaneGroup group, MetadataContext manager, ServiceKey caller) {
        boolean result = false;
        for (LaneProto.TrafficEntry entry : group.getEntriesList()) {
            try {
                switch (entry.getType()) {
                    case GATEWAY_SELECTOR:
                        LaneProto.ServiceGatewaySelector gatewaySelector = entry.getSelector().unpack(LaneProto.ServiceGatewaySelector.class);
                        if (RuleUtils.matchService(caller, gatewaySelector.getNamespace(), gatewaySelector.getService())
                                && RuleUtils.matchMetadata(gatewaySelector.getLabelsMap(), null, manager.getMetadataContainerGroup(false))) {
                            result = true;
                        }
                        break;
                    case SERVICE_SELECTOR:
                        LaneProto.ServiceSelector serviceSelector = entry.getSelector().unpack(LaneProto.ServiceSelector.class);
                        if (RuleUtils.matchService(caller, serviceSelector.getNamespace(), serviceSelector.getService())
                                && RuleUtils.matchMetadata(serviceSelector.getLabelsMap(), null, manager.getMetadataContainerGroup(false))) {
                            result = true;
                        }
                        break;
                }
            } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
                LOG.warn("lane_group: {} unpack traffic entry selector fail", group.getName(), invalidProtocolBufferException);
            }
        }
        return result;
    }

    private static class LaneRuleContainer {
        private final Map<String, LaneProto.LaneGroup> groups = new HashMap<>();

        private final List<LaneProto.LaneRule> rules = new LinkedList<>();

        private final Map<String, LaneProto.LaneRule> ruleMapping = new HashMap<>();

        LaneRuleContainer(MetadataContext manager, ServiceKey caller, List<LaneProto.LaneGroup> list) {
            list.forEach(laneGroup -> {
                if (groups.containsKey(laneGroup.getName())) {
                    LOG.warn("lane group: {} duplicate, ignore", laneGroup.getName());
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

            rules.sort((o1, o2) -> {
                // 主调泳道入口规则优先
                boolean b1 = isTrafficEntry(groups.get(o1.getGroupName()), manager, caller);
                boolean b2 = isTrafficEntry(groups.get(o2.getGroupName()), manager, caller);
                int entryResult = CompareUtils.compareBoolean(b1, b2);
                if (entryResult != 0) {
                    return entryResult;
                }

                // 比较优先级，数字越小，规则优先级越大
                return o1.getPriority() - o2.getPriority();
            });
        }

        public List<LaneProto.LaneGroup> getGroupListByCalleeNamespaceAndService(ServiceKey callee) {
            List<LaneProto.LaneGroup> groupList = new ArrayList<>();
            for (LaneProto.LaneGroup group : groups.values()) {
                for (RoutingProto.DestinationGroup destinationGroup : group.getDestinationsList()) {
                    if (RuleUtils.matchService(callee, destinationGroup.getNamespace(), destinationGroup.getService())) {
                        groupList.add(group);
                    }
                }
            }
            return groupList;
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
                            boolean a = StringUtils.isNotBlank(trafficValue) &&
                                    RuleUtils.matchStringValue(sourceMatch.getValue().getType(), trafficValue,
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

        MessageMetadataContainer calleeMessageContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
        MetadataContainer calleeCustomContainer = manager.getMetadataContainer(MetadataType.CUSTOM, false);
        MessageMetadataContainer callerMessageContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);

        String trafficValue = "";
        switch (sourceMatch.getType()) {
            case HEADER:
                String headerKey = RouteArgument.ArgumentType.HEADER.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(headerKey)) {
                    return trafficLabels.get(headerKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getHeader(sourceMatch.getKey())).orElse(callerMessageContainer.getHeader(sourceMatch.getKey()));
                break;
            case CUSTOM:
                String customKey = RouteArgument.ArgumentType.CUSTOM.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(customKey)) {
                    return trafficLabels.get(customKey);
                }
                trafficValue = Optional.ofNullable(calleeCustomContainer.getRawMetadataStringValue(sourceMatch.getKey())).orElse("");
                break;
            case METHOD:
                String methodKey = RouteArgument.ArgumentType.METHOD.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(methodKey)) {
                    return trafficLabels.get(methodKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getMethod()).orElse(callerMessageContainer.getMethod());
                break;
            case CALLER_IP:
                String callerIpKey = RouteArgument.ArgumentType.CALLER_IP.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(callerIpKey)) {
                    return trafficLabels.get(callerIpKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getCallerIP()).orElse(callerMessageContainer.getCallerIP());
                break;
            case COOKIE:
                String cookieKey = RouteArgument.ArgumentType.COOKIE.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(cookieKey)) {
                    return trafficLabels.get(cookieKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getCookie(sourceMatch.getKey())).orElse(callerMessageContainer.getCookie(sourceMatch.getKey()));
                break;
            case QUERY:
                String queryKey = RouteArgument.ArgumentType.QUERY.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(queryKey)) {
                    return trafficLabels.get(queryKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getQuery(sourceMatch.getKey())).orElse(callerMessageContainer.getQuery(sourceMatch.getKey()));
                break;
            case PATH:
                String pathKey = RouteArgument.ArgumentType.PATH.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(pathKey)) {
                    return trafficLabels.get(pathKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getPath()).orElse(callerMessageContainer.getPath());
                break;
        }
        return trafficValue;
    }

    private static String buildStainLabel(LaneProto.LaneRule rule) {
        return rule.getGroupName() + "/" + rule.getName();
    }
}
