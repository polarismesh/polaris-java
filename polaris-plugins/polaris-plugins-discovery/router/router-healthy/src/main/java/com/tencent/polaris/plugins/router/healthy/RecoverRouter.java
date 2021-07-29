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

package com.tencent.polaris.plugins.router.healthy;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全死全活路由
 */
public class RecoverRouter extends AbstractServiceRouter {

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances instances)
            throws PolarisException {
        //过滤不健康的节点，只有心跳而且没有被熔断的才是健康的
        List<Instance> healthyInstance = instances.getInstances().stream().filter(
                instance -> Utils.isHealthyInstance(instance, routeInfo.getStatusDimensions()))
                .collect(Collectors.toList());
        int healthyInstanceCount = healthyInstance.size();
        if (healthyInstanceCount == 0) {
            return new RouteResult(instances.getInstances(), RouteResult.State.Next);
        }
        return new RouteResult(healthyInstance, RouteResult.State.Next);
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVICE_ROUTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
    }

    @Override
    public String getName() {
        return ServiceRouterConfig.DEFAULT_ROUTER_RECOVER;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.AFTER;
    }

    @Override
    public boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo) {
        return true;
    }
}
