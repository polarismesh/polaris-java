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

package com.tencent.polaris.discovery.client.api;

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.flow.DiscoveryFlow;
import com.tencent.polaris.api.plugin.server.ReportServiceContractRequest;
import com.tencent.polaris.api.plugin.server.ReportServiceContractResponse;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.client.flow.RegisterStateManager;
import com.tencent.polaris.discovery.client.util.Validator;

/**
 * ProviderAPI的标准实现
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class DefaultProviderAPI extends BaseEngine implements ProviderAPI {

    private DiscoveryFlow discoveryFlow;

    public DefaultProviderAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() {
        discoveryFlow = sdkContext.getOrInitFlow(DiscoveryFlow.class);
    }

    @Override
    public InstanceRegisterResponse registerInstance(InstanceRegisterRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateInstanceRegisterRequest(req);
        req.setAutoHeartbeat(true);
        return discoveryFlow.register(req);
    }

    @Override
    protected void doDestroy() {
        RegisterStateManager.destroy(sdkContext);
        super.doDestroy();
        if (discoveryFlow != null) {
            discoveryFlow.destroy();
        }
    }

    @Override
    public InstanceRegisterResponse register(InstanceRegisterRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateInstanceRegisterRequest(req);
        return discoveryFlow.register(req);
    }

    @Override
    public void deRegister(InstanceDeregisterRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateInstanceDeregisterRequest(req);
        discoveryFlow.deRegister(req);
    }

    @Override
    public void heartbeat(InstanceHeartbeatRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateHeartbeatRequest(req);
        discoveryFlow.heartbeat(req);
    }

    @Override
    public ReportServiceContractResponse reportServiceContract(ReportServiceContractRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateReportServiceContractRequest(req);
        return discoveryFlow.reportServiceContract(req);
    }
}
