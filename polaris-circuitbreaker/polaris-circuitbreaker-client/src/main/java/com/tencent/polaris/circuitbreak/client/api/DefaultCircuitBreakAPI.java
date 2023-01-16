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

package com.tencent.polaris.circuitbreak.client.api;

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.Status;
import com.tencent.polaris.api.pojo.HalfOpenStatus;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.CheckResult;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.api.ServiceCallResultListener;
import com.tencent.polaris.client.util.CommonValidator;
import java.util.List;

public class DefaultCircuitBreakAPI extends BaseEngine implements CircuitBreakAPI {

    private ServiceCallResultChecker checker;

    public DefaultCircuitBreakAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() {
        CircuitBreakerConfig cbConfig = sdkContext.getConfig().getConsumer().getCircuitBreaker();
        if (!cbConfig.isEnable()) {
            return;
        }
        List<ServiceCallResultListener> serviceCallResultListeners = ServiceCallResultListener
                .getServiceCallResultListeners(sdkContext);
        for (ServiceCallResultListener listener : serviceCallResultListeners) {
            if (listener instanceof ServiceCallResultChecker) {
                checker = (ServiceCallResultChecker) listener;
                break;
            }
        }
    }

    @Override
    protected void doDestroy() {
        if (null != checker) {
            checker.destroy();
        }
        super.doDestroy();
    }

    @Override
    public CheckResult check(Resource resource) {
        return check(resource, sdkContext.getExtensions());
    }

    public static CheckResult check(Resource resource, Extensions extensions) {
        CircuitBreaker circuitBreaker = extensions.getResourceBreaker();
        if (null == circuitBreaker) {
            return new CheckResult(true, "", null);
        }
        CircuitBreakerStatus circuitBreakerStatus = circuitBreaker.checkResource(resource);
        if (null != circuitBreakerStatus) {
            return circuitBreakerStatusToResult(circuitBreakerStatus);
        }
        return new CheckResult(true, "", null);
    }

    private static CheckResult circuitBreakerStatusToResult(CircuitBreakerStatus circuitBreakerStatus) {
        Status status = circuitBreakerStatus.getStatus();
        if (status == Status.CLOSE) {
            return new CheckResult(true, circuitBreakerStatus.getCircuitBreaker(),
                    circuitBreakerStatus.getFallbackInfo());
        }
        if (status == Status.OPEN) {
            return new CheckResult(false, circuitBreakerStatus.getCircuitBreaker(),
                    circuitBreakerStatus.getFallbackInfo());
        }
        HalfOpenStatus halfOpenStatus = (HalfOpenStatus) circuitBreakerStatus;
        boolean allocated = halfOpenStatus.allocate();
        return new CheckResult(allocated, circuitBreakerStatus.getCircuitBreaker(),
                circuitBreakerStatus.getFallbackInfo());
    }

    @Override
    public void report(ResourceStat reportStat) {
        report(reportStat, sdkContext.getExtensions());
    }

    public static void report(ResourceStat reportStat, Extensions extensions) {
        CircuitBreaker circuitBreaker = extensions.getResourceBreaker();
        if (null == circuitBreaker) {
            return;
        }
        circuitBreaker.report(reportStat);
    }

    @Override
    public FunctionalDecorator makeFunctionalDecorator(FunctionalDecoratorRequest makeDecoratorRequest) {
        CommonValidator.validateService(makeDecoratorRequest.getService());
        CommonValidator.validateNamespaceService(makeDecoratorRequest.getService().getNamespace(),
                makeDecoratorRequest.getService().getService());
        return new DefaultFunctionalDecorator(makeDecoratorRequest, this);
    }
}
