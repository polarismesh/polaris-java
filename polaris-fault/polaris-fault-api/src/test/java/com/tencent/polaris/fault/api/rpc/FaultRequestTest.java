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

package com.tencent.polaris.fault.api.rpc;

import com.tencent.polaris.api.pojo.ServiceKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FaultRequest}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class FaultRequestTest {

    /**
     * 测试构造函数正确创建 sourceService 和 targetService
     * 测试目的：验证构造函数将命名空间+服务名转换为 ServiceKey
     * 测试场景：传入源/目标命名空间与服务名构造 FaultRequest
     * 验证内容：sourceService/targetService 的 namespace/service 与入参一致
     */
    @Test
    public void testConstructorCreatesServiceKeys() {
        // Act
        FaultRequest request = new FaultRequest("Prod", "caller", "Test", "target", null);

        // Assert
        ServiceKey sourceService = request.getSourceService();
        assertThat(sourceService.getNamespace()).isEqualTo("Prod");
        assertThat(sourceService.getService()).isEqualTo("caller");
        ServiceKey targetService = request.getTargetService();
        assertThat(targetService.getNamespace()).isEqualTo("Test");
        assertThat(targetService.getService()).isEqualTo("target");
    }

    /**
     * 测试 metadataContext 的读取
     * 测试目的：验证 metadataContext 传入 null 时 getMetadataContext 返回 null
     * 测试场景：构造时 metadataContext 传 null
     * 验证内容：getMetadataContext 为 null
     */
    @Test
    public void testGetMetadataContext() {
        // Act
        FaultRequest request = new FaultRequest("Prod", "caller", "Test", "target", null);

        // Assert
        assertThat(request.getMetadataContext()).isNull();
    }

    /**
     * 测试 toString 包含源/目标服务
     * 测试目的：验证 toString 输出包含 sourceService 和 targetService
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含源和目标服务名
     */
    @Test
    public void testToString() {
        // Arrange
        FaultRequest request = new FaultRequest("Prod", "caller", "Test", "target", null);

        // Act
        String str = request.toString();

        // Assert
        assertThat(str).contains("caller");
        assertThat(str).contains("target");
    }
}
