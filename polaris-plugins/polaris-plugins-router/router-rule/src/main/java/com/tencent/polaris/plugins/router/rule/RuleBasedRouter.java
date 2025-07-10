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

package com.tencent.polaris.plugins.router.rule;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.Service;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.rpc.RuleBasedRouterFailoverType;
import com.tencent.polaris.api.utils.*;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.manager.MetadataContainerGroup;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import com.tencent.polaris.plugins.router.common.RoutingUtils;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto.Destination;
import org.slf4j.Logger;

import java.util.*;

import static com.tencent.polaris.api.plugin.cache.CacheConstants.API_ID;
import static com.tencent.polaris.api.plugin.route.RouterConstants.ROUTER_FAULT_TOLERANCE_ENABLE;

/**
 * 基于规则的服务路由能力
 *
 * @author andrewshan
 * @date 2019/8/28
 */
public class RuleBasedRouter extends AbstractServiceRouter implements PluginConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RuleBasedRouter.class);
    public static final String ROUTER_TYPE_RULE_BASED = "ruleRouter";
    public static final String ROUTER_ENABLED = "enabled";

    private Map<String, String> globalVariablesConfig;

    private RuleBasedRouterConfig routerConfig;

    private FlowCache flowCache;

    /**
     * 根据路由规则进行服务实例过滤, 并返回过滤后的实例列表
     *
     * @param routeInfo
     * @param ruleMatchType
     * @return 路由规则列表
     * @throws PolarisException
     */
    private List<RoutingProto.Route> getRoutesFromRule(RouteInfo routeInfo, RuleMatchType ruleMatchType)
            throws PolarisException {
        // 跟据服务类型获取对应路由规则
        // 被调inbound
        if (ruleMatchType == RuleMatchType.destRouteRuleMatch) {
            if (null == routeInfo.getDestRouteRule() || null == routeInfo.getDestRouteRule().getRule()) {
                return null;
            }
            Object routing = routeInfo.getDestRouteRule().getRule();
            if (!(routing instanceof RoutingProto.Routing)) {
                throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "getRuleFilteredInstances param invalid, "
                        + "inbound routing must be instance of RoutingProto.Routing");
            }

            return ((RoutingProto.Routing) routing).getInboundsList();
        }

        // 主调outbound
        if (null == routeInfo.getSourceRouteRule() || null == routeInfo.getSourceRouteRule().getRule()) {
            return null;
        }
        Object routing = routeInfo.getSourceRouteRule().getRule();
        if (!(routing instanceof RoutingProto.Routing)) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "getRuleFilteredInstances param invalid, "
                    + "outbound routing must be instance of RoutingProto.Routing");
        }
        return ((RoutingProto.Routing) routing).getOutboundsList();
    }

    // 匹配source规则
    private boolean matchSource(List<RoutingProto.Source> sources, Service sourceService,
                                Map<String, String> trafficLabels,
                                MetadataContainerGroup metadataContainerGroup,
                                RuleMatchType ruleMatchType, Map<String, String> multiEnvRouterParamMap) {
        if (CollectionUtils.isEmpty(sources)) {
            return true;
        }

        // source匹配成功标志
        boolean matched = true;
        for (RoutingProto.Source source : sources) {
            // 对于inbound规则, 需要匹配source服务
            if (ruleMatchType == RuleMatchType.destRouteRuleMatch) {
                matched = RoutingUtils.matchSourceService(source, sourceService);
                if (!matched) {
                    continue;
                }
            }

            matched = RoutingUtils.matchSourceMetadata(source, sourceService, trafficLabels, metadataContainerGroup,
                    multiEnvRouterParamMap, globalVariablesConfig,
                    key -> flowCache.loadPluginCacheObject(API_ID, key, path -> TrieUtil.buildSimpleApiTrieNode((String) path)));
            if (matched) {
                break;
            }
        }

        return matched;
    }

    private List<RoutingProto.Destination> filterAvailableDestinations(List<RoutingProto.Destination> destinations) {
        List<RoutingProto.Destination> availableSubsets = new ArrayList<>();
        for (RoutingProto.Destination dest : destinations) {
            if (dest == null) {
                continue;
            }
            if (dest.getIsolate().getValue()) {
                continue;
            }
            availableSubsets.add(dest);
        }
        return availableSubsets;
    }

    private List<Instance> getRuleFilteredInstances(RouteInfo routeInfo, ServiceInstances instances,
                                                    RuleMatchType ruleMatchType, MatchStatus matchStatus) throws PolarisException {
        // 获取路由规则
        List<RoutingProto.Route> routes = getRoutesFromRule(routeInfo, ruleMatchType);
        if (CollectionUtils.isEmpty(routes)) {
            return Collections.emptyList();
        }
        Map<String, String> multiEnvRouterParamMap = new HashMap<>();
        for (RoutingProto.Route route : routes) {
            if (route == null) {
                continue;
            } else {
                matchStatus.fallback = Boolean.parseBoolean(route.getExtendInfoMap().get(ROUTER_FAULT_TOLERANCE_ENABLE));
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("getRuleFilteredInstances, route:{}", route);
            }

            Map<String, String> trafficLabels = routeInfo.getRouterMetadata(ROUTER_TYPE_RULE_BASED);
            MetadataContainerGroup metadataContainerGroup = routeInfo.getMetadataContainerGroup();
            // 匹配source规则
            boolean sourceMatched = matchSource(route.getSourcesList(), routeInfo.getSourceService(), trafficLabels,
                    metadataContainerGroup, ruleMatchType, multiEnvRouterParamMap);
            if (!sourceMatched) {
                continue;
            }
            matchStatus.matched = true;
            // 如果source匹配成功, 继续匹配destination规则
            // 然后将结果写进map(key: 权重, value: 带权重的实例分组)
            Map<Integer, PrioritySubsets> subsetsMap = new HashMap<>();
            int smallestPriority = -1;
            List<Destination> destinations = filterAvailableDestinations(route.getDestinationsList());
            for (RoutingProto.Destination dest : destinations) {
                // 对于outbound规则, 需要匹配DestService服务
                if (ruleMatchType == RuleMatchType.sourceRouteRuleMatch) {
                    if (!RuleUtils.MATCH_ALL.equals(dest.getNamespace().getValue()) && !dest.getNamespace().getValue()
                            .equals(routeInfo.getDestService().getNamespace())) {
                        continue;
                    }

                    if (!RuleUtils.MATCH_ALL.equals(dest.getService().getValue()) && !dest.getService().getValue()
                            .equals(routeInfo.getDestService().getService())) {
                        continue;
                    }
                }

                if (dest.getWeight().getValue() == 0) {
                    continue;
                }

                boolean populated = populateSubsetsFromDest(instances, dest, subsetsMap, multiEnvRouterParamMap);
                if (populated) {
                    int priority = dest.getPriority().getValue();
                    if (smallestPriority < 0 || smallestPriority > priority) {
                        smallestPriority = priority;
                    }
                }
            }

            // 如果未匹配到分组, 继续匹配
            if (MapUtils.isEmpty(subsetsMap)) {
                continue;
            }
            // 匹配到分组, 返回
            return selectInstances(routeInfo, subsetsMap.get(smallestPriority));
        }

        // 全部匹配完成, 未匹配到任何分组, 返回空
        return Collections.emptyList();
    }

    /**
     * populateSubsetsFromDest 根据destination中的规则填充分组列表
     *
     * @param instances  实例信息
     * @param dest       目标规则
     * @param subsetsMap 实例分组
     * @return 是否成功加入subset列表
     */
    private boolean populateSubsetsFromDest(
            ServiceInstances instances, RoutingProto.Destination dest, Map<Integer, PrioritySubsets> subsetsMap,
            Map<String, String> multiEnvRouterParamMap) {
        // 获取subset
        List<Instance> oriInstances = instances.getInstances();
        List<Instance> filteredInstances = new ArrayList<>();
        for (Instance ins : oriInstances) {
            if (!RuleUtils.matchMetadata(dest.getMetadataMap(), ins.getMetadata(), false, multiEnvRouterParamMap,
                    globalVariablesConfig)) {
                continue;
            }
            filteredInstances.add(ins);
        }

        // subset中无实例
        if (CollectionUtils.isEmpty(filteredInstances)) {
            return false;
        }

        // 根据优先级填充subset列表
        int priority = dest.getPriority().getValue();
        int weight = dest.getWeight().getValue();
        PrioritySubsets weightedSubsets = subsetsMap.get(priority);
        if (weightedSubsets == null) {
            PrioritySubsets prioritySubsets = new PrioritySubsets();

            WeightedSubset weightedSubset = new WeightedSubset(dest);
            weightedSubset.setInstances(filteredInstances);
            weightedSubset.setWeight(weight);

            prioritySubsets.setSubsets(new ArrayList<>(Collections.singletonList(weightedSubset)));
            prioritySubsets.setTotalWeight(weight);

            subsetsMap.put(priority, prioritySubsets);

        } else {
            WeightedSubset weightedSubset = new WeightedSubset(dest);
            weightedSubset.setInstances(filteredInstances);
            weightedSubset.setWeight(weight);

            weightedSubsets.getSubsets().add(weightedSubset);
            weightedSubsets.setTotalWeight(weightedSubsets.getTotalWeight() + weight);
        }
        return true;
    }

    //selectInstances 从subset中选取实例
    private List<Instance> selectInstances(RouteInfo routeInfo, PrioritySubsets weightedSubsets) {
        if (weightedSubsets.getSubsets().size() == 1) {
            return weightedSubsets.getSubsets().get(0).getInstances();
        }
        Random random = new Random();

        // 根据区间算法选择subset
        long weight = random.nextInt(weightedSubsets.getTotalWeight());
        //匹配的区间下标
        int matchedRange = -1;
        for (WeightedSubset weightedSubset : weightedSubsets.getSubsets()) {
            weight -= weightedSubset.getWeight();
            if (weight < 0) {
                Destination destination = weightedSubset.getDestination();
                routeInfo.setSubsetName(destination.getName().getValue());
                routeInfo.setSubsetMetadata(destination.getMetadataMap());
                return weightedSubset.getInstances();
            }
        }
        return Collections.emptyList();
    }


    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances instances) {
        // 根据匹配过程修改状态, 默认无路由策略状态
        RuleStatus ruleStatus = RuleStatus.noRule;
        // 优先匹配inbound规则, 成功则不需要继续匹配outbound规则
        List<Instance> destFilteredInstances = null;
        List<Instance> sourceFilteredInstances = null;

        MatchStatus matchStatus = new MatchStatus();
        if (routeInfo.getDestRouteRule() != null) {
            destFilteredInstances = getRuleFilteredInstances(routeInfo, instances,
                    RuleMatchType.destRouteRuleMatch, matchStatus);
            if (!destFilteredInstances.isEmpty()) {
                ruleStatus = RuleStatus.destRuleSucc;
            }
            if (destFilteredInstances.isEmpty() && matchStatus.matched) {
                ruleStatus = RuleStatus.destRuleFail;
            }
        }
        if (ruleStatus == RuleStatus.noRule && routeInfo.getSourceRouteRule() != null) {
            // 然后匹配outbound规则
            sourceFilteredInstances = getRuleFilteredInstances(routeInfo, instances,
                    RuleMatchType.sourceRouteRuleMatch, matchStatus);
            if (!sourceFilteredInstances.isEmpty()) {
                ruleStatus = RuleStatus.sourceRuleSucc;
            }
            if (sourceFilteredInstances.isEmpty() && matchStatus.matched) {
                ruleStatus = RuleStatus.sourceRuleFail;
            }
        }
        switch (ruleStatus) {
            case sourceRuleSucc:
                return new RouteResult(sourceFilteredInstances, RouteResult.State.Next);
            case destRuleSucc:
                return new RouteResult(destFilteredInstances, RouteResult.State.Next);
            case noRule:
                return new RouteResult(instances.getInstances(), RouteResult.State.Next);
            default:
                LOG.warn("route rule not match, rule status: {}, not matched source {}", ruleStatus,
                        routeInfo.getSourceService());

                //请求里的配置优先级高于配置文件
                RuleBasedRouterFailoverType failoverType = routeInfo.getRuleBasedRouterFailoverType();
                if (failoverType == null) {
                    failoverType = routerConfig.getFailoverType();
                }

                if (failoverType == RuleBasedRouterFailoverType.none && !matchStatus.fallback) {
                    return new RouteResult(Collections.emptyList(), RouteResult.State.Next);
                }

                return new RouteResult(instances.getInstances(), RouteResult.State.Next);
        }
    }

    private static class MatchStatus {

        boolean matched;

        boolean fallback = false;
    }

    private List<Instance> getHealthyInstances(List<Instance> instances) {
        List<Instance> healthyInstances = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.isHealthy()) {
                healthyInstances.add(instance);
            }
        }

        return healthyInstances;
    }

    private List<Instance> getHealthyInstancesOrAllIfAllDead(List<Instance> instances) {
        List<Instance> healthyInstances = getHealthyInstances(instances);
        if (!healthyInstances.isEmpty()) {
            //如果有健康实例，则只返回健康实例
            LOG.info("getHealthyInstancesOrAllIfAllDead found {} healthy instances, return heathy ones",
                    healthyInstances.size());
            return healthyInstances;
        }
        return instances;
    }

    private List<Instance> getInstancesOfEnv(ServiceInstances serviceInstances, String env) {
        List<Instance> instances = serviceInstances.getInstances();
        List<Instance> retInstances = new ArrayList<>();
        for (Instance ins : instances) {
            if (env.equals(ins.getMetadata().get("env"))) {
                retInstances.add(ins);
            }
        }

        return retInstances;
    }


    @Override
    public String getName() {
        return ServiceRouterConfig.DEFAULT_ROUTER_RULE;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return RuleBasedRouterConfig.class;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVICE_ROUTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        globalVariablesConfig = ctx.getConfig().getGlobal().getSystem().getVariables();
        this.routerConfig = ctx.getConfig().getConsumer().getServiceRouter()
                .getPluginConfig(getName(), RuleBasedRouterConfig.class);
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        flowCache = extensions.getFlowCache();
    }

    @Override
    public ServiceRouter.Aspect getAspect() {
        return ServiceRouter.Aspect.MIDDLE;
    }

    @Override
    public boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo) {
        if (!super.enable(routeInfo, dstSvcInfo)) {
            return false;
        }

        if (routeInfo.getSourceService() == null) {
            return false;
        }

        //默认开启，需要显示关闭
        boolean enabled = true;
        if (routeInfo.getMetadataContainerGroup() != null && routeInfo.getMetadataContainerGroup().getCustomMetadataContainer() != null) {
            String enabledStr = routeInfo.getMetadataContainerGroup().getCustomMetadataContainer().getRawMetadataMapValue(ROUTER_TYPE_RULE_BASED, ROUTER_ENABLED);
            if (StringUtils.isNotBlank(enabledStr) && !Boolean.parseBoolean(enabledStr)) {
                enabled = false;
            }
        }
        Map<String, String> routerMetadata = routeInfo.getRouterMetadata(ROUTER_TYPE_RULE_BASED);
        if (MapUtils.isNotEmpty(routerMetadata)) {
            String enabledStr = routerMetadata.get(ROUTER_ENABLED);
            if (StringUtils.isNotBlank(enabledStr) && !Boolean.parseBoolean(enabledStr)) {
                enabled = false;
            }
        }
        if (!enabled) {
            return false;
        }

        List<RoutingProto.Route> dstRoutes = getRoutesFromRule(routeInfo, RuleMatchType.destRouteRuleMatch);
        List<RoutingProto.Route> srcRoutes = getRoutesFromRule(routeInfo, RuleMatchType.sourceRouteRuleMatch);

        return !(CollectionUtils.isEmpty(dstRoutes) && CollectionUtils.isEmpty(srcRoutes));
    }

    //just for test
    void setRouterConfig(RuleBasedRouterConfig routerConfig) {
        this.routerConfig = routerConfig;
    }
}
