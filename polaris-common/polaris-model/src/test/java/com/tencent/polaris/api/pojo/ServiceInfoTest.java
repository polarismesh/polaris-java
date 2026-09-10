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
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link ServiceInfo}.
 *
 * @author Fishtail
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceInfoTest {

    /**
     * 测试 builder 能带上服务扩展元数据。
     * 测试目的：验证 ServiceInfo 可通过 builder 暴露 extended_metadata。
     * 测试场景：构造包含 AgentSkill 的 ExtendedMetadata 并构建 ServiceInfo。
     * 验证内容：getExtendedMetadata 返回同一条 skill 元数据。
     */
    @Test
    public void testBuilderKeepsExtendedMetadata() {
        // Arrange
        ServiceProto.AgentSkill skill = ServiceProto.AgentSkill.newBuilder()
                .setId("skill-weather")
                .setName("weather")
                .setDescription("Query weather")
                .addTags("weather")
                .build();
        ServiceProto.ExtendedMetadata extendedMetadata = ServiceProto.ExtendedMetadata.newBuilder()
                .setType(ServiceProto.ExtendedMetadata.ExtendedMetadataType.EXTENDED_METADATA_SKILL)
                .setAgentSkill(skill)
                .build();

        // Act
        ServiceInfo serviceInfo = ServiceInfo.builder()
                .namespace("default")
                .service("weather-agent")
                .extendedMetadata(Collections.singletonList(extendedMetadata))
                .build();

        // Assert
        Assertions.assertThat(serviceInfo.getExtendedMetadata()).hasSize(1);
        Assertions.assertThat(serviceInfo.getExtendedMetadata().get(0).getAgentSkill().getId())
                .isEqualTo("skill-weather");
    }

    /**
     * 测试未设置扩展元数据时返回空列表。
     * 测试目的：避免调用方处理 null。
     * 测试场景：只设置服务名构建 ServiceInfo。
     * 验证内容：getExtendedMetadata 为空列表且非 null。
     */
    @Test
    public void testGetExtendedMetadataReturnsEmptyWhenUnset() {
        // Arrange
        ServiceInfo serviceInfo = ServiceInfo.builder()
                .namespace("default")
                .service("weather-agent")
                .build();

        // Act
        List<ServiceProto.ExtendedMetadata> extendedMetadata = serviceInfo.getExtendedMetadata();

        // Assert
        Assertions.assertThat(extendedMetadata).isNotNull();
        Assertions.assertThat(extendedMetadata).isEmpty();
    }
}
