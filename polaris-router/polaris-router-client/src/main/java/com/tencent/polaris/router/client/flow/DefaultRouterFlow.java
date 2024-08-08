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

package com.tencent.polaris.router.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.DefaultRouterChainGroup;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.DefaultFlowControlParam;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.router.api.flow.RouterFlow;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest.RouterNamesGroup;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DefaultRouterFlow implements RouterFlow {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultRouterFlow.class);

    private Configuration config;

    private Extensions extensions;

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        extensions = sdkContext.getExtensions();
        config = sdkContext.getConfig();
    }

    @Override
    public ProcessRoutersResponse processRouters(ProcessRoutersRequest request) {
        RouterNamesGroup requestRouterGroup = request.getRouters();
        List<ServiceRouter> beforeRouters;
        if (null != requestRouterGroup && CollectionUtils.isNotEmpty(requestRouterGroup.getBeforeRouters())) {
            beforeRouters = Extensions.loadServiceRouters(
                    requestRouterGroup.getBeforeRouters(), extensions.getPlugins(), true);
        } else {
            beforeRouters = extensions.getConfigRouterChainGroup().getBeforeRouters();
        }
        List<ServiceRouter> afterRouters;
        if (null != requestRouterGroup && CollectionUtils.isNotEmpty(requestRouterGroup.getAfterRouters())) {
            afterRouters = Extensions.loadServiceRouters(
                    requestRouterGroup.getAfterRouters(), extensions.getPlugins(), true);
        } else {
            afterRouters = extensions.getConfigRouterChainGroup().getAfterRouters();
        }
        List<ServiceRouter> coreRouters;
        if (null != requestRouterGroup && CollectionUtils.isNotEmpty(requestRouterGroup.getCoreRouters())) {
            coreRouters = Extensions.loadServiceRouters(
                    requestRouterGroup.getCoreRouters(), extensions.getPlugins(), true);
        } else {
            coreRouters = extensions.getConfigRouterChainGroup().getCoreRouters();
        }
        ServiceInstances dstInstances = request.getDstInstances();
        SourceService sourceService = new SourceService();
        sourceService.setService(request.getSourceService().getService());
        sourceService.setNamespace(request.getSourceService().getNamespace());
        RouteInfo routeInfo = new RouteInfo(sourceService, dstInstances, request.getMethod(),
                config.getProvider().getService());
        routeInfo.setRouterArguments(request.getRouterArguments());
        if (request.getMetadataFailoverType() != null) {
            routeInfo.setMetadataFailoverType(request.getMetadataFailoverType());
        }
        if (request.getRuleBasedRouterFailoverType() != null) {
            routeInfo.setRuleBasedRouterFailoverType(request.getRuleBasedRouterFailoverType());
        }
        if (request.getMetadataContainerGroup() != null) {
            routeInfo.setMetadataContainerGroup(request.getMetadataContainerGroup());
        }
        if (request.getNamespaceRouterFailoverType() != null) {
            routeInfo.setNamespaceRouterFailoverType(request.getNamespaceRouterFailoverType());
        }
        //获取路由规则
        DefaultFlowControlParam engineFlowControlParam = new DefaultFlowControlParam();
        BaseFlow.buildFlowControlParam(request, config, engineFlowControlParam);
        Set<ServiceEventKey> routerKeys = new HashSet<>();
        ServiceEventKey dstSvcEventKey = new ServiceEventKey(
                new ServiceKey(dstInstances.getNamespace(), dstInstances.getService()),
                EventType.ROUTING);
        routerKeys.add(dstSvcEventKey);
        ServiceEventKey srcSvcEventKey = null;
        if (null != routeInfo.getSourceService() && StringUtils.isNotBlank(routeInfo.getSourceService().getNamespace())
                && StringUtils.isNotBlank(routeInfo.getSourceService().getService())) {
            srcSvcEventKey = new ServiceEventKey(new ServiceKey(routeInfo.getSourceService().getNamespace(),
                    routeInfo.getSourceService().getService()),
                    EventType.ROUTING);
            routerKeys.add(srcSvcEventKey);
        }
        DefaultServiceEventKeysProvider svcKeysProvider = new DefaultServiceEventKeysProvider();
        svcKeysProvider.setSvcEventKeys(routerKeys);
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(extensions, false, svcKeysProvider, engineFlowControlParam);
        routeInfo.setDestRouteRule(resourcesResponse.getServiceRule(dstSvcEventKey));
        if (null != srcSvcEventKey) {
            routeInfo.setSourceRouteRule(resourcesResponse.getServiceRule(srcSvcEventKey));
        }
        //执行路由
        DefaultRouterChainGroup routerChainGroup = new DefaultRouterChainGroup(beforeRouters, coreRouters,
                afterRouters);
        ServiceInstances svcInstances = BaseFlow
                .processServiceRouters(routeInfo, dstInstances, routerChainGroup);
        return new ProcessRoutersResponse(svcInstances);
    }

    @Override
    public ProcessLoadBalanceResponse processLoadBalance(ProcessLoadBalanceRequest request) {
        String lbPolicy = request.getLbPolicy();
        if (StringUtils.isBlank(lbPolicy)) {
            lbPolicy = extensions.getConfiguration().getConsumer().getLoadbalancer().getType();
        }
        LoadBalancer loadBalancer = (LoadBalancer) extensions.getPlugins()
                .getPlugin(PluginTypes.LOAD_BALANCER.getBaseType(), lbPolicy);
        Instance instance = BaseFlow.processLoadBalance(loadBalancer, request.getCriteria(),
                request.getDstInstances(), extensions.getWeightAdjusters());
        return new ProcessLoadBalanceResponse(instance);
    }
}
