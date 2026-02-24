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
import com.tencent.polaris.api.plugin.auth.AuthResult;
import com.tencent.polaris.auth.api.flow.AuthFlow;
import com.tencent.polaris.auth.api.rpc.AuthRequest;
import com.tencent.polaris.auth.api.rpc.AuthResponse;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultAuthAPI}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultAuthAPITest {

    @Mock
    private SDKContext sdkContext;

    @Mock
    private AuthFlow authFlow;

    private DefaultAuthAPI defaultAuthAPI;

    @Before
    public void setUp() {
        when(sdkContext.getOrInitFlow(AuthFlow.class)).thenReturn(authFlow);
        defaultAuthAPI = new DefaultAuthAPI(sdkContext);
        defaultAuthAPI.subInit();
    }

    @Test
    public void testAuthenticate_Success() {
        // 准备：创建鉴权请求和成功的响应
        AuthRequest authRequest = new AuthRequest("test-namespace", "test-service", "/api/test", "HTTP", "GET", null);
        AuthResponse expectedResponse = new AuthResponse(new AuthResult(AuthResult.Code.AuthResultOk));
        
        when(authFlow.authenticate(authRequest)).thenReturn(expectedResponse);

        // 执行
        AuthResponse actualResponse = defaultAuthAPI.authenticate(authRequest);

        // 验证
        assertThat(actualResponse).isEqualTo(expectedResponse);
    }

    @Test
    public void testAuthenticate_WithForbiddenResult() {
        // 准备：创建鉴权请求和被拒绝的响应
        AuthRequest authRequest = new AuthRequest("test-namespace", "test-service", "/api/test", "HTTP", "GET", null);
        AuthResponse expectedResponse = new AuthResponse(new AuthResult(AuthResult.Code.AuthResultForbidden));
        
        when(authFlow.authenticate(authRequest)).thenReturn(expectedResponse);

        // 执行
        AuthResponse actualResponse = defaultAuthAPI.authenticate(authRequest);

        // 验证
        assertThat(actualResponse).isEqualTo(expectedResponse);
        assertThat(actualResponse.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultForbidden);
    }

    @Test
    public void testAuthenticate_WithNullRequest() {
        // 准备：创建null请求
        
        // 执行 & 验证
        assertThatThrownBy(() -> defaultAuthAPI.authenticate(null))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("AuthRequest can not be null");
    }

    @Test
    public void testAuthenticate_WithInvalidNamespace() {
        // 准备：创建无效命名空间的请求
        AuthRequest authRequest = new AuthRequest("", "test-service", "/api/test", "HTTP", "GET", null);

        // 执行 & 验证
        assertThatThrownBy(() -> defaultAuthAPI.authenticate(authRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("namespace");
    }

    @Test
    public void testAuthenticate_WithInvalidService() {
        // 准备：创建无效服务名的请求
        AuthRequest authRequest = new AuthRequest("test-namespace", "", "/api/test", "HTTP", "GET", null);

        // 执行 & 验证
        assertThatThrownBy(() -> defaultAuthAPI.authenticate(authRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("service");
    }
}