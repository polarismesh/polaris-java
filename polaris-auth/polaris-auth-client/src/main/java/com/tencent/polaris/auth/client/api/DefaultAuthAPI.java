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

package com.tencent.polaris.auth.client.api;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.auth.api.core.AuthAPI;
import com.tencent.polaris.auth.api.flow.AuthFlow;
import com.tencent.polaris.auth.api.rpc.AuthRequest;
import com.tencent.polaris.auth.api.rpc.AuthResponse;
import com.tencent.polaris.auth.client.utils.AuthValidator;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;

/**
 * 默认的鉴权API实现
 *
 * @author Haotian Zhang
 */
public class DefaultAuthAPI extends BaseEngine implements AuthAPI {

    private AuthFlow authFlow;

    public DefaultAuthAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() {
        authFlow = sdkContext.getOrInitFlow(AuthFlow.class);
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) throws PolarisException {
        checkAvailable("AuthFlow");
        AuthValidator.validateAuthRequest(authRequest);
        return authFlow.authenticate(authRequest);
    }
}
