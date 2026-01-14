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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CompareUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.specification.api.v1.traffic.manage.LaneProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

public class LaneRuleContainer {

    private static final Logger LOG = LoggerFactory.getLogger(LaneRuleContainer.class);

    private final Map<String, LaneProto.LaneGroup> groups = new HashMap<>();

    private final List<LaneProto.LaneRule> rules = new LinkedList<>();

    private final Map<String, LaneProto.LaneRule> ruleMapping = new HashMap<>();

    public LaneRuleContainer(MetadataContext manager, ServiceKey caller, List<LaneProto.LaneGroup> list) {
        for (LaneProto.LaneGroup laneGroup : list) {
            if (groups.containsKey(laneGroup.getName())) {
                LOG.debug("lane group: {} duplicate, ignore", laneGroup.getName());
                continue;
            }
            groups.put(laneGroup.getName(), laneGroup);
            for (LaneProto.LaneRule laneRule : laneGroup.getRulesList()) {
                if (!laneRule.getEnable()) {
                    continue;
                }
                String name = LaneUtils.buildStainLabel(laneRule);
                ruleMapping.put(name, laneRule);
                rules.add(laneRule);
            }
        }
        rules.sort((o1, o2) -> {
            // 主调泳道入口规则优先
            boolean b1 = LaneUtils.isTrafficEntry(groups.get(o1.getGroupName()), manager, caller);
            boolean b2 = LaneUtils.isTrafficEntry(groups.get(o2.getGroupName()), manager, caller);
            int entryResult = CompareUtils.compareBoolean(b1, b2);
            if (entryResult != 0) {
                return entryResult;
            }

            // 比较优先级，数字越小，规则优先级越大
            int priorityResult = o1.getPriority() - o2.getPriority();
            if (priorityResult != 0) {
                return priorityResult;
            }
            // 比较创建时间，越早创建的规则优先级越高
            return o1.getCtime().compareTo(o2.getCtime());
        });
    }

    public Map<String, LaneProto.LaneGroup> getGroups() {
        return groups;
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
                String trafficValue = LaneUtils.findTrafficValue(routeInfo, sourceMatch, manager);
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
            if (booleans.isEmpty()) {
                isMatched = true;
            }
            if (!isMatched) {
                continue;
            }
            targetRule = rule;
            break;
        }

        return Optional.ofNullable(targetRule);
    }

    public Optional<LaneProto.LaneRule> matchRule(MetadataContext manager) {
        // 当前流量无染色，根据泳道规则进行匹配判断
        LaneProto.LaneRule targetRule = null;
        for (LaneProto.LaneRule rule : rules) {
            if (!rule.getEnable()) {
                continue;
            }

            LaneProto.TrafficMatchRule matchRule = rule.getTrafficMatchRule();

            List<Boolean> booleans = new LinkedList<>();
            matchRule.getArgumentsList().forEach(sourceMatch -> {
                String trafficValue = LaneUtils.findTrafficValue(sourceMatch, manager);
                switch (sourceMatch.getValue().getValueType()) {
                    case TEXT:
                        // 直接匹配
                        boolean a = StringUtils.isNotBlank(trafficValue) &&
                                RuleUtils.matchStringValue(sourceMatch.getValue().getType(), trafficValue,
                                        sourceMatch.getValue().getValue().getValue());
                        booleans.add(a);
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