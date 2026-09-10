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

package com.tencent.polaris.discovery.test.core;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.api.rpc.ServicesResponse;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;

import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GetServicesTest {
    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    @Test
    public void testRoundTrip() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            for (int i = 0; i < 5; i++) {
                namingServer.getNamingService().addService(new ServiceKey(NAMESPACE_TEST, "get_services_test_" + i));
            }

            ServicesResponse response = consumerAPI.getServices(GetServicesRequest.builder().namespace(NAMESPACE_TEST).build());

            Assert.assertFalse(CollectionUtils.isEmpty(response.getServices()));
            Assert.assertEquals(5, response.getServices().size());
        }
    }

    /**
     * 测试指定服务名时走 INSTANCE 发现。
     * 测试目的：命名 getServices 只返回该服务。
     * 测试场景：命名空间下有 5 个服务，只查其中一个。
     * 验证内容：结果只有一条，name/namespace 匹配。
     */
    @Test
    public void testGetNamedServiceUsesInstanceDiscover() {
        // Arrange
        Configuration configuration = TestUtils.configWithEnvAddress();
        try (ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration)) {
            for (int i = 0; i < 5; i++) {
                namingServer.getNamingService().addService(new ServiceKey(NAMESPACE_TEST, "get_services_test_" + i));
            }

            // Act
            ServicesResponse response = consumerAPI.getServices(GetServicesRequest.builder()
                    .namespace(NAMESPACE_TEST)
                    .service("get_services_test_2")
                    .build());

            // Assert
            Assertions.assertThat(response.getServices()).hasSize(1);
            Assertions.assertThat(response.getServices().get(0).getService()).isEqualTo("get_services_test_2");
            Assertions.assertThat(response.getServices().get(0).getNamespace()).isEqualTo(NAMESPACE_TEST);
        }
    }
}
