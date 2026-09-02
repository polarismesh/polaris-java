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

package com.tencent.polaris.discovery.client.flow;

import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test for {@link CommonServicesRequest}.
 *
 * @author Fishtail
 */
@RunWith(MockitoJUnitRunner.class)
public class CommonServicesRequestTest {

    /**
     * 测试未指定服务名时仍走 SERVICES。
     * 测试目的：命名空间列表查询保持原缓存键。
     * 测试场景：只填 namespace。
     * 验证内容：eventType=SERVICE，service 为空。
     */
    @Test
    public void testBuildSvcEventKeyUsesServicesWhenNameBlank() {
        // Arrange
        GetServicesRequest request = GetServicesRequest.builder().namespace("default").build();

        // Act
        ServiceEventKey eventKey = CommonServicesRequest.buildSvcEventKey(request);

        // Assert
        Assertions.assertThat(eventKey.getEventType()).isEqualTo(EventType.SERVICE);
        Assertions.assertThat(eventKey.getNamespace()).isEqualTo("default");
        Assertions.assertThat(eventKey.getService()).isEmpty();
    }

    /**
     * 测试指定服务名时改走 INSTANCE。
     * 测试目的：单服务查询用服务级 revision，并能带出 extended_metadata。
     * 测试场景：namespace + service 都有值。
     * 验证内容：eventType=INSTANCE，key 绑定到该服务。
     */
    @Test
    public void testBuildSvcEventKeyUsesInstanceWhenNamePresent() {
        // Arrange
        GetServicesRequest request = GetServicesRequest.builder()
                .namespace("default")
                .service("weather-agent")
                .build();

        // Act
        ServiceEventKey eventKey = CommonServicesRequest.buildSvcEventKey(request);

        // Assert
        Assertions.assertThat(eventKey.getEventType()).isEqualTo(EventType.INSTANCE);
        Assertions.assertThat(eventKey.getNamespace()).isEqualTo("default");
        Assertions.assertThat(eventKey.getService()).isEqualTo("weather-agent");
    }
}
