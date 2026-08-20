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
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link ServicesByProto}.
 *
 * @author Fishtail
 */
@RunWith(MockitoJUnitRunner.class)
public class ServicesByProtoTest {

    /**
     * 测试 Discover SERVICES 应答会把 extended_metadata 映射到 ServiceInfo。
     * 测试目的：getServices 路径能拿到当前服务的扩展元数据。
     * 测试场景：应答中包含一条 AgentSkill 扩展元数据。
     * 验证内容：ServiceInfo.getExtendedMetadata 带出 skill id。
     */
    @Test
    public void testMapExtendedMetadataFromDiscoverResponse() {
        // Arrange
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
        ResponseProto.DiscoverResponse response = ResponseProto.DiscoverResponse.newBuilder()
                .addServices(service)
                .build();

        // Act
        ServicesByProto servicesByProto = new ServicesByProto(response, false);
        ServiceInfo serviceInfo = servicesByProto.getServices().get(0);

        // Assert
        Assertions.assertThat(serviceInfo.getExtendedMetadata()).hasSize(1);
        Assertions.assertThat(serviceInfo.getExtendedMetadata().get(0).getAgentSkill().getId())
                .isEqualTo("skill-weather");
    }
}
