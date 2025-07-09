/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.router.mirroring;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.TrieUtil;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.manager.MetadataContainerGroup;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import com.tencent.polaris.plugins.router.common.RoutingUtils;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.TrafficMirroringProto;
import org.slf4j.Logger;

import java.util.*;

import static com.tencent.polaris.api.config.consumer.ServiceRouterConfig.DEFAULT_ROUTER_TRAFFIC_MIRRORING;
import static com.tencent.polaris.api.plugin.cache.CacheConstants.API_ID;
import static com.tencent.polaris.api.plugin.route.RouterConstants.TRAFFIC_MIRRORING_NODE_KEY;
import static com.tencent.polaris.client.util.Utils.isHealthyInstance;

/**
 * 基于流量镜像的服务路由能力
 *
 * @author Haotian Zhang
 */
public class TrafficMirroringRouter extends AbstractServiceRouter {

    private static final Logger LOG = LoggerFactory.getLogger(TrafficMirroringRouter.class);

    private FlowCache flowCache;

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances instances) throws PolarisException {
        try {
            List<TrafficMirroringProto.TrafficMirroring> trafficMirroringList = getTrafficMirroringRules(routeInfo, instances);
            if (CollectionUtils.isNotEmpty(trafficMirroringList)) {
                for (TrafficMirroringProto.TrafficMirroring trafficMirroring : trafficMirroringList) {
                    if (trafficMirroring != null && trafficMirroring.getEnabled().getValue()) {
                        LOG.debug("TrafficMirroringProto.TrafficMirroring:{}", trafficMirroring);

                        MetadataContainerGroup metadataContainerGroup = routeInfo.getMetadataContainerGroup();
                        if (metadataContainerGroup == null) {
                            continue;
                        }
                        // 匹配source规则
                        boolean sourceMatched = matchSource(trafficMirroring.getSourcesList(),
                                routeInfo.getSourceService(), metadataContainerGroup);
                        if (!sourceMatched) {
                            LOG.debug("Source not matched, skipping traffic mirroring. TrafficMirroringProto.TrafficMirroring:{}", trafficMirroring);
                            continue;
                        }
                        // 计算百分比是否命中
                        float mirroringPercent = trafficMirroring.getMirroringPercent().getValue();
                        if (mirroringPercent < 0 || mirroringPercent > 100) {
                            mirroringPercent = 100;
                        }
                        float randomValue = new Random().nextFloat(100);
                        boolean percentMatched = randomValue < mirroringPercent;
                        if (!percentMatched) {
                            continue;
                        } else {
                            LOG.debug("Random value: {}, mirroring percent: {}", randomValue, mirroringPercent);
                        }
                        List<Instance> trafficMirroringInstances = new ArrayList<>();
                        for (Instance instance : instances.getInstances()) {
                            if (matchDestination(trafficMirroring.getDestinationsList(), instance) && isHealthyInstance(instance)) {
                                trafficMirroringInstances.add(instance);
                            }
                        }
                        if (CollectionUtils.isNotEmpty(trafficMirroringInstances)) {
                            Random random = new Random();
                            int index = random.nextInt(trafficMirroringInstances.size());
                            Node trafficMirroringNode = new Node(trafficMirroringInstances.get(index).getHost(), trafficMirroringInstances.get(index).getPort());
                            metadataContainerGroup.getCustomMetadataContainer().putMetadataObjectValue(TRAFFIC_MIRRORING_NODE_KEY, trafficMirroringNode);
                            LOG.debug("Node {} match traffic mirroring rule destination:{}.", trafficMirroringNode, trafficMirroring);
                            break;
                        } else {
                            LOG.warn("No Node match traffic mirroring rule destination:{}.", trafficMirroring);
                        }
                    }
                }
            }
        } catch (Throwable throwable) {
            LOG.warn("TrafficMirroringRouter failed.", throwable);
        }
        return new RouteResult(instances.getInstances(), RouteResult.State.Next);
    }

    /**
     * 匹配source规则
     */
    private boolean matchSource(List<RoutingProto.Source> sources, Service sourceService, MetadataContainerGroup metadataContainerGroup) {
        if (CollectionUtils.isEmpty(sources)) {
            return true;
        }
        // source匹配成功标志
        boolean matched = true;
        for (RoutingProto.Source source : sources) {
            // 匹配source服务
            matched = RoutingUtils.matchSourceService(source, sourceService);
            if (!matched) {
                continue;
            }
            // 匹配source metadata
            matched = RoutingUtils.matchSourceMetadata(source, sourceService, metadataContainerGroup,
                    key -> flowCache.loadPluginCacheObject(API_ID, key, path -> TrieUtil.buildSimpleApiTrieNode((String) path)));
            if (matched) {
                break;
            }
        }
        return matched;
    }

    /**
     * 匹配destination规则
     */
    private boolean matchDestination(List<RoutingProto.Destination> destinationList, Instance instance) {
        // destination匹配成功标志
        boolean matched = false;
        for (RoutingProto.Destination destination : destinationList) {
            Map<String, ModelProto.MatchString> destMetadataMap = destination.getMetadataMap();
            if (RuleUtils.matchMetadata(destMetadataMap, instance.getMetadata())) {
                matched = true;
                break;
            }
        }
        return matched;
    }

    /**
     * 获取流量镜像规则
     *
     * @param routeInfo
     * @param instances
     * @return
     */
    public List<TrafficMirroringProto.TrafficMirroring> getTrafficMirroringRules(RouteInfo routeInfo, ServiceInstances instances) {
        ServiceKey serviceKey = instances.getServiceKey();
        if (Objects.isNull(serviceKey)) {
            return Collections.emptyList();
        }
        if (StringUtils.isBlank(serviceKey.getService()) || StringUtils.isBlank(serviceKey.getNamespace())) {
            return Collections.emptyList();
        }

        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(new RequestBaseEntity(), extensions.getConfiguration(), engineFlowControlParam);
        Set<ServiceEventKey> routerKeys = new HashSet<>();
        ServiceEventKey dstSvcEventKey = ServiceEventKey.builder().serviceKey(serviceKey).eventType(ServiceEventKey.EventType.TRAFFIC_MIRRORING).build();
        routerKeys.add(dstSvcEventKey);
        DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
        svcKeysProvider.setSvcEventKeys(routerKeys);
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(extensions, false, svcKeysProvider, engineFlowControlParam);
        ServiceRule trafficMirroringServiceRule = resourcesResponse.getServiceRule(dstSvcEventKey);
        Object rule = trafficMirroringServiceRule.getRule();
        if (Objects.nonNull(rule) && rule instanceof ResponseProto.DiscoverResponse) {
            return ((ResponseProto.DiscoverResponse) rule).getTrafficMirroringList();
        }
        return Collections.emptyList();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        super.postContextInit(extensions);
        flowCache = extensions.getFlowCache();
    }

    @Override
    public String getName() {
        return DEFAULT_ROUTER_TRAFFIC_MIRRORING;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.MIDDLE;
    }
}