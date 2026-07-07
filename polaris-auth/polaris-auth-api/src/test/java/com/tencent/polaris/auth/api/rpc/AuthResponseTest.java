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

package com.tencent.polaris.auth.api.rpc;

import com.tencent.polaris.api.plugin.auth.AuthResult;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AuthResponse}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthResponseTest {

    /**
     * 测试构造函数保存 authResult
     * 测试目的：验证 AuthResponse 正确包装 AuthResult
     * 测试场景：用 AuthResultOk 构造 AuthResponse
     * 验证内容：getAuthResult 返回的 code 为 AuthResultOk
     */
    @Test
    public void testConstructorWithOkResult() {
        // Arrange
        AuthResult authResult = new AuthResult(AuthResult.Code.AuthResultOk);

        // Act
        AuthResponse response = new AuthResponse(authResult);

        // Assert
        assertThat(response.getAuthResult()).isEqualTo(authResult);
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultOk);
    }

    /**
     * 测试构造函数保存 Forbidden 结果
     * 测试目的：验证 AuthResponse 包装禁止访问结果
     * 测试场景：用 AuthResultForbidden 构造 AuthResponse
     * 验证内容：getAuthResult 的 code 为 AuthResultForbidden
     */
    @Test
    public void testConstructorWithForbiddenResult() {
        // Arrange
        AuthResult authResult = new AuthResult(AuthResult.Code.AuthResultForbidden);

        // Act
        AuthResponse response = new AuthResponse(authResult);

        // Assert
        assertThat(response.getAuthResult().getCode()).isEqualTo(AuthResult.Code.AuthResultForbidden);
    }

    /**
     * 测试 toString 包含 authResult
     * 测试目的：验证 toString 输出包含 authResult 信息
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含 AuthResultOk
     */
    @Test
    public void testToString() {
        // Arrange
        AuthResponse response = new AuthResponse(new AuthResult(AuthResult.Code.AuthResultOk));

        // Act
        String str = response.toString();

        // Assert
        assertThat(str).contains("AuthResultOk");
    }
}
