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

package com.tencent.polaris.auth.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.config.provider.AuthConfig;
import com.tencent.polaris.api.config.provider.ProviderConfig;
import com.tencent.polaris.api.plugin.auth.AuthInfo;
import com.tencent.polaris.api.plugin.auth.AuthResult;
import com.tencent.polaris.api.plugin.auth.Authenticator;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.auth.api.rpc.AuthRequest;
import com.tencent.polaris.auth.api.rpc.AuthResponse;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultAuthFlow}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultAuthFlowTest {

    @Mock
    private SDKContext sdkContext;

    @Mock
    private Configuration configuration;

    @Mock
    private ProviderConfig providerConfig;

    @Mock
    private AuthConfig authConfig;

    @Mock
    private Extensions extensions;

    @Mock
    private Authenticator authenticator1;

    @Mock
    private Authenticator authenticator2;

    private DefaultAuthFlow defaultAuthFlow;

    @Before
    public void setUp() {
        // 构建 mock 链：sdkContext -> configuration -> providerConfig -> authConfig
        when(sdkContext.getConfig()).thenReturn(configuration);
        when(configuration.getProvider()).thenReturn(providerConfig);
        when(providerConfig.getAuth()).thenReturn(authConfig);
        when(sdkContext.getExtensions()).thenReturn(extensions);
        when(extensions.getAuthenticatorList()).thenReturn(Collections.emptyList());

        defaultAuthFlow = new DefaultAuthFlow();
        defaultAuthFlow.setSDKContext(sdkContext);
    }

    @Test
    public void testGetName() {
        // 执行 & 验证
        assertThat(defaultAuthFlow.getName()).isEqualTo(FlowConfig.DEFAULT_FLOW_NAME);
    }

    @Test
    public void testAuthenticate_WithAuthDisabled() {
        // 准备：鉴权功能禁用
        when(authConfig.isEnable()).thenReturn(false);

        // 执行
        AuthResponse response = defaultAuthFlow.authenticate(createValidAuthRequest());

        // 验证：返回成功结果
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultOk);
    }

    @Test
    public void testAuthenticate_WithNullAuthConfig() {
        // 准备：authConfig 为 null，需要重新构建 mock 链
        when(providerConfig.getAuth()).thenReturn(null);

        DefaultAuthFlow flowWithNullConfig = new DefaultAuthFlow();
        flowWithNullConfig.setSDKContext(sdkContext);

        // 执行
        AuthResponse response = flowWithNullConfig.authenticate(createValidAuthRequest());

        // 验证：返回成功结果
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultOk);
    }

    @Test
    public void testAuthenticate_WithAllAuthenticatorsPass() {
        // 准备：鉴权功能启用，所有认证器都通过
        when(authConfig.isEnable()).thenReturn(true);
        when(extensions.getAuthenticatorList()).thenReturn(Arrays.asList(authenticator1, authenticator2));
        // 重新 setSDKContext 以更新 authenticatorList
        defaultAuthFlow.setSDKContext(sdkContext);
        when(authenticator1.authenticate(any(AuthInfo.class))).thenReturn(new AuthResult(AuthResult.Code.AuthResultOk));
        when(authenticator2.authenticate(any(AuthInfo.class))).thenReturn(new AuthResult(AuthResult.Code.AuthResultOk));

        // 执行
        AuthResponse response = defaultAuthFlow.authenticate(createValidAuthRequest());

        // 验证：返回成功结果
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultOk);
    }

    @Test
    public void testAuthenticate_WithFirstAuthenticatorForbidden() {
        // 准备：第一个认证器拒绝访问
        when(authConfig.isEnable()).thenReturn(true);
        when(extensions.getAuthenticatorList()).thenReturn(Arrays.asList(authenticator1, authenticator2));
        defaultAuthFlow.setSDKContext(sdkContext);
        when(authenticator1.authenticate(any(AuthInfo.class))).thenReturn(new AuthResult(AuthResult.Code.AuthResultForbidden));

        // 执行
        AuthResponse response = defaultAuthFlow.authenticate(createValidAuthRequest());

        // 验证：返回拒绝结果，第二个认证器不会被调用
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultForbidden);
    }

    @Test
    public void testAuthenticate_WithSecondAuthenticatorForbidden() {
        // 准备：第二个认证器拒绝访问
        when(authConfig.isEnable()).thenReturn(true);
        when(extensions.getAuthenticatorList()).thenReturn(Arrays.asList(authenticator1, authenticator2));
        defaultAuthFlow.setSDKContext(sdkContext);
        when(authenticator1.authenticate(any(AuthInfo.class))).thenReturn(new AuthResult(AuthResult.Code.AuthResultOk));
        when(authenticator2.authenticate(any(AuthInfo.class))).thenReturn(new AuthResult(AuthResult.Code.AuthResultForbidden));

        // 执行
        AuthResponse response = defaultAuthFlow.authenticate(createValidAuthRequest());

        // 验证：返回拒绝结果
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultForbidden);
    }

    @Test
    public void testAuthenticate_WithEmptyAuthenticatorList() {
        // 准备：空认证器列表，鉴权功能启用
        when(authConfig.isEnable()).thenReturn(true);
        when(extensions.getAuthenticatorList()).thenReturn(Collections.emptyList());
        defaultAuthFlow.setSDKContext(sdkContext);

        // 执行
        AuthResponse response = defaultAuthFlow.authenticate(createValidAuthRequest());

        // 验证：返回成功结果
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultOk);
    }

    @Test
    public void testAuthenticate_WithSingleAuthenticatorPass() {
        // 准备：单个认证器通过
        when(authConfig.isEnable()).thenReturn(true);
        when(extensions.getAuthenticatorList()).thenReturn(Collections.singletonList(authenticator1));
        defaultAuthFlow.setSDKContext(sdkContext);
        when(authenticator1.authenticate(any(AuthInfo.class))).thenReturn(new AuthResult(AuthResult.Code.AuthResultOk));

        // 执行
        AuthResponse response = defaultAuthFlow.authenticate(createValidAuthRequest());

        // 验证：返回成功结果
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultOk);
    }

    private AuthRequest createValidAuthRequest() {
        return new AuthRequest("test-namespace", "test-service", "/api/test", "HTTP", "GET", null);
    }
}