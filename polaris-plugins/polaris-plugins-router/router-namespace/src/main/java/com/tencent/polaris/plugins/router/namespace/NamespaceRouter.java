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

package com.tencent.polaris.plugins.router.namespace;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.rpc.NamespaceRouterFailoverType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import org.slf4j.Logger;

import java.util.*;

/**
 * 命名空间就近路由，优先返回同命名空间的实例节点。
 *
 * @author Haotian Zhang
 */
public class NamespaceRouter extends AbstractServiceRouter {

    private static final Logger LOG = LoggerFactory.getLogger(NamespaceRouter.class);

    private static final String NAMESPACE_ROUTER_ENABLE = "internal-enable-router-namespace";

    public static final String ROUTER_TYPE_NAMESPACE = "namespaceRoute";

    public static final String ROUTER_ENABLED = "enabled";

    /**
     * 默认关闭，需要显示打开
     *
     * @param routeInfo  路由参数
     * @param dstSvcInfo 被调服务
     * @return if enabled
     */
    @Override
    public boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo) {
        // 判断目标服务的服务元数据
        Map<String, String> destSvcMetadata = Optional.ofNullable(dstSvcInfo.getMetadata()).orElse(Collections.emptyMap());
        if (Boolean.parseBoolean(destSvcMetadata.get(NAMESPACE_ROUTER_ENABLE))) {
            return true;
        }

        // 判断目标服务的路由请求
        if (routeInfo.getMetadataContainerGroup() != null && routeInfo.getMetadataContainerGroup().getCustomMetadataContainer() != null) {
            String enabledStr = routeInfo.getMetadataContainerGroup().getCustomMetadataContainer().getRawMetadataMapValue(ROUTER_TYPE_NAMESPACE, ROUTER_ENABLED);
            return StringUtils.isNotBlank(enabledStr) && Boolean.parseBoolean(enabledStr);
        }

        return false;
    }

    @Override
    public String getName() {
        return ServiceRouterConfig.DEFAULT_ROUTER_NAMESPACE;
    }


    @Override
    public Aspect getAspect() {
        return Aspect.MIDDLE;
    }

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances svcInstances) throws PolarisException {
        List<Instance> instances = new ArrayList<>();
        for (Instance instance : svcInstances.getInstances()) {
            if (StringUtils.equals(routeInfo.getSourceService().getNamespace(), instance.getNamespace())) {
                instances.add(instance);
            }
        }
        if (CollectionUtils.isEmpty(instances)) {
            LOG.debug("No same namespace instance from {} instances of {}:{}", svcInstances.getInstances().size(),
                    routeInfo.getSourceService().getNamespace(), routeInfo.getSourceService().getService());
            if (routeInfo.getNamespaceRouterFailoverType() == NamespaceRouterFailoverType.none) {
                LOG.debug("Fail over to empty instance list of {}:{}",
                        routeInfo.getSourceService().getNamespace(), routeInfo.getSourceService().getService());
            } else {
                LOG.debug("Fail over to original instance list of {}:{}",
                        routeInfo.getSourceService().getNamespace(), routeInfo.getSourceService().getService());
                instances.addAll(svcInstances.getInstances());
            }
        }
        return new RouteResult(instances, RouteResult.State.Next);
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }
}
