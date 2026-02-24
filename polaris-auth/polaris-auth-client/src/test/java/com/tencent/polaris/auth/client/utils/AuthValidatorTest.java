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

package com.tencent.polaris.auth.client.utils;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.auth.api.rpc.AuthRequest;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link AuthValidator}.
 *
 * @author Haotian Zhang
 */
public class AuthValidatorTest {

    @Test
    public void testValidateAuthRequest_WithValidRequest() {
        // 准备：创建有效的鉴权请求
        AuthRequest authRequest = new AuthRequest("test-namespace", "test-service", "/api/test", "HTTP", "GET", null);

        // 执行 & 验证：不抛出异常
        AuthValidator.validateAuthRequest(authRequest);
    }

    @Test
    public void testValidateAuthRequest_WithNullRequest() {
        // 准备：null请求
        
        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> AuthValidator.validateAuthRequest(null))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("AuthRequest can not be null");
    }

    @Test
    public void testValidateAuthRequest_WithEmptyNamespace() {
        // 准备：空命名空间的请求
        AuthRequest authRequest = new AuthRequest("", "test-service", "/api/test", "HTTP", "GET", null);

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> AuthValidator.validateAuthRequest(authRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("namespace");
    }

    @Test
    public void testValidateAuthRequest_WithNullNamespace() {
        // 准备：null命名空间的请求
        AuthRequest authRequest = new AuthRequest(null, "test-service", "/api/test", "HTTP", "GET", null);

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> AuthValidator.validateAuthRequest(authRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("namespace");
    }

    @Test
    public void testValidateAuthRequest_WithEmptyService() {
        // 准备：空服务名的请求
        AuthRequest authRequest = new AuthRequest("test-namespace", "", "/api/test", "HTTP", "GET", null);

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> AuthValidator.validateAuthRequest(authRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("service");
    }

    @Test
    public void testValidateAuthRequest_WithNullService() {
        // 准备：null服务名的请求
        AuthRequest authRequest = new AuthRequest("test-namespace", null, "/api/test", "HTTP", "GET", null);

        // 执行 & 验证：抛出异常
        assertThatThrownBy(() -> AuthValidator.validateAuthRequest(authRequest))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("service");
    }

    @Test
    public void testValidateAuthRequest_WithValidMetadataContext() {
        // 准备：包含有效metadata context的请求
        MetadataContext metadataContext = new MetadataContext();
        AuthRequest authRequest = new AuthRequest("test-namespace", "test-service", "/api/test", "HTTP", "GET", metadataContext);

        // 执行 & 验证：不抛出异常
        AuthValidator.validateAuthRequest(authRequest);
    }

    @Test
    public void testValidateAuthRequest_WithNullPath() {
        // 准备：null路径的请求
        AuthRequest authRequest = new AuthRequest("test-namespace", "test-service", null, "HTTP", "GET", null);

        // 执行 & 验证：不抛出异常（路径不是必填项）
        AuthValidator.validateAuthRequest(authRequest);
    }

    @Test
    public void testValidateAuthRequest_WithNullProtocol() {
        // 准备：null协议的请求
        AuthRequest authRequest = new AuthRequest("test-namespace", "test-service", "/api/test", null, "GET", null);

        // 执行 & 验证：不抛出异常（协议不是必填项）
        AuthValidator.validateAuthRequest(authRequest);
    }

    @Test
    public void testValidateAuthRequest_WithNullMethod() {
        // 准备：null方法的请求
        AuthRequest authRequest = new AuthRequest("test-namespace", "test-service", "/api/test", "HTTP", null, null);

        // 执行 & 验证：不抛出异常（方法不是必填项）
        AuthValidator.validateAuthRequest(authRequest);
    }
}