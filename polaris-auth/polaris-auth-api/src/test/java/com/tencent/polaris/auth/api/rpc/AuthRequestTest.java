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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AuthRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthRequestTest {

    /**
     * 测试构造函数保存所有字段
     * 测试目的：验证 AuthRequest 构造函数正确保存各字段
     * 测试场景：传入命名空间、服务、路径、协议、方法构造 AuthRequest
     * 验证内容：各 getter 返回值与入参一致
     */
    @Test
    public void testConstructorSavesAllFields() {
        // Act
        AuthRequest request = new AuthRequest("Test", "svc", "/api/v1", "http", "GET", null);

        // Assert
        assertThat(request.getNamespace()).isEqualTo("Test");
        assertThat(request.getService()).isEqualTo("svc");
        assertThat(request.getPath()).isEqualTo("/api/v1");
        assertThat(request.getProtocol()).isEqualTo("http");
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getMetadataContext()).isNull();
    }

    /**
     * 测试 timeoutMs 继承自 RequestBaseEntity
     * 测试目的：验证 AuthRequest 继承的超时属性读写
     * 测试场景：setTimeoutMs 后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testTimeoutMs() {
        // Arrange
        AuthRequest request = new AuthRequest("Test", "svc", "/api", "http", "GET", null);

        // Act
        request.setTimeoutMs(3000L);

        // Assert
        assertThat(request.getTimeoutMs()).isEqualTo(3000L);
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含 namespace/service/method
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含各字段值
     */
    @Test
    public void testToString() {
        // Arrange
        AuthRequest request = new AuthRequest("Test", "svc", "/api", "http", "POST", null);

        // Act
        String str = request.toString();

        // Assert
        assertThat(str).contains("Test");
        assertThat(str).contains("svc");
        assertThat(str).contains("POST");
    }
}
