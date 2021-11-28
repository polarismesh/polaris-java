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

package com.tencent.polaris.plugins.router.isolated;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import java.util.ArrayList;
import java.util.List;

/**
 * 剔除隔离的，以及不健康的节点
 */
public class IsolatedRouter extends AbstractServiceRouter {

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances svcInstances) throws PolarisException {
        List<Instance> instances = new ArrayList<>();
        for (Instance instance : svcInstances.getInstances()) {
            if (instance.getWeight() == 0 || instance.isIsolated()) {
                continue;
            }
            instances.add(instance);
        }
        return new RouteResult(instances, RouteResult.State.Next);
    }

    @Override
    public Aspect getAspect() {
        return Aspect.BEFORE;
    }

    @Override
    public boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo) {
        return true;
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {

    }

    @Override
    public String getName() {
        return ServiceRouterConfig.DEFAULT_ROUTER_ISOLATED;
    }
}
