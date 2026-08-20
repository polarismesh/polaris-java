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
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceInstancesWrap;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link ServiceInstancesByProto}.
 *
 * @author Fishtail
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceInstancesByProtoTest {

    /**
     * 测试 Discover INSTANCE 应答会从 Service proto 取出 extended_metadata。
     * 测试目的：getInstances 路径能拿到当前服务的扩展元数据。
     * 测试场景：应答 service 字段包含一条 AgentSkill。
     * 验证内容：ServiceInstances.getExtendedMetadata 带出 skill id。
     */
    @Test
    public void testGetExtendedMetadataFromServiceProto() {
        // Arrange
        ServiceInstancesByProto serviceInstances = new ServiceInstancesByProto(buildInstanceResponse(), null, false);

        // Act & Assert
        Assertions.assertThat(serviceInstances.getExtendedMetadata()).hasSize(1);
        Assertions.assertThat(serviceInstances.getExtendedMetadata().get(0).getAgentSkill().getId())
                .isEqualTo("skill-weather");
    }

    /**
     * 测试 ServiceInstancesWrap 会委托内部实例的扩展元数据。
     * 测试目的：getInstances 过滤后的 wrap 不丢服务级扩展元数据。
     * 测试场景：用带 skill 的 ServiceInstancesByProto 构造 wrap。
     * 验证内容：wrap.getExtendedMetadata 与原始列表一致。
     */
    @Test
    public void testServiceInstancesWrapDelegatesExtendedMetadata() {
        // Arrange
        ServiceInstancesByProto serviceInstances = new ServiceInstancesByProto(buildInstanceResponse(), null, false);
        ServiceInstances wrap = new ServiceInstancesWrap(serviceInstances, serviceInstances.getInstances(),
                serviceInstances.getTotalWeight());

        // Act & Assert
        Assertions.assertThat(wrap.getExtendedMetadata()).hasSize(1);
        Assertions.assertThat(wrap.getExtendedMetadata().get(0).getAgentSkill().getId())
                .isEqualTo("skill-weather");
    }

    private ResponseProto.DiscoverResponse buildInstanceResponse() {
        ServiceProto.ExtendedMetadata extendedMetadata = ServiceProto.ExtendedMetadata.newBuilder()
                .setType(ServiceProto.ExtendedMetadata.ExtendedMetadataType.EXTENDED_METADATA_SKILL)
                .setAgentSkill(ServiceProto.AgentSkill.newBuilder()
                        .setId("skill-weather")
                        .setName("weather")
                        .build())
                .build();
        ServiceProto.Service service = ServiceProto.Service.newBuilder()
                .setNamespace(StringValue.newBuilder().setValue("default").build())
                .setName(StringValue.newBuilder().setValue("weather-agent").build())
                .setRevision(StringValue.newBuilder().setValue("rev-1").build())
                .addExtendedMetadata(extendedMetadata)
                .build();
        return ResponseProto.DiscoverResponse.newBuilder()
                .setService(service)
                .build();
    }
}
