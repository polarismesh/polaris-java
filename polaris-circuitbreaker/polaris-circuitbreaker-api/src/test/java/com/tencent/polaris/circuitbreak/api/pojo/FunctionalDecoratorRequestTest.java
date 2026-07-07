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

package com.tencent.polaris.circuitbreak.api.pojo;

import com.tencent.polaris.api.pojo.ServiceKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FunctionalDecoratorRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class FunctionalDecoratorRequestTest {

    /**
     * 测试构造函数继承自 RequestContext
     * 测试目的：验证 FunctionalDecoratorRequest 正确保存父类字段
     * 测试场景：构造 FunctionalDecoratorRequest 并读取 service/method
     * 验证内容：service/method 与入参一致
     */
    @Test
    public void testConstructorInheritsFields() {
        // Arrange
        ServiceKey service = new ServiceKey("Test", "svc");

        // Act
        FunctionalDecoratorRequest request = new FunctionalDecoratorRequest(service, "http", "POST", "/order");

        // Assert
        assertThat(request.getService()).isEqualTo(service);
        assertThat(request.getProtocol()).isEqualTo("http");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/order");
    }

    /**
     * 测试 sourceService 的 setter 继承
     * 测试目的：验证 FunctionalDecoratorRequest 可设置 sourceService
     * 测试场景：构造后设置 sourceService
     * 验证内容：getSourceService 返回设置的值
     */
    @Test
    public void testSetSourceService() {
        // Arrange
        FunctionalDecoratorRequest request = new FunctionalDecoratorRequest(
                new ServiceKey("Test", "svc"), "http", "GET", "/api");
        ServiceKey sourceService = new ServiceKey("Prod", "caller");

        // Act
        request.setSourceService(sourceService);

        // Assert
        assertThat(request.getSourceService()).isEqualTo(sourceService);
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含 service 和 method
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含 method 值
     */
    @Test
    public void testToString() {
        // Arrange
        FunctionalDecoratorRequest request = new FunctionalDecoratorRequest(
                new ServiceKey("Test", "svc"), "http", "PUT", "/update");

        // Act
        String str = request.toString();

        // Assert
        assertThat(str).contains("PUT");
        assertThat(str).contains("FunctionalDecoratorRequest");
    }
}
