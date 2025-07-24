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

package com.tencent.polaris.fault.client.api;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.fault.api.core.FaultAPI;
import com.tencent.polaris.fault.api.flow.FaultFlow;
import com.tencent.polaris.fault.api.rpc.FaultRequest;
import com.tencent.polaris.fault.api.rpc.FaultResponse;
import com.tencent.polaris.fault.client.utils.FaultValidator;

/**
 * 默认的故障注入API实现
 *
 * @author Haotian Zhang
 */
public class DefaultFaultAPI extends BaseEngine implements FaultAPI {

    private FaultFlow faultFlow;

    public DefaultFaultAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() {
        faultFlow = sdkContext.getOrInitFlow(FaultFlow.class);
    }

    @Override
    public FaultResponse fault(FaultRequest faultRequest) throws PolarisException {
        checkAvailable("FaultFlow");
        FaultValidator.validateFaultRequest(faultRequest);
        return faultFlow.fault(faultRequest);
    }
}
