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

package com.tencent.polaris.assembly.client;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.api.pojo.AfterRequest;
import com.tencent.polaris.assembly.api.pojo.BeforeRequest;
import com.tencent.polaris.assembly.api.pojo.BeforeResponse;
import com.tencent.polaris.assembly.api.pojo.GetOneInstanceRequest;
import com.tencent.polaris.assembly.api.pojo.ServiceCallResult;
import com.tencent.polaris.assembly.flow.AssemblyFlow;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;

public class DefaultAssemblyAPI extends BaseEngine implements AssemblyAPI {

    private final AssemblyFlow assemblyFlow;

    public DefaultAssemblyAPI(SDKContext context) {
        super(context);
        assemblyFlow = AssemblyFlow.loadAssemblyFlow(
                context.getConfig().getGlobal().getSystem().getFlow().getName());
    }

    @Override
    protected void subInit() throws PolarisException {
        assemblyFlow.setSDKContext(sdkContext);
    }

    @Override
    public BeforeResponse beforeCallService(BeforeRequest beforeRequest) {
        return assemblyFlow.beforeCallService(beforeRequest);
    }

    @Override
    public void afterCallService(AfterRequest afterRequest) {
        assemblyFlow.afterCallService(afterRequest);
    }

    @Override
    public BeforeResponse beforeProcess(BeforeRequest beforeRequest) {
        return assemblyFlow.beforeProcess(beforeRequest);
    }

    @Override
    public void afterProcess(AfterRequest afterRequest) {
        assemblyFlow.afterProcess(afterRequest);
    }

    @Override
    public void initService(ServiceKey serviceKey) {
        assemblyFlow.initService(serviceKey);
    }

    @Override
    public Instance getOneInstance(GetOneInstanceRequest request) {
        return assemblyFlow.getOneInstance(request);
    }

    @Override
    public void updateServiceCallResult(ServiceCallResult result) {
        assemblyFlow.updateServiceCallResult(result);
    }
}
