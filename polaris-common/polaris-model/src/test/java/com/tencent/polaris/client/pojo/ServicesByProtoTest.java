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

package com.tencent.polaris.client.pojo;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ServicesByProto}.
 *
 * @author fishtailfu
 */
@RunWith(MockitoJUnitRunner.class)
public class ServicesByProtoTest {

    /**
     * 测试从 DiscoverResponse 映射 service_type
     * 测试目的：验证 AI Agent 服务类型能透传到 ServiceInfo
     * 测试场景：构造含 SERVICE_TYPE_AI_AGENT 的 DiscoverResponse
     * 验证内容：ServicesByProto 中 ServiceInfo.getServiceType() 为 SERVICE_TYPE_AI_AGENT
     */
    @Test
    public void testMapAiAgentServiceType() {
        // Arrange
        ServiceProto.Service service = ServiceProto.Service.newBuilder()
                .setNamespace(StringValue.of("default"))
                .setName(StringValue.of("ai-agent-demo"))
                .setRevision(StringValue.of("rev-1"))
                .setServiceType(ServiceProto.ServiceType.SERVICE_TYPE_AI_AGENT)
                .build();
        ResponseProto.DiscoverResponse response = ResponseProto.DiscoverResponse.newBuilder()
                .addServices(service)
                .build();

        // Act
        ServicesByProto servicesByProto = new ServicesByProto(response, false);

        // Assert
        assertThat(servicesByProto.getServices()).hasSize(1);
        ServiceInfo serviceInfo = servicesByProto.getServices().get(0);
        assertThat(serviceInfo.getNamespace()).isEqualTo("default");
        assertThat(serviceInfo.getService()).isEqualTo("ai-agent-demo");
        assertThat(serviceInfo.getServiceType())
                .isEqualTo(ServiceProto.ServiceType.SERVICE_TYPE_AI_AGENT);
    }

    /**
     * 测试未设置 service_type 时的默认值
     * 测试目的：验证 proto 默认枚举映射为 SERVICE_TYPE_MICROSERVICE
     * 测试场景：不显式设置 service_type
     * 验证内容：ServiceInfo.getServiceType() 为 SERVICE_TYPE_MICROSERVICE
     */
    @Test
    public void testDefaultServiceTypeWhenUnset() {
        // Arrange
        ServiceProto.Service service = ServiceProto.Service.newBuilder()
                .setNamespace(StringValue.of("default"))
                .setName(StringValue.of("ms-demo"))
                .setRevision(StringValue.of("rev-1"))
                .build();
        ResponseProto.DiscoverResponse response = ResponseProto.DiscoverResponse.newBuilder()
                .addServices(service)
                .build();

        // Act
        ServicesByProto servicesByProto = new ServicesByProto(response, false);

        // Assert
        assertThat(servicesByProto.getServices().get(0).getServiceType())
                .isEqualTo(ServiceProto.ServiceType.SERVICE_TYPE_MICROSERVICE);
    }
}
