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

package com.tencent.polaris.assembly.client.flow;

import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.assembly.api.pojo.AfterRequest;
import com.tencent.polaris.assembly.api.pojo.BeforeRequest;
import com.tencent.polaris.assembly.api.pojo.BeforeResponse;
import com.tencent.polaris.assembly.api.pojo.GetOneInstanceRequest;
import com.tencent.polaris.assembly.api.pojo.ServiceCallResult;
import com.tencent.polaris.assembly.flow.AssemblyFlow;
import com.tencent.polaris.client.api.SDKContext;

public class DefaultAssemblyFlow implements AssemblyFlow {

    private SDKContext sdkContext;

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
    }

    @Override
    public BeforeResponse beforeCallService(BeforeRequest beforeRequest) {
        return BeforeResponse.builder().setRetStatus(RetStatus.RetSuccess).build();
    }

    @Override
    public void afterCallService(AfterRequest afterRequest) {

    }

    @Override
    public BeforeResponse beforeProcess(BeforeRequest beforeRequest) {
        return BeforeResponse.builder().setRetStatus(RetStatus.RetSuccess).build();
    }

    @Override
    public void afterProcess(AfterRequest afterRequest) {

    }

    @Override
    public void initService(ServiceKey serviceKey) {

    }

    @Override
    public Instance getOneInstance(GetOneInstanceRequest request) {
        return null;
    }

    @Override
    public void updateServiceCallResult(ServiceCallResult result) {

    }
}
