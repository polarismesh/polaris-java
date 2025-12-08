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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
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
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

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

        LaneRuleContainer container = LaneUtils.fetchLaneRules(manager, caller, callee, extensions);

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
        boolean stainOK = LaneUtils.tryStainCurrentTraffic(manager, caller, container, laneRule);
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
        LaneProto.LaneGroup group = container.getGroups().get(rule.getGroupName());
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
                LOG.debug("lane_group: {}, rulesList:{}, laneKeyValueMap: {}", laneGroup.getName(), laneGroup.getRulesList(), laneKeyValueMap);
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
}
