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

package com.tencent.polaris.assembly.client;

import java.util.List;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.assembly.api.AssemblyAPI;
import com.tencent.polaris.assembly.api.pojo.GetOneInstanceRequest;
import com.tencent.polaris.assembly.api.pojo.GetReachableInstancesRequest;
import com.tencent.polaris.assembly.api.pojo.TraceAttributes;
import com.tencent.polaris.assembly.flow.AssemblyFlow;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

public class DefaultAssemblyAPI extends BaseEngine implements AssemblyAPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAssemblyAPI.class);

    private AssemblyFlow assemblyFlow;

    public DefaultAssemblyAPI(SDKContext context) {
        super(context);
    }

    @Override
    protected void subInit() throws PolarisException {
        assemblyFlow = sdkContext.getOrInitFlow(AssemblyFlow.class);
    }

    @Override
    public void initService(ServiceKey serviceKey) {
        checkAvailable("AssemblyAPI");
        Validator.validateServiceKey(serviceKey);
        assemblyFlow.initService(serviceKey);
    }

    @Override
    public List<Instance> getReachableInstances(GetReachableInstancesRequest request) {
        checkAvailable("AssemblyAPI");
        Validator.validateGetReachableInstancesRequest(request);
        return assemblyFlow.getReachableInstances(request);
    }

    @Override
    public Instance getOneInstance(GetOneInstanceRequest request) {
        checkAvailable("AssemblyAPI");
        Validator.validateGetOneInstanceRequest(request);
        return assemblyFlow.getOneInstance(request);
    }

    @Override
    public void updateServiceCallResult(ServiceCallResult result) {
        if (!checkAvailable("AssemblyAPI", false)) {
            return;
        }
        String errMsg = Validator.validateServiceCallResult(result);
        if (null != errMsg) {
            LOGGER.error("fail to validate updateServiceCallResult: " + errMsg);
            return;
        }
        assemblyFlow.updateServiceCallResult(result);
    }

    @Override
    public void updateTraceAttributes(TraceAttributes traceAttributes) {
        if (!checkAvailable("AssemblyAPI", false)) {
            return;
        }
        if (CollectionUtils.isEmpty(traceAttributes.getAttributes())) {
            return;
        }
        assemblyFlow.updateTraceAttributes(traceAttributes);
    }
}
