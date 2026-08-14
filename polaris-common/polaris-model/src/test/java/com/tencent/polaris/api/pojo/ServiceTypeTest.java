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

import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ServiceType}.
 *
 * @author fishtailfu
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceTypeTest {

    /**
     * 测试枚举值的描述信息
     * 测试目的：验证每个枚举值的 desc 字段正确
     * 测试场景：依次检查 MICROSERVICE、MCP_SERVER、AI_AGENT
     * 验证内容：getDesc() 返回值与预期一致
     */
    @Test
    public void testGetDesc() {
        // Arrange & Act & Assert
        assertThat(ServiceType.MICROSERVICE.getDesc()).isEqualTo("microservice");
        assertThat(ServiceType.MCP_SERVER.getDesc()).isEqualTo("mcp_server");
        assertThat(ServiceType.AI_AGENT.getDesc()).isEqualTo("ai_agent");
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
        ServiceType[] values = ServiceType.values();

        // Assert
        assertThat(values).hasSize(3);
        assertThat(values[0]).isEqualTo(ServiceType.MICROSERVICE);
        assertThat(values[1]).isEqualTo(ServiceType.MCP_SERVER);
        assertThat(values[2]).isEqualTo(ServiceType.AI_AGENT);
    }

    /**
     * 测试 fromProto 映射
     * 测试目的：验证 specification ServiceType 能正确转换为 SDK ServiceType
     * 测试场景：覆盖 MICROSERVICE、MCP_SERVER、AI_AGENT 以及 null
     * 验证内容：映射结果与预期一致，null 默认 MICROSERVICE
     */
    @Test
    public void testFromProto() {
        // Arrange & Act & Assert
        assertThat(ServiceType.fromProto(ServiceProto.ServiceType.SERVICE_TYPE_MICROSERVICE))
                .isEqualTo(ServiceType.MICROSERVICE);
        assertThat(ServiceType.fromProto(ServiceProto.ServiceType.SERVICE_TYPE_MCP_SERVER))
                .isEqualTo(ServiceType.MCP_SERVER);
        assertThat(ServiceType.fromProto(ServiceProto.ServiceType.SERVICE_TYPE_AI_AGENT))
                .isEqualTo(ServiceType.AI_AGENT);
        assertThat(ServiceType.fromProto(null)).isEqualTo(ServiceType.MICROSERVICE);
    }
}
