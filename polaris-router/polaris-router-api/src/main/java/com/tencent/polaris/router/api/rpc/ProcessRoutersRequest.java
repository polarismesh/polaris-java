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

package com.tencent.polaris.router.api.rpc;

import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 路由处理请求
 */
public class ProcessRoutersRequest extends RequestBaseEntity {

    private ServiceInfo sourceService;

    private RouterNamesGroup routers;

    private ServiceInstances dstInstances;

    private String method;

    //各个路由插件依赖的 metadata 参数
    private Map<String, Set<RouteArgument>> routerArgument;
    //元数据路由降级策略
    private MetadataFailoverType metadataFailoverType;

    public ServiceInfo getSourceService() {
        return sourceService;
    }

    public void setSourceService(ServiceInfo sourceService) {
        this.sourceService = sourceService;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public RouterNamesGroup getRouters() {
        return routers;
    }

    public void setRouters(RouterNamesGroup routers) {
        this.routers = routers;
    }

    public ServiceInstances getDstInstances() {
        return dstInstances;
    }

    public void setDstInstances(ServiceInstances dstInstances) {
        this.dstInstances = dstInstances;
    }

    public void putRouterArgument(String routerType, Set<RouteArgument> arguments) {
        if (CollectionUtils.isEmpty(arguments)) {
            return;
        }
        if (routerArgument == null) {
            routerArgument = new HashMap<>();
        }

        routerArgument.put(routerType, arguments);
    }

    public void addRouterMetadata(String routerType, Set<RouteArgument> arguments) {
        if (CollectionUtils.isEmpty(arguments)) {
            return;
        }

        if (routerArgument == null) {
            routerArgument = new HashMap<>();
        }

        Set<RouteArgument> subArguments = routerArgument.computeIfAbsent(routerType, k -> new HashSet<>());
        subArguments.addAll(arguments);
    }

    public Map<String, String> getRouterMetadata(String routerType) {
        if (routerArgument == null) {
            return Collections.emptyMap();
        }
        Set<RouteArgument> arguments = routerArgument.get(routerType);
        if (arguments == null || arguments.size() == 0) {
            return Collections.emptyMap();
        }

        Map<String, String> labels = new HashMap<>();
        arguments.forEach(entry -> {
            entry.toLabel(labels);
        });

        return Collections.unmodifiableMap(labels);
    }

    @Deprecated
    public Map<String, Map<String, String>> getRouterMetadata() {
        Map<String, Map<String, String>> routerMetadata = new HashMap<>();

        routerArgument.forEach((s, arguments) -> {
            routerMetadata.computeIfAbsent(s, key -> new HashMap<>());
            Map<String, String> labels = routerMetadata.get(s);
            arguments.forEach(routeArgument -> routeArgument.toLabel(labels));
        });

        return routerMetadata;
    }

    public Map<String, Set<RouteArgument>> getRouterArguments() {
        return Collections.unmodifiableMap(routerArgument);
    }

    public MetadataFailoverType getMetadataFailoverType() {
        return metadataFailoverType;
    }

    public void setMetadataFailoverType(MetadataFailoverType metadataFailoverType) {
        this.metadataFailoverType = metadataFailoverType;
    }

    public static class RouterNamesGroup {

        private List<String> beforeRouters;

        private List<String> coreRouters;

        private List<String> afterRouters;

        public List<String> getBeforeRouters() {
            return beforeRouters;
        }

        public void setBeforeRouters(List<String> beforeRouters) {
            this.beforeRouters = beforeRouters;
        }

        public List<String> getCoreRouters() {
            return coreRouters;
        }

        public void setCoreRouters(List<String> coreRouters) {
            this.coreRouters = coreRouters;
        }

        public List<String> getAfterRouters() {
            return afterRouters;
        }

        public void setAfterRouters(List<String> afterRouters) {
            this.afterRouters = afterRouters;
        }
    }
}
