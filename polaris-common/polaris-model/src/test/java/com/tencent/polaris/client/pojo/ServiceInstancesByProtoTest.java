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
import com.tencent.polaris.api.pojo.ServiceType;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ServiceInstancesByProto}.
 *
 * @author fishtailfu
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceInstancesByProtoTest {

    /**
     * 测试实例发现结果暴露 service_type
     * 测试目的：验证 DiscoverResponse.service.service_type 能透传到 ServiceInstances
     * 测试场景：构造含 SERVICE_TYPE_AI_AGENT 的实例发现响应
     * 验证内容：getServiceType() 返回 AI_AGENT
     */
    @Test
    public void testGetServiceTypeAiAgent() {
        // Arrange
        ServiceProto.Service service = ServiceProto.Service.newBuilder()
                .setNamespace(StringValue.of("default"))
                .setName(StringValue.of("ai-agent-demo"))
                .setRevision(StringValue.of("rev-1"))
                .setServiceType(ServiceProto.ServiceType.SERVICE_TYPE_AI_AGENT)
                .build();
        ResponseProto.DiscoverResponse response = ResponseProto.DiscoverResponse.newBuilder()
                .setService(service)
                .build();

        // Act
        ServiceInstancesByProto serviceInstances = new ServiceInstancesByProto(response, null, false);

        // Assert
        assertThat(serviceInstances.getServiceType()).isEqualTo(ServiceType.AI_AGENT);
        assertThat(serviceInstances.getNamespace()).isEqualTo("default");
        assertThat(serviceInstances.getService()).isEqualTo("ai-agent-demo");
    }

    /**
     * 测试空实例对象的默认服务类型
     * 测试目的：验证 EMPTY_INSTANCES 默认为 MICROSERVICE
     * 测试场景：读取 EMPTY_INSTANCES
     * 验证内容：getServiceType() 返回 MICROSERVICE
     */
    @Test
    public void testEmptyInstancesDefaultServiceType() {
        // Arrange & Act & Assert
        assertThat(ServiceInstancesByProto.EMPTY_INSTANCES.getServiceType())
                .isEqualTo(ServiceType.MICROSERVICE);
    }
}
