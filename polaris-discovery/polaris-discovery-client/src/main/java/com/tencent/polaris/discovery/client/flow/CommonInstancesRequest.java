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

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceEventKeysProvider;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.flow.FlowControlParam;
import java.util.HashSet;
import java.util.Set;

/**
 * 基础的服务信息请求
 */
public class CommonInstancesRequest implements ServiceEventKeysProvider, FlowControlParam {

    private final ServiceEventKey srcRuleEventKey;

    private final ServiceEventKey dstInstanceEventKey;

    private final ServiceEventKey dstRuleEventKey;

    private final RouteInfo routeInfo;

    private ServiceInstances dstInstances;

    private final Criteria criteria;

    private final Set<ServiceEventKey> svcEventKeys = new HashSet<>();

    private long timeoutMs;

    private int maxRetry;

    private long retryIntervalMs;

    /**
     * 构造函数
     *
     * @param request 请求
     * @param configuration 配置
     */
    public CommonInstancesRequest(GetAllInstancesRequest request, Configuration configuration) {
        ServiceKey dstSvcKey = new ServiceKey(request.getNamespace(), request.getService());
        dstInstanceEventKey = new ServiceEventKey(dstSvcKey, EventType.INSTANCE);
        svcEventKeys.add(dstInstanceEventKey);
        dstRuleEventKey = null;
        srcRuleEventKey = null;
        routeInfo = null;
        criteria = null;
        BaseFlow.buildFlowControlParam(request, configuration, this);
    }

    /**
     * 构造函数，获取健康的全部实例。只保留：isolatedRouter，recoverRouter
     *
     * @param request 请求
     * @param configuration 配置
     */
    public CommonInstancesRequest(GetHealthyInstancesRequest request, Configuration configuration) {
        ServiceKey dstSvcKey = new ServiceKey(request.getNamespace(), request.getService());
        dstInstanceEventKey = new ServiceEventKey(dstSvcKey, EventType.INSTANCE);
        svcEventKeys.add(dstInstanceEventKey);

        dstRuleEventKey = new ServiceEventKey(dstSvcKey, EventType.ROUTING);
        svcEventKeys.add(dstRuleEventKey);

        srcRuleEventKey = null;

        ServiceInfo dstServiceInfo = new ServiceInfo();
        dstServiceInfo.setNamespace(request.getNamespace());
        dstServiceInfo.setService(request.getService());
        dstServiceInfo.setMetadata(request.getMetadata());

        routeInfo = new RouteInfo(null, dstServiceInfo, null);
        routeInfo.setIncludeUnhealthyInstances(false);
        criteria = null;

        // 关闭非必要的 Router，只保留 isolatedRouter，recoverRouter 两个最基本的 Router。
        // 并且 recoverRouter 只过滤掉 unHealthy 的实例，不需要过滤被熔断的实例
        routeInfo.disableRouter(ServiceRouterConfig.DEFAULT_ROUTER_METADATA);
        routeInfo.disableRouter(ServiceRouterConfig.DEFAULT_ROUTER_RULE);
        routeInfo.disableRouter(ServiceRouterConfig.DEFAULT_ROUTER_NEARBY);
        routeInfo.disableRouter(ServiceRouterConfig.DEFAULT_ROUTER_SET);
        routeInfo.disableRouter(ServiceRouterConfig.DEFAULT_ROUTER_CANARY);

        BaseFlow.buildFlowControlParam(request, configuration, this);
    }

        /**
         * 构造函数
         *
         * @param request 请求
         * @param configuration 配置
         */
    public CommonInstancesRequest(GetOneInstanceRequest request, Configuration configuration) {
        ServiceKey dstSvcKey = new ServiceKey(request.getNamespace(), request.getService());
        dstInstanceEventKey = new ServiceEventKey(dstSvcKey, EventType.INSTANCE);
        svcEventKeys.add(dstInstanceEventKey);
        dstRuleEventKey = new ServiceEventKey(dstSvcKey, EventType.ROUTING);
        svcEventKeys.add(dstRuleEventKey);
        SourceService srcServiceInfo = request.getServiceInfo();
        if (null != srcServiceInfo && !StringUtils.isBlank(srcServiceInfo.getNamespace()) && !StringUtils
                .isBlank(srcServiceInfo.getService())) {
            ServiceKey srcService = new ServiceKey(srcServiceInfo.getNamespace(), srcServiceInfo.getService());
            srcRuleEventKey = new ServiceEventKey(srcService, EventType.ROUTING);
            svcEventKeys.add(srcRuleEventKey);
        } else {
            srcRuleEventKey = null;
        }
        ServiceInfo dstServiceInfo = new ServiceInfo();
        dstServiceInfo.setNamespace(request.getNamespace());
        dstServiceInfo.setService(request.getService());
        dstServiceInfo.setMetadata(request.getMetadata());
        routeInfo = new RouteInfo(srcServiceInfo, dstServiceInfo, request.getMethod());
        routeInfo.setCanary(request.getCanary());
        routeInfo.setMetadataFailoverType(request.getMetadataFailoverType());
        criteria = request.getCriteria();
        BaseFlow.buildFlowControlParam(request, configuration, this);
    }

    /**
     * 构造函数
     *
     * @param request 请求
     * @param configuration 配置
     */
    public CommonInstancesRequest(GetInstancesRequest request, Configuration configuration) {
        ServiceKey dstSvcKey = new ServiceKey(request.getNamespace(), request.getService());
        dstInstanceEventKey = new ServiceEventKey(dstSvcKey, EventType.INSTANCE);
        svcEventKeys.add(dstInstanceEventKey);
        dstRuleEventKey = new ServiceEventKey(dstSvcKey, EventType.ROUTING);
        svcEventKeys.add(dstRuleEventKey);
        SourceService srcServiceInfo = request.getSourceService();
        if (null != srcServiceInfo && StringUtils.isNotBlank(srcServiceInfo.getNamespace()) && StringUtils
                .isNotBlank(srcServiceInfo.getService())) {
            ServiceKey srcService = new ServiceKey(srcServiceInfo.getNamespace(), srcServiceInfo.getService());
            srcRuleEventKey = new ServiceEventKey(srcService, EventType.ROUTING);
            svcEventKeys.add(srcRuleEventKey);
        } else {
            srcRuleEventKey = null;
        }
        ServiceInfo dstServiceInfo = new ServiceInfo();
        dstServiceInfo.setNamespace(request.getNamespace());
        dstServiceInfo.setService(request.getService());
        dstServiceInfo.setMetadata(request.getMetadata());
        routeInfo = new RouteInfo(srcServiceInfo, dstServiceInfo, request.getMethod());
        routeInfo.setIncludeCircuitBreakInstances(request.isIncludeCircuitBreak());
        routeInfo.setIncludeUnhealthyInstances(request.isIncludeUnhealthy());
        routeInfo.setCanary(request.getCanary());
        routeInfo.setMetadataFailoverType(request.getMetadataFailoverType());
        criteria = null;
        BaseFlow.buildFlowControlParam(request, configuration, this);
    }

    public ServiceEventKey getSrcRuleEventKey() {
        return srcRuleEventKey;
    }

    public ServiceEventKey getDstInstanceEventKey() {
        return dstInstanceEventKey;
    }

    public ServiceEventKey getDstRuleEventKey() {
        return dstRuleEventKey;
    }

    public RouteInfo getRouteInfo() {
        return routeInfo;
    }

    public ServiceInstances getDstInstances() {
        return dstInstances;
    }

    public void setDstInstances(ServiceInstances dstInstances) {
        this.dstInstances = dstInstances;
    }

    public Criteria getCriteria() {
        return criteria;
    }

    @Override
    public boolean isUseCache() {
        return false;
    }

    @Override
    public Set<ServiceEventKey> getSvcEventKeys() {
        return svcEventKeys;
    }

    @Override
    public ServiceEventKey getSvcEventKey() {
        return null;
    }

    @Override
    public long getTimeoutMs() {
        return timeoutMs;
    }

    @Override
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public long getRetryIntervalMs() {
        return retryIntervalMs;
    }

    @Override
    public void setRetryIntervalMs(long retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    @Override
    public int getMaxRetry() {
        return maxRetry;
    }

    @Override
    public void setMaxRetry(int maxRetry) {
        this.maxRetry = maxRetry;
    }
}
