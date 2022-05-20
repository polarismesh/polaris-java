/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.tencent.polaris.api.plugin.route;

import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.pojo.StatusDimension.Level;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.api.utils.StringUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务路由信息
 *
 * @author andrewshan
 * @date 2019/9/23
 */
public class RouteInfo {

    //源服务信息
    private final ServiceMetadata sourceService;
    //目的服务信息
    private final ServiceMetadata destService;

    //源路由规则
    private ServiceRule sourceRouteRule;
    //目的路由规则
    private ServiceRule destRouteRule;

    private String canary;

    private final Map<Level, StatusDimension> statusDimensions;

    //开启哪些路由
    private final Map<String, Boolean> chainEnable = new HashMap<>();
    // 是否包含不健康的服务实例，默认false
    private boolean includeUnhealthyInstances;
    // 是否包含被熔断的服务实例，默认false
    private boolean includeCircuitBreakInstances;
    //元数据路由降级类型
    private MetadataFailoverType metadataFailoverType;
    //各个路由插件依赖的 metadata 参数
    private Map<String, Map<String, String>> routerMetadata;

    /**
     * 下一步的路由信息
     */
    private RouteResult.NextRouterInfo nextRouterInfo;

    /**
     * 构造器
     *
     * @param sourceService 源服务
     * @param sourceRouteRule 源规则
     * @param destService 目标服务
     * @param destRouteRule 目标规则
     * @param method 接口名
     */
    public RouteInfo(ServiceMetadata sourceService, ServiceRule sourceRouteRule,
            ServiceMetadata destService, ServiceRule destRouteRule, String method) {
        this.sourceService = sourceService;
        this.sourceRouteRule = sourceRouteRule;
        this.destService = destService;
        this.destRouteRule = destRouteRule;
        Map<Level, StatusDimension> dimensionMap = new HashMap<>();
        dimensionMap.put(Level.SERVICE, StatusDimension.EMPTY_DIMENSION);
        if (StringUtils.isNotBlank(method)) {
            dimensionMap.put(Level.ALL_CALLER, new StatusDimension(method, null));
        }
        if (null != sourceService) {
            dimensionMap.put(Level.ALL_METHOD, new StatusDimension("", sourceService));
        }
        if (StringUtils.isNotBlank(method) && null != sourceService) {
            dimensionMap.put(Level.CALLER_METHOD, new StatusDimension(method, sourceService));
        }
        this.statusDimensions = Collections.unmodifiableMap(dimensionMap);
    }

    /**
     * 构造器
     *
     * @param sourceService 源服务
     * @param destService 目标服务
     * @param method 接口名
     */
    public RouteInfo(ServiceMetadata sourceService, ServiceMetadata destService, String method) {
        this(sourceService, null, destService, null, method);
    }

    public MetadataFailoverType getMetadataFailoverType() {
        return metadataFailoverType;
    }

    public void setMetadataFailoverType(MetadataFailoverType metadataFailoverType) {
        this.metadataFailoverType = metadataFailoverType;
    }

    public Map<Level, StatusDimension> getStatusDimensions() {
        return statusDimensions;
    }

    public void setSourceRouteRule(ServiceRule sourceRouteRule) {
        this.sourceRouteRule = sourceRouteRule;
    }

    public void setDestRouteRule(ServiceRule destRouteRule) {
        this.destRouteRule = destRouteRule;
    }

    public RouteResult.NextRouterInfo getNextRouterInfo() {
        return nextRouterInfo;
    }

    public void setNextRouterInfo(RouteResult.NextRouterInfo nextRouterInfo) {
        this.nextRouterInfo = nextRouterInfo;
    }

    public String getCanary() {
        return canary;
    }

    public void setCanary(String canary) {
        this.canary = canary;
    }

    /**
     * 判断某个路由是否开启
     *
     * @param routerType 路由类型
     * @return 是否开启
     */
    public Boolean routerIsEnabled(String routerType) {
        return chainEnable.get(routerType);
    }

    /**
     * 开启某个路由
     *
     * @param routerType 路由类型
     */
    public void enableRouter(String routerType) {
        chainEnable.put(routerType, true);
    }

    /**
     * 关闭某个路由
     *
     * @param routerType routerType
     */
    public void disableRouter(String routerType) {
        chainEnable.put(routerType, false);
    }

    public ServiceMetadata getSourceService() {
        return sourceService;
    }

    public ServiceRule getSourceRouteRule() {
        return sourceRouteRule;
    }

    public ServiceMetadata getDestService() {
        return destService;
    }

    public ServiceRule getDestRouteRule() {
        return destRouteRule;
    }

    public boolean isIncludeUnhealthyInstances() {
        return includeUnhealthyInstances;
    }

    public void setIncludeUnhealthyInstances(boolean includeUnhealthyInstances) {
        this.includeUnhealthyInstances = includeUnhealthyInstances;
    }

    public boolean isIncludeCircuitBreakInstances() {
        return includeCircuitBreakInstances;
    }

    public void setIncludeCircuitBreakInstances(boolean includeCircuitBreakInstances) {
        this.includeCircuitBreakInstances = includeCircuitBreakInstances;
    }

    public Map<String, String> getRouterMetadata(String routerType) {
        if (routerMetadata == null) {
            return Collections.emptyMap();
        }
        Map<String, String> metadata = routerMetadata.get(routerType);
        if (metadata == null || metadata.size() == 0) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(metadata);
    }

    public void setRouterMetadata(Map<String, Map<String, String>> routerMetadata) {
        this.routerMetadata = routerMetadata;
    }
}
