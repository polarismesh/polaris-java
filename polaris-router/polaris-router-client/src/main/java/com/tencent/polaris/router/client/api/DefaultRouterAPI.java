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

package com.tencent.polaris.router.client.api;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.flow.RouterFlow;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceResponse;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import com.tencent.polaris.router.client.util.RouterValidator;

/**
 * 默认引擎API
 */
public class DefaultRouterAPI extends BaseEngine implements RouterAPI {

    private final RouterFlow routerFlow;

    public DefaultRouterAPI(SDKContext context) {
        super(context);
        routerFlow = RouterFlow.loadRouterFlow(context.getConfig().getGlobal().getSystem().getFlow().getName());
    }

    @Override
    protected void subInit() {
        routerFlow.setSDKContext(sdkContext);
    }

    @Override
    public ProcessRoutersResponse processRouters(ProcessRoutersRequest request) throws PolarisException {
        checkAvailable("RouterAPI");
        RouterValidator.validateProcessRouterRequest(request);
        return routerFlow.processRouters(request);
    }

    @Override
    public ProcessLoadBalanceResponse processLoadBalance(ProcessLoadBalanceRequest request) throws PolarisException {
        checkAvailable("RouterAPI");
        RouterValidator.validateProcessLoadBalanceRequest(request);
        return routerFlow.processLoadBalance(request);
    }

}
