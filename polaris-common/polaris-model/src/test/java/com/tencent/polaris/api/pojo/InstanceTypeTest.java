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

package com.tencent.polaris.api.pojo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link InstanceType}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class InstanceTypeTest {

    /**
     * 测试枚举值的描述信息
     * 测试目的：验证每个枚举值的 desc 字段正确
     * 测试场景：依次检查 MICROSERVICE、MCP、A2A
     * 验证内容：getDesc() 返回值与预期一致
     */
    @Test
    public void testGetDesc() {
        // Arrange & Act & Assert
        assertThat(InstanceType.MICROSERVICE.getDesc()).isEqualTo("microservice");
        assertThat(InstanceType.MCP.getDesc()).isEqualTo("mcp");
        assertThat(InstanceType.A2A.getDesc()).isEqualTo("a2a");
    }

    /**
     * 测试枚举值的数量和顺序
     * 测试目的：验证枚举包含且仅包含三个值，且 MICROSERVICE 在第一位
     * 测试场景：获取所有枚举值
     * 验证内容：枚举值数量为3，且顺序正确
     */
    @Test
    public void testEnumValues() {
        // Arrange & Act
        InstanceType[] values = InstanceType.values();

        // Assert
        assertThat(values).hasSize(3);
        assertThat(values[0]).isEqualTo(InstanceType.MICROSERVICE);
        assertThat(values[1]).isEqualTo(InstanceType.MCP);
        assertThat(values[2]).isEqualTo(InstanceType.A2A);
    }

    /**
     * 测试枚举值的 valueOf 方法
     * 测试目的：验证字符串转枚举正常工作
     * 测试场景：使用枚举名称字符串进行转换
     * 验证内容：valueOf 返回正确的枚举值
     */
    @Test
    public void testValueOf() {
        // Arrange & Act & Assert
        assertThat(InstanceType.valueOf("MICROSERVICE")).isEqualTo(InstanceType.MICROSERVICE);
        assertThat(InstanceType.valueOf("MCP")).isEqualTo(InstanceType.MCP);
        assertThat(InstanceType.valueOf("A2A")).isEqualTo(InstanceType.A2A);
    }
}
