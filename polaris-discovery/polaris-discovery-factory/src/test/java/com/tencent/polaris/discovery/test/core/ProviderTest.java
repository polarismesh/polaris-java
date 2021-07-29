/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.test.common.Consts;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ProviderTest {

    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(10081);
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
        try (ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPIByConfig(configuration)) {
            for (int i = 0; i < 5; i++) {
                namingServer.getNamingService().addService(new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER));
                //注册
                InstanceRegisterRequest registerRequest = new InstanceRegisterRequest();
                registerRequest.setNamespace(NAMESPACE_TEST);
                registerRequest.setService(SERVICE_PROVIDER);
                registerRequest.setHost(Consts.HOST);
                registerRequest.setPort(Consts.PORT);
                registerRequest.setProtocol("http");
                registerRequest.setToken(Consts.PROVIDER_TOKEN);
                registerRequest.setTtl(5);
                InstanceRegisterResponse response = providerAPI.register(registerRequest);
                Assert.assertTrue(StringUtils.isNotBlank(response.getInstanceId()));
                Assert.assertFalse(response.isExists());
                //再注册
                response = providerAPI.register(registerRequest);
                Assert.assertTrue(StringUtils.isNotBlank(response.getInstanceId()));
                Assert.assertTrue(response.isExists());
                Utils.sleepUninterrupted(5000);
                //心跳上报
                InstanceHeartbeatRequest heartbeatRequest = new InstanceHeartbeatRequest();
                heartbeatRequest.setNamespace(NAMESPACE_TEST);
                heartbeatRequest.setService(SERVICE_PROVIDER);
                heartbeatRequest.setHost(Consts.HOST);
                heartbeatRequest.setPort(Consts.PORT);
                heartbeatRequest.setToken(Consts.PROVIDER_TOKEN);
                providerAPI.heartbeat(heartbeatRequest);
                //反注册
                InstanceDeregisterRequest deregisterRequest = new InstanceDeregisterRequest();
                deregisterRequest.setNamespace(NAMESPACE_TEST);
                deregisterRequest.setService(SERVICE_PROVIDER);
                deregisterRequest.setHost(Consts.HOST);
                deregisterRequest.setPort(Consts.PORT);
                deregisterRequest.setToken(Consts.PROVIDER_TOKEN);
                providerAPI.deRegister(deregisterRequest);
            }
        }
    }
}
