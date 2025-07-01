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

package com.tencent.polaris.circuitbreak.client.api;

import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.flow.CircuitBreakerFlow;
import com.tencent.polaris.circuitbreak.api.pojo.CheckResult;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.CommonValidator;

public class DefaultCircuitBreakAPI extends BaseEngine implements CircuitBreakAPI {

    private CircuitBreakerFlow circuitBreakerFlow;

    public DefaultCircuitBreakAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() {
        circuitBreakerFlow = sdkContext.getOrInitFlow(CircuitBreakerFlow.class);
    }

    @Override
    protected void doDestroy() {
        super.doDestroy();
    }

    @Override
    public CheckResult check(Resource resource) {
        return circuitBreakerFlow.check(resource);
    }

    @Override
    public void report(ResourceStat reportStat) {
        circuitBreakerFlow.report(reportStat);
    }


    @Override
    public FunctionalDecorator makeFunctionalDecorator(FunctionalDecoratorRequest makeDecoratorRequest) {
        CommonValidator.validateService(makeDecoratorRequest.getService());
        CommonValidator.validateNamespaceService(makeDecoratorRequest.getService().getNamespace(),
                makeDecoratorRequest.getService().getService());
        return new DefaultFunctionalDecorator(makeDecoratorRequest, this);
    }

    @Override
    public InvokeHandler makeInvokeHandler(InvokeContext.RequestContext requestContext) {
        CommonValidator.validateService(requestContext.getService());
        CommonValidator.validateNamespaceService(requestContext.getService().getNamespace(),
                requestContext.getService().getService());
        return new DefaultInvokeHandler(requestContext, this);
    }

}
