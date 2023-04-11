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
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.route.RouteInfo;
import com.tencent.polaris.api.plugin.route.RouteResult;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceMetadata;
import com.tencent.polaris.circuitbreak.api.pojo.CheckResult;
import com.tencent.polaris.circuitbreak.client.api.DefaultCircuitBreakAPI;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.plugins.router.common.AbstractServiceRouter;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 全死全活路由
 */
public class RecoverRouter extends AbstractServiceRouter implements PluginConfigProvider {

    private RecoverRouterConfig recoverRouterConfig;

    @Override
    public RouteResult router(RouteInfo routeInfo, ServiceInstances instances)
            throws PolarisException {
        //过滤不健康的节点，只有心跳而且没有被熔断的才是健康的
        List<Instance> healthyInstance;
        if (isExcludeCircuitBreakInstances(routeInfo)) {
            //不包含被熔断的实例，需要过滤被熔断的实例
            healthyInstance = instances.getInstances().stream().filter(
                    new Predicate<Instance>() {
                        @Override
                        public boolean test(Instance instance) {
                            if (!Utils.isHealthyInstance(instance)) {
                                return false;
                            }
                            return checkCircuitBreakerPassing(routeInfo, instance);
                        }
                    }
            )
                    .collect(Collectors.toList());
        } else {
            //只过滤不健康的实例
            healthyInstance = instances.getInstances().stream().filter(Utils::isHealthyInstance)
                    .collect(Collectors.toList());
        }

        int healthyInstanceCount = healthyInstance.size();
        //如果过滤之后，没有实例，则返回全量的实例。推空保护
        if (healthyInstanceCount == 0) {
            return new RouteResult(instances.getInstances(), RouteResult.State.Next);
        }

        return new RouteResult(healthyInstance, RouteResult.State.Next);
    }

    private boolean isExcludeCircuitBreakInstances(RouteInfo routeInfo) {
        if (routeInfo.isIncludeCircuitBreakInstances()) {
            return false;
        }
        return recoverRouterConfig.isExcludeCircuitBreakInstances();
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVICE_ROUTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        this.recoverRouterConfig = ctx.getConfig().getConsumer().getServiceRouter()
                .getPluginConfig(getName(), RecoverRouterConfig.class);
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return RecoverRouterConfig.class;
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

    private boolean checkCircuitBreakerPassing(RouteInfo routeInfo, Instance instance) {
        CircuitBreakerStatus circuitBreakerStatus = instance.getCircuitBreakerStatus();
        if (null != circuitBreakerStatus) {
            return circuitBreakerStatus.getStatus() != CircuitBreakerStatus.Status.OPEN;
        }
        ServiceKey sourceService = null;
        if (null != routeInfo.getSourceService()) {
            sourceService = routeInfo.getSourceService().getServiceKey();
        }
        Resource resource = new InstanceResource(new ServiceKey(instance.getNamespace(), instance.getService()),
                instance.getHost(), instance.getPort(), sourceService);
        CheckResult check = DefaultCircuitBreakAPI.check(resource, extensions);
        return check.isPass();
    }
}
