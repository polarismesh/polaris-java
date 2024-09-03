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

package com.tencent.polaris.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.APIConfig;
import com.tencent.polaris.api.config.provider.ServiceConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.DefaultRouterChainGroup;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.compose.RouterChainGroup;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.plugin.weight.WeightAdjuster;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.rpc.Criteria;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tencent.polaris.api.plugin.route.RouterConstants.ROUTER_FAULT_TOLERANCE_ENABLE;

/**
 * 同步调用流程
 *
 * @author andrewshan
 * @date 2019/8/24
 */
public class BaseFlow {


    private static final Logger LOG = LoggerFactory.getLogger(BaseFlow.class);

    /**
     * 通用获取单个服务实例的方法，用于SDK内部调用
     *
     * @param extensions      插件上下文
     * @param serviceKey      服务信息
     * @param coreRouterNames 核心路由插件链
     * @param lbPolicy        负载均衡策略
     * @param protocol        协议信息
     * @param hashKey         一致性hash的key
     * @return 过滤后的实例
     */
    public static Instance commonGetOneInstance(Extensions extensions, ServiceKey serviceKey,
                                                List<String> coreRouterNames, String lbPolicy, String protocol, String hashKey) {
        ServiceEventKey svcEventKey = new ServiceEventKey(serviceKey, EventType.INSTANCE);
        LOG.debug("[ConnectionManager]start to discover service {}", svcEventKey);
        DefaultServiceEventKeysProvider provider = new DefaultServiceEventKeysProvider();
        provider.setSvcEventKey(svcEventKey);
        //为性能考虑，优先使用本地缓存
        provider.setUseCache(true);
        FlowControlParam flowControlParam = new DefaultFlowControlParam();
        APIConfig apiConfig = extensions.getConfiguration().getGlobal().getAPI();
        flowControlParam.setTimeoutMs(apiConfig.getTimeout());
        flowControlParam.setMaxRetry(apiConfig.getMaxRetryTimes());
        flowControlParam.setRetryIntervalMs(apiConfig.getRetryInterval());
        //执行服务路由
        ServiceInfo dstSvcInfo = new ServiceInfo();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("protocol", protocol);
        dstSvcInfo.setMetadata(metadata);
        ServiceConfig serviceConfig = extensions.getConfiguration().getProvider().getService();
        RouteInfo routeInfo = new RouteInfo(
                null, null, dstSvcInfo, null, "", serviceConfig);
        ResourcesResponse resourcesResponse = BaseFlow
                .syncGetResources(extensions, false, provider, flowControlParam);
        LOG.debug("[ConnectionManager]success to discover service {}", svcEventKey);
        ServiceInstances serviceInstances = resourcesResponse.getServiceInstances(svcEventKey);
        RouterChainGroup sysRouterChainGroup = extensions.getSysRouterChainGroup();
        List<ServiceRouter> coreRouters = Extensions
                .loadServiceRouters(coreRouterNames, extensions.getPlugins(), false);
        RouterChainGroup routerChainGroup = new DefaultRouterChainGroup(sysRouterChainGroup.getBeforeRouters(),
                coreRouters, sysRouterChainGroup.getAfterRouters());
        ServiceInstances instancesAfterRoute = BaseFlow
                .processServiceRouters(routeInfo, serviceInstances, routerChainGroup);

        //执行负载均衡
        LoadBalancer loadBalancer = (LoadBalancer) extensions.getPlugins()
                .getPlugin(PluginTypes.LOAD_BALANCER.getBaseType(), lbPolicy);
        Criteria criteria = new Criteria();
        criteria.setHashKey(hashKey);
        return BaseFlow.processLoadBalance(loadBalancer, criteria, instancesAfterRoute, extensions.getWeightAdjusters());
    }

    /**
     * 处理服务路由
     *
     * @param routeInfo        路由信息
     * @param dstInstances     目标实例列表
     * @param routerChainGroup 插件链
     * @return 过滤后的实例
     * @throws PolarisException 异常
     */
    public static ServiceInstances processServiceRouters(RouteInfo routeInfo, ServiceInstances dstInstances,
                                                         RouterChainGroup routerChainGroup) throws PolarisException {
        if (null == dstInstances || CollectionUtils.isEmpty(dstInstances.getInstances())) {
            return dstInstances;
        }
        boolean processed = false;
        ServiceInstancesWrap serviceInstancesWrap = new ServiceInstancesWrap(
                dstInstances, dstInstances.getInstances(), dstInstances.getTotalWeight());
        //先走前置路由
        if (processRouterChain(routerChainGroup.getBeforeRouters(), routeInfo, serviceInstancesWrap)) {
            processed = true;
        }
        Map<String, String> destSvcMetadata = Optional.ofNullable(serviceInstancesWrap.getMetadata()).orElse(Collections.emptyMap());
        List<Instance> faultToleranceServiceInstances = new ArrayList<>();
        if (Boolean.parseBoolean(destSvcMetadata.get(ROUTER_FAULT_TOLERANCE_ENABLE))) {
            faultToleranceServiceInstances = new ArrayList<>(dstInstances.getInstances());
        }
        //再走业务路由
        if (processRouterChain(routerChainGroup.getCoreRouters(), routeInfo, serviceInstancesWrap)) {
            processed = true;
        }
        if (CollectionUtils.isEmpty(serviceInstancesWrap.getInstances())
                && Boolean.parseBoolean(destSvcMetadata.get(ROUTER_FAULT_TOLERANCE_ENABLE))) {
            serviceInstancesWrap.setInstances(faultToleranceServiceInstances);
        }
        //最后走后置路由
        if (processRouterChain(routerChainGroup.getAfterRouters(), routeInfo, serviceInstancesWrap)) {
            processed = true;
        }
        if (processed) {
            serviceInstancesWrap.reloadTotalWeight();
        }
        return serviceInstancesWrap;
    }

    private static boolean processRouterChain(List<ServiceRouter> routers,
                                              RouteInfo routeInfo, ServiceInstancesWrap serviceInstances) throws PolarisException {
        if (CollectionUtils.isEmpty(routers)) {
            return false;
        }
        boolean processed = false;
        for (ServiceRouter router : routers) {
            if (CollectionUtils.isEmpty(serviceInstances.getInstances())) {
                //实例为空，则退出路由
                break;
            }
            if (!router.enable(routeInfo, serviceInstances)) {
                continue;
            }
            processed = true;
            do {
                RouteResult filteredInstances = router.getFilteredInstances(routeInfo, serviceInstances);
                RouteResult.NextRouterInfo nextRouterInfo = filteredInstances.getNextRouterInfo();
                if (nextRouterInfo.getState() == RouteResult.State.Next) {
                    serviceInstances.setInstances(filteredInstances.getInstances());
                    LOG.debug("router: {} get filtered instance result size : {} serviceInstances: {}", router.getName(),
                            serviceInstances.getInstances().size(), serviceInstances.getObjectId());
                    break;
                }
                //重试获取
                routeInfo.setNextRouterInfo(nextRouterInfo);
            } while (true);
        }
        return processed;
    }


    /**
     * 同步拉取资源数据
     *
     * @param extensions      插件集合
     * @param internalRequest 是否内部请求
     * @param paramProvider   参数提供器
     * @param controlParam    控制参数
     * @return 多资源应答
     * @throws PolarisException 获取异常
     */
    public static ResourcesResponse syncGetResources(Extensions extensions, boolean internalRequest,
                                                     ServiceEventKeysProvider paramProvider, FlowControlParam controlParam)
            throws PolarisException {

        if (CollectionUtils.isEmpty(paramProvider.getSvcEventKeys()) && null == paramProvider.getSvcEventKey()) {
            return new ResourcesResponse();
        }
        long currentTime = System.currentTimeMillis();
        long deadline = currentTime + controlParam.getTimeoutMs();
        int retryTime = 0;
        while (currentTime < deadline) {
            if (retryTime > controlParam.getMaxRetry()) {
                break;
            }
            retryTime++;
            long diffTimeout = deadline - currentTime;
            try {
                GetResourcesInvoker invoker = new GetResourcesInvoker(paramProvider, extensions, internalRequest,
                        paramProvider.isUseCache());
                return invoker.get(diffTimeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                //重试
                currentTime = Utils.sleepUninterrupted(controlParam.getRetryIntervalMs());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                LOG.error(String.format("syncGetInstances fail for services %s", paramProvider.toString()), cause);
                if (cause instanceof RetriableException) {
                    //可重试异常，尝试进行重试
                    currentTime = Utils.sleepUninterrupted(controlParam.getRetryIntervalMs());
                } else {
                    break;
                }
            } catch (TimeoutException e) {
                break;
            }
        }
        //远程访问超时，从本地缓存读取数据
        ResourcesResponse resourcesResponse = new ResourcesResponse();
        if (readResourcesFromLocalCache(paramProvider, extensions, resourcesResponse)) {
            return resourcesResponse;
        }
        String errMsg = String.format("timeout while waiting response for svcEventKeys %s, svcEventKey %s",
                paramProvider.getSvcEventKeys(), paramProvider.getSvcEventKey());
        LOG.warn(errMsg);
        return new ResourcesResponse();
    }

    private static boolean readResourcesFromLocalCache(ServiceEventKeysProvider paramProvider,
                                                       Extensions extensions, ResourcesResponse resourcesResponse) {
        LocalRegistry localRegistry = extensions.getLocalRegistry();
        if (null != paramProvider.getSvcEventKey()) {
            if (loadLocalResources(paramProvider.getSvcEventKey(), resourcesResponse, localRegistry)) {
                return false;
            }
        }
        if (CollectionUtils.isNotEmpty(paramProvider.getSvcEventKeys())) {
            for (ServiceEventKey svcEventKey : paramProvider.getSvcEventKeys()) {
                if (loadLocalResources(svcEventKey, resourcesResponse, localRegistry)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean loadLocalResources(ServiceEventKey svcEventKey, ResourcesResponse resourcesResponse,
                                              LocalRegistry localRegistry) {
        ResourceFilter filter = new ResourceFilter(svcEventKey, false, true);
        if (svcEventKey.getEventType() == EventType.INSTANCE) {
            ServiceInstances instances = localRegistry.getInstances(filter);
            if (instances.isInitialized()) {
                resourcesResponse.addServiceInstances(svcEventKey, instances);
                return false;
            } else {
                return true;
            }
        }
        if (svcEventKey.getEventType() == EventType.SERVICE) {
            Services services = localRegistry.getServices(filter);
            if (services.isInitialized()) {
                resourcesResponse.addServices(svcEventKey, services);
                return false;
            } else {
                return true;
            }
        }
        ServiceRule serviceRule = localRegistry.getServiceRule(filter);
        if (serviceRule.isInitialized()) {
            resourcesResponse.addServiceRule(svcEventKey, serviceRule);
            return false;
        } else {
            return true;
        }
    }

    public static Instance processLoadBalance(LoadBalancer loadBalancer, Criteria criteria,
            ServiceInstances dstInstances, List<WeightAdjuster> weightAdjusters) throws PolarisException {
        if (criteria == null) {
            criteria = new Criteria();
        }
        Map<String, InstanceWeight> dynamicWeight = new HashMap<>();
        if (CollectionUtils.isNotEmpty(weightAdjusters)) {
            for (WeightAdjuster weightAdjuster : weightAdjusters) {
                dynamicWeight = weightAdjuster.timingAdjustDynamicWeight(dynamicWeight, dstInstances);
            }
            if (CollectionUtils.isNotEmpty(criteria.getDynamicWeight())) {
                // rebuild dstInstances with new total weight
                int totalWeight = 0;
                for (Map.Entry<String, InstanceWeight> weightEntry : criteria.getDynamicWeight().entrySet()) {
                    totalWeight += weightEntry.getValue().getDynamicWeight();
                }
                dstInstances = new ServiceInstancesWrap(dstInstances, dstInstances.getInstances(), totalWeight);
            }
        }
        criteria.setDynamicWeight(dynamicWeight);
        Instance instance = loadBalancer.chooseInstance(criteria, dstInstances);
        LOG.debug("[processLoadBalance] choose instance: {}", instance);
        if (null == instance) {
            throw new PolarisException(ErrorCode.INSTANCE_NOT_FOUND,
                    String.format("no suitable instance for service %s after loadbanlancer %s",
                            dstInstances.getNamespace() + "-" + dstInstances.getService(), loadBalancer.getName()));
        }
        return instance;
    }

    /**
     * 构建流程控制参数
     *
     * @param entity       请求对象
     * @param config       配置对象
     * @param controlParam 控制参数
     */
    public static void buildFlowControlParam(RequestBaseEntity entity, Configuration config,
                                             FlowControlParam controlParam) {
        long timeoutMs = entity.getTimeoutMs();
        if (timeoutMs == 0) {
            timeoutMs = config.getGlobal().getAPI().getTimeout();
        }
        controlParam.setTimeoutMs(timeoutMs);
        controlParam.setMaxRetry(config.getGlobal().getAPI().getMaxRetryTimes());
        controlParam.setRetryIntervalMs(config.getGlobal().getAPI().getRetryInterval());
    }

}
