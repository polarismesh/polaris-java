/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugin.router.canary;

import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.plugin.route.RouterConstants;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import java.util.List;
import java.util.stream.Collectors;

public class CanaryRouter extends AbstractServiceRouter {

    private static final String canaryMetadataEnable = "internal-canary";

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances instances)
            throws PolarisException {
        String canaryValue = routeInfo.getCanary();
        List<Instance> instanceList;
        if (!StringUtils.isBlank(canaryValue)) {
            instanceList = instances.getInstances().stream()
                    .filter(instance -> MapUtils.isNotEmpty(instance.getMetadata())
                            && StringUtils.equals(canaryValue, instance.getMetadata().get(RouterConstants.CANARY_KEY)))
                    .collect(Collectors.toList());
        } else {
            instanceList = instances.getInstances().stream()
                    .filter(instance -> MapUtils.isEmpty(instance.getMetadata())
                            || !StringUtils.equals(canaryValue, instance.getMetadata().get(RouterConstants.CANARY_KEY)))
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isEmpty(instanceList)) {
            return new RouteResult(instances.getInstances(), RouteResult.State.Next);
        }
        return new RouteResult(instanceList, RouteResult.State.Next);
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
        return ServiceRouterConfig.DEFAULT_ROUTER_CANARY;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.MIDDLE;
    }

    @Override
    public boolean enable(RouteInfo routeInfo, ServiceMetadata dstSvcInfo) {
        if (!super.enable(routeInfo, dstSvcInfo)) {
            return false;
        }

        if (!dstSvcInfo.getMetadata().containsKey(canaryMetadataEnable)) {
            return false;
        }
        return Boolean.parseBoolean(dstSvcInfo.getMetadata().get(canaryMetadataEnable));
    }
}
