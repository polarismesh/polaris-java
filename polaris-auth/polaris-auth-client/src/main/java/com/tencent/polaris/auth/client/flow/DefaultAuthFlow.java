/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.auth.client.flow;

import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.config.provider.AuthConfig;
import com.tencent.polaris.api.plugin.auth.AuthInfo;
import com.tencent.polaris.api.plugin.auth.AuthResult;
import com.tencent.polaris.api.plugin.auth.Authenticator;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.auth.api.flow.AuthFlow;
import com.tencent.polaris.auth.api.rpc.AuthRequest;
import com.tencent.polaris.auth.api.rpc.AuthResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/**
 * 默认的鉴权Flow实现
 *
 * @author Haotian Zhang
 */
public class DefaultAuthFlow implements AuthFlow {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthFlow.class);

    private SDKContext sdkContext;

    private AuthConfig authConfig;

    private Extensions extensions;

    private List<Authenticator> authenticatorList;

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
        this.authConfig = sdkContext.getConfig().getProvider().getAuth();
        this.extensions = sdkContext.getExtensions();
        this.authenticatorList = extensions.getAuthenticatorList();
    }

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        if (authConfig == null || !authConfig.isEnable()) {
            return new AuthResponse(new AuthResult(AuthResult.Code.AuthResultOk));
        }
        AuthInfo authInfo = new AuthInfo(authRequest.getNamespace(), authRequest.getService(), authRequest.getPath(),
                authRequest.getProtocol(), authRequest.getMethod(), authRequest.getMetadataContext());
        AuthResponse authResponse = new AuthResponse(new AuthResult(AuthResult.Code.AuthResultOk));
        for (Authenticator authenticator : authenticatorList) {
            AuthResult authResult = authenticator.authenticate(authInfo);
            if (authResult.getCode().equals(AuthResult.Code.AuthResultForbidden)) {
                return new AuthResponse(authResult);
            }
        }
        return authResponse;
    }
}
