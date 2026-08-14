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
 * Test for {@link ServiceInfo}.
 *
 * @author fishtailfu
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceInfoTest {

    /**
     * 测试默认服务类型
     * 测试目的：验证未设置时默认为 SERVICE_TYPE_MICROSERVICE
     * 测试场景：直接 new ServiceInfo
     * 验证内容：getServiceType() 返回 SERVICE_TYPE_MICROSERVICE
     */
    @Test
    public void testDefaultServiceType() {
        // Arrange
        ServiceInfo serviceInfo = new ServiceInfo();

        // Act & Assert
        assertThat(serviceInfo.getServiceType())
                .isEqualTo(ServiceProto.ServiceType.SERVICE_TYPE_MICROSERVICE);
    }

    /**
     * 测试 Builder 设置服务类型
     * 测试目的：验证 builder 能正确写入 SERVICE_TYPE_AI_AGENT
     * 测试场景：通过 builder 构建 ServiceInfo
     * 验证内容：getServiceType() 返回 SERVICE_TYPE_AI_AGENT，toString 包含 serviceType
     */
    @Test
    public void testBuilderServiceType() {
        // Arrange & Act
        ServiceInfo serviceInfo = ServiceInfo.builder()
                .namespace("default")
                .service("ai-agent-demo")
                .serviceType(ServiceProto.ServiceType.SERVICE_TYPE_AI_AGENT)
                .build();

        // Assert
        assertThat(serviceInfo.getServiceType())
                .isEqualTo(ServiceProto.ServiceType.SERVICE_TYPE_AI_AGENT);
        assertThat(serviceInfo.toString()).contains("serviceType=SERVICE_TYPE_AI_AGENT");
    }

    /**
     * 测试 setter 设置服务类型
     * 测试目的：验证 setServiceType 生效
     * 测试场景：设置为 SERVICE_TYPE_MCP_SERVER
     * 验证内容：getServiceType() 返回 SERVICE_TYPE_MCP_SERVER
     */
    @Test
    public void testSetServiceType() {
        // Arrange
        ServiceInfo serviceInfo = new ServiceInfo();

        // Act
        serviceInfo.setServiceType(ServiceProto.ServiceType.SERVICE_TYPE_MCP_SERVER);

        // Assert
        assertThat(serviceInfo.getServiceType())
                .isEqualTo(ServiceProto.ServiceType.SERVICE_TYPE_MCP_SERVER);
    }
}
