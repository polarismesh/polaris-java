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

package com.tencent.polaris.assembly.client.flow;

import java.util.Collections;
import java.util.List;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.stat.TraceReporter;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.assembly.api.pojo.GetOneInstanceRequest;
import com.tencent.polaris.assembly.api.pojo.GetReachableInstancesRequest;
import com.tencent.polaris.assembly.api.pojo.TraceAttributes;
import com.tencent.polaris.assembly.flow.AssemblyFlow;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.api.ServiceCallResultListener;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.discovery.client.flow.CommonInstancesRequest;

public class DefaultAssemblyFlow implements AssemblyFlow {

    private SDKContext sdkContext;

    private Extensions extensions;

    private List<ServiceCallResultListener> serviceCallResultListeners;

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
        this.extensions = sdkContext.getExtensions();
        serviceCallResultListeners = ServiceCallResultListener.getServiceCallResultListeners(sdkContext);
    }

    @Override
    public void initService(ServiceKey serviceKey) {
        CommonInstancesRequest commonInstancesRequest = buildCommonInstancesRequest(serviceKey, sdkContext.getConfig());
        BaseFlow.syncGetResources(extensions, false, commonInstancesRequest, commonInstancesRequest);
    }

    private static CommonInstancesRequest buildCommonInstancesRequest(ServiceKey serviceKey, Configuration configuration) {
        ServiceKey dstSvcKey = new ServiceKey(serviceKey.getNamespace(), serviceKey.getService());
        ServiceEventKey dstInstanceEventKey = new ServiceEventKey(dstSvcKey, ServiceEventKey.EventType.INSTANCE);
        ServiceEventKey dstRuleEventKey = new ServiceEventKey(dstSvcKey, ServiceEventKey.EventType.ROUTING);
        ServiceInfo dstServiceInfo = new ServiceInfo();
        dstServiceInfo.setNamespace(serviceKey.getNamespace());
        dstServiceInfo.setService(serviceKey.getService());
        RequestBaseEntity requestBaseEntity = new RequestBaseEntity();
        requestBaseEntity.setService(serviceKey.getService());
        requestBaseEntity.setNamespace(serviceKey.getNamespace());
        RouteInfo routeInfo = new RouteInfo(null, dstServiceInfo, "", configuration.getProvider().getService());
        return new CommonInstancesRequest(dstInstanceEventKey, dstRuleEventKey, null, routeInfo,
                null, requestBaseEntity, configuration);
    }

    @Override
    public List<Instance> getReachableInstances(GetReachableInstancesRequest request) {
        CommonInstancesRequest commonInstancesRequest = buildCommonInstancesRequest(request, sdkContext.getConfig());
        ResourcesResponse resourcesResponse = BaseFlow.syncGetResources(
                extensions, false, commonInstancesRequest, commonInstancesRequest);
        ServiceInstances dstInstances = resourcesResponse.getServiceInstances(commonInstancesRequest.getDstInstanceEventKey());
        if (CollectionUtils.isEmpty(dstInstances.getInstances())) {
            return Collections.emptyList();
        }
        RouteInfo routeInfo = commonInstancesRequest.getRouteInfo();
        if (null != commonInstancesRequest.getDstRuleEventKey()) {
            routeInfo.setDestRouteRule(resourcesResponse.getServiceRule(commonInstancesRequest.getDstRuleEventKey()));
        }
        if (null != commonInstancesRequest.getSrcRuleEventKey()) {
            routeInfo.setSourceRouteRule(resourcesResponse.getServiceRule(commonInstancesRequest.getSrcRuleEventKey()));
        }
        ServiceInstances routerInstances =
                BaseFlow.processServiceRouters(routeInfo, dstInstances, extensions.getConfigRouterChainGroup());
        return Collections.unmodifiableList(routerInstances.getInstances());
    }

    private static CommonInstancesRequest buildCommonInstancesRequest(GetReachableInstancesRequest request, Configuration configuration) {
        ServiceKey dstSvcKey = new ServiceKey(request.getNamespace(), request.getService());
        ServiceEventKey dstInstanceEventKey = new ServiceEventKey(dstSvcKey, ServiceEventKey.EventType.INSTANCE);
        ServiceEventKey dstRuleEventKey = new ServiceEventKey(dstSvcKey, ServiceEventKey.EventType.ROUTING);
        ServiceEventKey srcRuleEventKey = null;
        SourceService srcServiceInfo = request.getServiceInfo();
        if (null != srcServiceInfo && StringUtils.isNotBlank(srcServiceInfo.getNamespace()) && StringUtils
                .isNotBlank(srcServiceInfo.getService())) {
            ServiceKey srcService = new ServiceKey(srcServiceInfo.getNamespace(), srcServiceInfo.getService());
            srcRuleEventKey = new ServiceEventKey(srcService, ServiceEventKey.EventType.ROUTING);
        }
        ServiceInfo dstServiceInfo = new ServiceInfo();
        dstServiceInfo.setNamespace(request.getNamespace());
        dstServiceInfo.setService(request.getService());
        dstServiceInfo.setMetadata(request.getMetadata());
        RouteInfo routeInfo = new RouteInfo(srcServiceInfo, dstServiceInfo, request.getMethod(),
                configuration.getProvider().getService());
        routeInfo.setIncludeCircuitBreakInstances(request.isIncludeCircuitBreak());
        routeInfo.setIncludeUnhealthyInstances(request.isIncludeUnhealthy());
        routeInfo.setCanary(request.getCanary());
        routeInfo.setMetadataFailoverType(request.getMetadataFailoverType());
        return new CommonInstancesRequest(dstInstanceEventKey, dstRuleEventKey, srcRuleEventKey, routeInfo,
                null, request, configuration);
    }

    @Override
    public Instance getOneInstance(GetOneInstanceRequest request) {
        CommonInstancesRequest commonInstancesRequest = buildCommonInstancesRequest(request, sdkContext.getConfig());
        ResourcesResponse resourcesResponse = BaseFlow.syncGetResources(
                extensions, false, commonInstancesRequest, commonInstancesRequest);
        ServiceInstances dstInstances = resourcesResponse.getServiceInstances(commonInstancesRequest.getDstInstanceEventKey());
        if (CollectionUtils.isEmpty(dstInstances.getInstances())) {
            return null;
        }
        RouteInfo routeInfo = commonInstancesRequest.getRouteInfo();
        if (null != commonInstancesRequest.getDstRuleEventKey()) {
            routeInfo.setDestRouteRule(resourcesResponse.getServiceRule(commonInstancesRequest.getDstRuleEventKey()));
        }
        if (null != commonInstancesRequest.getSrcRuleEventKey()) {
            routeInfo.setSourceRouteRule(resourcesResponse.getServiceRule(commonInstancesRequest.getSrcRuleEventKey()));
        }
        ServiceInstances routerInstances =
                BaseFlow.processServiceRouters(routeInfo, dstInstances, extensions.getConfigRouterChainGroup());
        LoadBalancer loadBalancer = extensions.getLoadBalancer();
        return BaseFlow.processLoadBalance(loadBalancer, request.getCriteria(), routerInstances, extensions.getWeightAdjusters());
    }

    private static CommonInstancesRequest buildCommonInstancesRequest(GetOneInstanceRequest request, Configuration configuration) {
        ServiceKey dstSvcKey = new ServiceKey(request.getNamespace(), request.getService());
        ServiceEventKey dstInstanceEventKey = new ServiceEventKey(dstSvcKey, ServiceEventKey.EventType.INSTANCE);
        ServiceEventKey dstRuleEventKey = new ServiceEventKey(dstSvcKey, ServiceEventKey.EventType.ROUTING);
        ServiceEventKey srcRuleEventKey = null;
        SourceService srcServiceInfo = request.getServiceInfo();
        if (null != srcServiceInfo && StringUtils.isNotBlank(srcServiceInfo.getNamespace()) && StringUtils
                .isNotBlank(srcServiceInfo.getService())) {
            ServiceKey srcService = new ServiceKey(srcServiceInfo.getNamespace(), srcServiceInfo.getService());
            srcRuleEventKey = new ServiceEventKey(srcService, ServiceEventKey.EventType.ROUTING);
        }
        ServiceInfo dstServiceInfo = new ServiceInfo();
        dstServiceInfo.setNamespace(request.getNamespace());
        dstServiceInfo.setService(request.getService());
        dstServiceInfo.setMetadata(request.getMetadata());
        RouteInfo routeInfo = new RouteInfo(srcServiceInfo, dstServiceInfo, request.getMethod(),
                configuration.getProvider().getService());
        routeInfo.setCanary(request.getCanary());
        routeInfo.setMetadataFailoverType(request.getMetadataFailoverType());
        return new CommonInstancesRequest(dstInstanceEventKey, dstRuleEventKey, srcRuleEventKey, routeInfo,
                request.getCriteria(), request, configuration);
    }

    @Override
    public void updateServiceCallResult(ServiceCallResult result) {
        for (ServiceCallResultListener listener : serviceCallResultListeners) {
            listener.onServiceCallResult(result);
        }
    }

    @Override
    public void updateTraceAttributes(TraceAttributes traceAttributes) {
        if (!sdkContext.getConfig().getGlobal().getTraceReporter().isEnable()) {
            return;
        }
        TraceReporter traceReporter = extensions.getTraceReporter();
        if (null == traceReporter) {
            return;
        }
        switch (traceAttributes.getAttributeLocation()) {
        case SPAN:
            traceReporter.setSpanAttributes(traceAttributes.getAttributes());
            break;
        case BAGGAGE:
            traceReporter.setBaggageAttributes(traceAttributes.getAttributes());
            break;
        default:
            break;
        }
    }

}
