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

package com.tencent.polaris.api.plugin.server;

import com.tencent.polaris.specification.api.v1.service.manage.ServiceContractProto;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link ServiceFeature}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceFeatureTest {

    /**
     * 测试所有字段的 getter 和 setter
     * 测试目的：验证 ServiceFeature POJO 的所有字段可以正确设置和读取
     * 测试场景：设置全部字段后逐一验证
     * 验证内容：每个 getter 返回对应 setter 设置的值
     */
    @Test
    public void testGettersAndSetters() {
        // Arrange
        ServiceFeature feature = new ServiceFeature();

        // Act
        feature.setId("feature-001");
        feature.setType(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool);
        feature.setName("get_weather");
        feature.setDescription("Get weather information for a city");
        feature.setContent("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}");
        feature.setStatus(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);

        // Assert
        Assertions.assertThat(feature.getId()).isEqualTo("feature-001");
        Assertions.assertThat(feature.getType()).isEqualTo(ServiceContractProto.ServiceFeatureType.Service_Feature_Type_MCP_Tool);
        Assertions.assertThat(feature.getName()).isEqualTo("get_weather");
        Assertions.assertThat(feature.getDescription()).isEqualTo("Get weather information for a city");
        Assertions.assertThat(feature.getContent()).isEqualTo("{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\"}}}");
        Assertions.assertThat(feature.getStatus()).isEqualTo(ServiceContractProto.ServiceFeatureStatus.Service_Feature_Status_Enabled);
    }

    /**
     * 测试字段默认值为 null
     * 测试目的：验证新建对象所有字段默认值
     * 测试场景：不调用任何 setter
     * 验证内容：所有字段为 null
     */
    @Test
    public void testDefaultValues() {
        // Arrange & Act
        ServiceFeature feature = new ServiceFeature();

        // Assert
        Assertions.assertThat(feature.getId()).isNull();
        Assertions.assertThat(feature.getType()).isNull();
        Assertions.assertThat(feature.getName()).isNull();
        Assertions.assertThat(feature.getDescription()).isNull();
        Assertions.assertThat(feature.getContent()).isNull();
        Assertions.assertThat(feature.getStatus()).isNull();
    }
}
