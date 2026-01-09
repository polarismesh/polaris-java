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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.google.protobuf.InvalidProtocolBufferException;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.pojo.DefaultServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
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
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

import static com.tencent.polaris.plugins.router.lane.LaneRouter.TRAFFIC_STAIN_LABEL;

public class LaneUtils {

    private static final Logger LOG = LoggerFactory.getLogger(LaneUtils.class);

    private static final String GATEWAY_SELECTOR = "polarismesh.cn/gateway/spring-cloud-gateway";

    private static final String SERVICE_SELECTOR = "polarismesh.cn/service";


    public static List<LaneProto.LaneGroup> getLaneGroups(ServiceKey serviceKey, Extensions extensions) {
        if (Objects.isNull(serviceKey)) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(serviceKey.getService()) || StringUtils.isBlank(serviceKey.getNamespace())) {
            return Collections.emptyList();
        }

        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(new RequestBaseEntity(), extensions.getConfiguration(), engineFlowControlParam);
        Set<ServiceEventKey> routerKeys = new HashSet<>();
        ServiceEventKey dstSvcEventKey = ServiceEventKey.builder().serviceKey(serviceKey)
                .eventType(ServiceEventKey.EventType.LANE_RULE).build();
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
    }

    public static boolean tryStainCurrentTraffic(MetadataContext manager, ServiceKey caller,
            LaneRuleContainer container, LaneProto.LaneRule rule) {
        if (Objects.isNull(caller)) {
            LOG.debug("caller is null, stain current traffic ignore, lane_rule: {}, lane_group: {}", rule.getName(),
                    rule.getGroupName());
            return false;
        }

        LaneProto.LaneGroup group = container.getGroups().get(rule.getGroupName());
        if (Objects.isNull(group)) {
            // 泳道规则存在，但是对应的泳道组却不存在，这种情况需要直接抛出异常
            LOG.error("lane_group where lane_rule located not found, lane_rule: {}, lane_group: {}", rule.getName(),
                    rule.getGroupName());
            throw new PolarisException(ErrorCode.INVALID_STATE, "lane_group where lane_rule located not found");
        }

        boolean isTrafficEntry = isTrafficEntry(group, manager, caller);
        boolean isWarmupStain = isWarmupStain(rule);
        boolean isStained = false;
        if (isTrafficEntry) {
            // 固定染色，直接设置染色标签
            if (!isWarmupStain && tryStainByPercentage(rule)) {
                MessageMetadataContainer metadataContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
                metadataContainer.setHeader(LaneRouter.TRAFFIC_STAIN_LABEL, buildStainLabel(rule),
                        TransitiveType.PASS_THROUGH);
                isStained = true;
            }
            if (isWarmupStain && tryStainByWarmup(rule)) {
                // 流量预热染色，设置染色标签，并且设置染色时间
                MessageMetadataContainer metadataContainer = manager.getMetadataContainer(MetadataType.MESSAGE,
                        false);
                metadataContainer.setHeader(LaneRouter.TRAFFIC_STAIN_LABEL, buildStainLabel(rule),
                        TransitiveType.PASS_THROUGH);
                isStained = true;
            }
        }
        LOG.debug("stain current traffic: {}, lane_rule: {}, lane_group: {}, caller: {}, is warmup stain: {}",
                isTrafficEntry, rule.getName(),
                rule.getGroupName(), caller, isWarmupStain);
        return isStained;
    }

    public static boolean isWarmupStain(LaneProto.LaneRule rule) {

        if (!rule.hasTrafficGray()) {
            // 老版本服务端没有该字段
            return false;
        }
         LaneProto.TrafficGray trafficGray = rule.getTrafficGray();
         return trafficGray.getMode()==LaneProto.TrafficGray.Mode.WARMUP;
    }

    // 按比例染色
    public static boolean tryStainByPercentage(LaneProto.LaneRule rule) {
        if (!rule.hasTrafficGray()) {
            // 没有灰度配置，默认100%染色
            return true;
        }
        LaneProto.TrafficGray trafficGray = rule.getTrafficGray();
        if (!trafficGray.hasPercentage()) {
            // 没有百分比配置，默认100%染色
            return true;
        }
        int percentage = trafficGray.getPercentage().getPercent();
        if (percentage == 100) {
            return true;
        }
        return ThreadLocalRandom.current().nextInt(100) < percentage;
    }

    // 预热泳道，判断是否需要染色
    public static boolean tryStainByWarmup(LaneProto.LaneRule rule) {
        // 将创建时间转换为毫秒时间戳（只计算一次，可考虑缓存）
        String ruleEnabledTime = rule.getEtime(); // "2025-09-17 16:00:00"

        long enabledTimeMillis = LocalDateTime.parse(ruleEnabledTime,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli();
        long uptimeMillis = System.currentTimeMillis() - enabledTimeMillis;
        // TODO: 删除注释
//        String[] description = rule.getDescription().split(",");
//        Map<String, String> warmupConfig = new HashMap<>();
//        for (String s : description) {
//            String[] kv = s.split(":");
//            if (kv.length != 2) {
//                continue;
//            }
//            warmupConfig.put(kv[0], kv[1]);
//        }

        int warmupIntervalSeconds = rule.getTrafficGray().getWarmup().getIntervalSecond();
        int curvature = rule.getTrafficGray().getWarmup().getCurvature();
        long warmupIntervalMillis = warmupIntervalSeconds * 1000L;

        // 过了预热时间，预热完成，染色
        if (uptimeMillis >= warmupIntervalMillis) {
            return true;
        }
        // 未到创建时间，不染色
        if (uptimeMillis <= 0) {
            return false;
        }

        // 计算染色概率：probability = (uptime / warmupInterval) ^ curvature
        double progress = (double) uptimeMillis / warmupIntervalMillis;
        double probability = Math.pow(progress, curvature);

        // 使用 ThreadLocalRandom 替代 new Random()，避免每次创建对象，性能更好
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    public static LaneRuleContainer fetchLaneRules(MetadataContext manager, ServiceKey caller, ServiceKey callee,
            Extensions extensions) {
        // 获取泳道规则
        List<LaneProto.LaneGroup> result = new ArrayList<>();
        if (Objects.nonNull(caller)) {
            result.addAll(getLaneGroups(caller, extensions));
        }
        if (Objects.nonNull(callee)) {
            result.addAll(getLaneGroups(callee, extensions));
        }
        return new LaneRuleContainer(manager, caller, result);
    }

    public static boolean isTrafficEntry(LaneProto.LaneGroup group, MetadataContext manager, ServiceKey caller) {
        boolean result = false;
        for (LaneProto.TrafficEntry entry : group.getEntriesList()) {
            try {
                switch (entry.getType()) {
                    case GATEWAY_SELECTOR:
                        LaneProto.ServiceGatewaySelector gatewaySelector = entry.getSelector()
                                .unpack(LaneProto.ServiceGatewaySelector.class);
                        if (RuleUtils.matchService(caller, gatewaySelector.getNamespace(), gatewaySelector.getService())
                                && RuleUtils.matchMetadata(gatewaySelector.getLabelsMap(), null,
                                manager.getMetadataContainerGroup(false))) {
                            result = true;
                        }
                        break;
                    case SERVICE_SELECTOR:
                        LaneProto.ServiceSelector serviceSelector = entry.getSelector()
                                .unpack(LaneProto.ServiceSelector.class);
                        if (RuleUtils.matchService(caller, serviceSelector.getNamespace(), serviceSelector.getService())
                                && RuleUtils.matchMetadata(serviceSelector.getLabelsMap(), null,
                                manager.getMetadataContainerGroup(false))) {
                            result = true;
                        }
                        break;
                }
            } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
                LOG.warn("lane_group: {} unpack traffic entry selector fail", group.getName(),
                        invalidProtocolBufferException);
            }
        }
        return result;
    }


    public static String findTrafficValue(RouteInfo routeInfo, RoutingProto.SourceMatch sourceMatch,
            MetadataContext manager) {
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
                trafficValue = Optional.ofNullable(calleeMessageContainer.getHeader(sourceMatch.getKey()))
                        .orElse(callerMessageContainer.getHeader(sourceMatch.getKey()));
                break;
            case CUSTOM:
                String customKey = RouteArgument.ArgumentType.CUSTOM.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(customKey)) {
                    return trafficLabels.get(customKey);
                }
                trafficValue = Optional.ofNullable(
                        calleeCustomContainer.getRawMetadataStringValue(sourceMatch.getKey())).orElse("");
                break;
            case METHOD:
                String methodKey = RouteArgument.ArgumentType.METHOD.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(methodKey)) {
                    return trafficLabels.get(methodKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getMethod())
                        .orElse(callerMessageContainer.getMethod());
                break;
            case CALLER_IP:
                String callerIpKey = RouteArgument.ArgumentType.CALLER_IP.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(callerIpKey)) {
                    return trafficLabels.get(callerIpKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getCallerIP())
                        .orElse(callerMessageContainer.getCallerIP());
                break;
            case COOKIE:
                String cookieKey = RouteArgument.ArgumentType.COOKIE.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(cookieKey)) {
                    return trafficLabels.get(cookieKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getCookie(sourceMatch.getKey()))
                        .orElse(callerMessageContainer.getCookie(sourceMatch.getKey()));
                break;
            case QUERY:
                String queryKey = RouteArgument.ArgumentType.QUERY.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(queryKey)) {
                    return trafficLabels.get(queryKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getQuery(sourceMatch.getKey()))
                        .orElse(callerMessageContainer.getQuery(sourceMatch.getKey()));
                break;
            case PATH:
                String pathKey = RouteArgument.ArgumentType.PATH.key(sourceMatch.getKey());
                if (trafficLabels.containsKey(pathKey)) {
                    return trafficLabels.get(pathKey);
                }
                trafficValue = Optional.ofNullable(calleeMessageContainer.getPath())
                        .orElse(callerMessageContainer.getPath());
                break;
        }
        return trafficValue;
    }

    public static String findTrafficValue(RoutingProto.SourceMatch sourceMatch, MetadataContext manager) {

        MessageMetadataContainer calleeMessageContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
        MetadataContainer calleeCustomContainer = manager.getMetadataContainer(MetadataType.CUSTOM, false);
        MessageMetadataContainer callerMessageContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);

        String trafficValue = "";
        switch (sourceMatch.getType()) {
            case HEADER:
                trafficValue = Optional.ofNullable(calleeMessageContainer.getHeader(sourceMatch.getKey()))
                        .orElse(callerMessageContainer.getHeader(sourceMatch.getKey()));
                break;
            case CUSTOM:
                trafficValue = Optional.ofNullable(
                        calleeCustomContainer.getRawMetadataStringValue(sourceMatch.getKey())).orElse("");
                break;
            case METHOD:
                trafficValue = Optional.ofNullable(calleeMessageContainer.getMethod())
                        .orElse(callerMessageContainer.getMethod());
                break;
            case CALLER_IP:
                trafficValue = Optional.ofNullable(calleeMessageContainer.getCallerIP())
                        .orElse(callerMessageContainer.getCallerIP());
                break;
            case COOKIE:
                trafficValue = Optional.ofNullable(calleeMessageContainer.getCookie(sourceMatch.getKey()))
                        .orElse(callerMessageContainer.getCookie(sourceMatch.getKey()));
                break;
            case QUERY:
                trafficValue = Optional.ofNullable(calleeMessageContainer.getQuery(sourceMatch.getKey()))
                        .orElse(callerMessageContainer.getQuery(sourceMatch.getKey()));
                break;
            case PATH:
                trafficValue = Optional.ofNullable(calleeMessageContainer.getPath())
                        .orElse(callerMessageContainer.getPath());
                break;
        }
        return trafficValue;
    }

    public static String buildStainLabel(LaneProto.LaneRule rule) {
        return rule.getGroupName() + "/" + rule.getName();
    }

    /**
     * 根据 caller 信息，匹配泳道，用于 mq 泳道等场景.
     */
    public static String fetchLaneByCaller(Extensions extensions, String namespace, String serviceName) {
        MetadataContext manager = MetadataContextHolder.getOrCreate();
        MessageMetadataContainer callerMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);
        ServiceKey caller = new ServiceKey(namespace, serviceName);

        LaneRuleContainer container = LaneUtils.fetchLaneRules(manager, caller, null, extensions);

        // 判断当前流量是否已存在染色，如果存在，则直接返回
        String stainLabel = callerMsgContainer.getHeader(TRAFFIC_STAIN_LABEL);
        if (StringUtils.isNotBlank(stainLabel)) {
            MessageMetadataContainer metadataContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
            metadataContainer.setHeader(LaneRouter.TRAFFIC_STAIN_LABEL, stainLabel, TransitiveType.PASS_THROUGH);
            return stainLabel;
        }
        // 当前流量无染色，根据泳道规则进行匹配判断
        Optional<LaneProto.LaneRule> targetRule = container.matchRule(manager);

        // 泳道规则不存在，直接返回
        if (!targetRule.isPresent()) {
            return null;
        }

        LaneProto.LaneRule laneRule = targetRule.get();
        // 尝试进行流量染色动作，该操作仅在当前 Caller 服务为泳道入口时操作
        LaneUtils.tryStainCurrentTraffic(manager, caller, container, laneRule);

        MessageMetadataContainer metadataContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
        return metadataContainer.getHeader(LaneRouter.TRAFFIC_STAIN_LABEL);
    }

    /**
     * Sets the lane ID for the caller.
     * <p>
     * This method is used in two primary scenarios:
     * <ol>
     *   <li><b>Standard Polaris Producer:</b> When the message queue producer is a standard Polaris implementation,
     *       the lane ID is automatically set within the consumer's {@code KafkaLaneAspect}.</li>
     *   <li><b>Custom MQ Producer:</b> When the producer is a custom implementation,
     *       the lane ID must be explicitly set by the developer in the consumer code,
     *       prior to the execution of the {@code KafkaLaneAspect}.</li>
     * </ol>
     *
     * @param laneId the lane identifier to be associated with the caller
     */
    public static void setCallerLaneId(String laneId) {
        MetadataContext manager = MetadataContextHolder.getOrCreate();
        MessageMetadataContainer callerMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);
        callerMsgContainer.setHeader(LaneRouter.TRAFFIC_STAIN_LABEL, laneId, TransitiveType.PASS_THROUGH);
    }

    /**
     * get upstream lane id.
     *
     * @return lane id of upstream service
     */
    public static String getCallerLaneId() {
        MetadataContext manager = MetadataContextHolder.getOrCreate();
        MessageMetadataContainer callerMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);
        return callerMsgContainer.getHeader(LaneRouter.TRAFFIC_STAIN_LABEL);
    }

    public static void removeCallerLaneId() {
        MetadataContext manager = MetadataContextHolder.getOrCreate();
        MessageMetadataContainer callerMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, true);
        callerMsgContainer.setHeader(LaneRouter.TRAFFIC_STAIN_LABEL, "", TransitiveType.PASS_THROUGH);
    }

    /**
     * get current lane id.
     *
     * @return lane id of current service
     */
    public static String getCalleeLaneId() {
        MetadataContext manager = MetadataContextHolder.getOrCreate();
        MessageMetadataContainer calleeMsgContainer = manager.getMetadataContainer(MetadataType.MESSAGE, false);
        return calleeMsgContainer.getHeader(LaneRouter.TRAFFIC_STAIN_LABEL);
    }
}
