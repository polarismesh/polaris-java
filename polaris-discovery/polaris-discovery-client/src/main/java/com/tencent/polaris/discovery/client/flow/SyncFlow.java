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

package com.tencent.polaris.discovery.client.flow;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.api.rpc.ServicesResponse;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.ResourcesResponse;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

public class SyncFlow {

    private static final Logger LOG = LoggerFactory.getLogger(SyncFlow.class);

    private Extensions extensions;

    public void init(Extensions extensions) {
        this.extensions = extensions;
    }

    /**
     * 获取全量服务实例
     *
     * @param request 请求对象
     * @return 全量服务实例
     * @throws PolarisException 异常
     */
    public InstancesResponse commonSyncGetAllInstances(CommonInstancesRequest request) throws PolarisException {
        syncGetServiceInstances(request);
        ServiceInstances dstInstances = request.getDstInstances();
        return new InstancesResponse(dstInstances, "", null);
    }

    /**
     * 获取多个实例列表
     *
     * @param request 请求对象
     * @return 实例应答
     * @throws PolarisException 异常
     */
    public InstancesResponse commonSyncGetInstances(CommonInstancesRequest request) throws PolarisException {
        syncGetServiceInstances(request);
        ServiceInstances dstInstances = request.getDstInstances();
        if (CollectionUtils.isEmpty(dstInstances.getInstances())) {
            return new InstancesResponse(dstInstances, "", null);
        }
        RouteInfo routeInfo = request.getRouteInfo();
        ServiceInstances routerInstances =
                BaseFlow.processServiceRouters(routeInfo, request.getDstInstances(),
                        extensions.getConfigRouterChainGroup());
        return new InstancesResponse(routerInstances, routeInfo.getSubsetName(), routeInfo.getSubsetMetadata());
    }

    /**
     * 获取单个实例
     *
     * @param request 请求对象
     * @return 实例应答
     * @throws PolarisException 异常
     */
    public InstancesResponse commonSyncGetOneInstance(CommonInstancesRequest request) throws PolarisException {
        syncGetServiceInstances(request);
        ServiceInstances dstInstances = request.getDstInstances();
        if (CollectionUtils.isEmpty(dstInstances.getInstances())) {
            return new InstancesResponse(dstInstances, "", null);
        }
        RouteInfo routeInfo = request.getRouteInfo();
        ServiceInstances routerInstances =
                BaseFlow.processServiceRouters(routeInfo, request.getDstInstances(),
                        extensions.getConfigRouterChainGroup());
        LoadBalancer loadBalancer = extensions.getLoadBalancer();
        Instance instance = BaseFlow.processLoadBalance(loadBalancer, request.getCriteria(), routerInstances,
                extensions.getWeightAdjusters());
        return new InstancesResponse(dstInstances, instance, routeInfo.getSubsetName(), routeInfo.getSubsetMetadata());
    }

    /**
     * 获取服务规则信息
     *
     * @param request 请求对象
     * @return 规则数据
     * @throws PolarisException 异常
     */
    public ServiceRuleResponse commonSyncGetServiceRule(CommonRuleRequest request) throws PolarisException {
        ResourcesResponse resourcesResponse = BaseFlow.syncGetResources(extensions, false, request, request);
        return new ServiceRuleResponse(resourcesResponse.getServiceRule(request.getSvcEventKey()));
    }

    /**
     * 获取服务实例以及路由数据
     *
     * @param request 请求对象
     * @throws PolarisException 异常
     */
    private void syncGetServiceInstances(CommonInstancesRequest request) throws PolarisException {
        ResourcesResponse resourcesResponse = BaseFlow.syncGetResources(extensions, false, request, request);
        request.setDstInstances(resourcesResponse.getServiceInstances(request.getDstInstanceEventKey()));
        if (null != request.getDstRuleEventKey()) {
            request.getRouteInfo().setDestRouteRule(resourcesResponse.getServiceRule(request.getDstRuleEventKey()));
        }
        if (null != request.getSrcRuleEventKey()) {
            request.getRouteInfo().setSourceRouteRule(resourcesResponse.getServiceRule(request.getSrcRuleEventKey()));
        }
    }

    /**
     * 批量获取服务信息
     *
     * @param request 请求对象
     * @return 规则数据
     * @throws PolarisException 异常
     */
    public ServicesResponse commonSyncGetServices(CommonServicesRequest request) throws PolarisException {
        ResourcesResponse resourcesResponse = BaseFlow.syncGetResources(extensions, false, request, request);
        return new ServicesResponse(resourcesResponse.getServices(request.getSvcEventKey()));
    }
}
