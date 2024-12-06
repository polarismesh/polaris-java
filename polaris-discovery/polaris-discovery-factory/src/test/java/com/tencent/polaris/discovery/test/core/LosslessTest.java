/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.LosslessAPI;
import com.tencent.polaris.api.plugin.lossless.LosslessPolicy;
import com.tencent.polaris.api.plugin.lossless.RegisterStatus;
import com.tencent.polaris.api.pojo.DefaultBaseInstance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.provider.LosslessConfigImpl;
import com.tencent.polaris.test.common.HttpInvokeUtils;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;

public class LosslessTest {

    private NamingServer namingServer;

    private final ServiceKey serviceKey = new ServiceKey("test", "TestSvc");

    private final Node node = new Node("127.0.0.1", 8888);

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
    public void testLosslessRegisterWithoutHealthCheck() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
        ((LosslessConfigImpl) configurationImpl.getProvider().getLossless()).setEnable(true);
        configurationImpl.getGlobal().getAdmin().setPort(38080);
        try (SDKContext sdkContext = SDKContext.initContextByConfig(configuration)) {
            LosslessAPI losslessAPI = DiscoveryAPIFactory.createLosslessAPIByContext(sdkContext);
            DemoLosslessActionProvider demoLosslessActionProvider =
                    new DemoLosslessActionProvider(sdkContext, serviceKey, node, false);
            DefaultBaseInstance defaultBaseInstance = new DefaultBaseInstance();
            defaultBaseInstance.setNamespace(serviceKey.getNamespace());
            defaultBaseInstance.setService(serviceKey.getService());
            defaultBaseInstance.setHost(node.getHost());
            defaultBaseInstance.setPort(node.getPort());
            losslessAPI.setLosslessActionProvider(defaultBaseInstance, demoLosslessActionProvider);
            System.out.println("[testLosslessRegisterWithoutHealthCheck] start to do lossless register");
            losslessAPI.losslessRegister(defaultBaseInstance);
            Utils.sleepUninterrupted(5000);
            System.out.println("[testLosslessRegisterWithoutHealthCheck] start to query status");
            try {
                HttpInvokeUtils.ResponseWrapper wrapper = HttpInvokeUtils.sendRequest("http://127.0.0.1:38080/online", "GET");
                Assert.assertEquals(200, wrapper.getCode());
                Assert.assertEquals(RegisterStatus.REGISTERED.toString(), wrapper.getMessage());
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
            System.out.println("[testLosslessRegisterWithoutHealthCheck] start to do lossless deregister");
            try {
                HttpInvokeUtils.ResponseWrapper wrapper = HttpInvokeUtils.sendRequest("http://127.0.0.1:38080/offline", "PUT");
                Assert.assertEquals(200, wrapper.getCode());
                Assert.assertEquals(LosslessPolicy.REPS_TEXT_OK, wrapper.getMessage());
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
            System.out.println("[testLosslessRegisterWithoutHealthCheck] start to query status");
            try {
                HttpInvokeUtils.ResponseWrapper wrapper = HttpInvokeUtils.sendRequest("http://127.0.0.1:38080/online", "GET");
                Assert.assertEquals(503, wrapper.getCode());
                Assert.assertEquals(RegisterStatus.UNREGISTERED.toString(), wrapper.getMessage());
            } catch (IOException e) {
                Assert.fail("e is " + e.getClass().getCanonicalName() + ", message " + e.getMessage());
            }
        }
    }

    @Test
    public void testLosslessRegisterWithHealthCheck() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
        ((LosslessConfigImpl) configurationImpl.getProvider().getLossless()).setEnable(true);
        configurationImpl.getGlobal().getAdmin().setPort(38081);
        try (SDKContext sdkContext = SDKContext.initContextByConfig(configuration)) {
            LosslessAPI losslessAPI = DiscoveryAPIFactory.createLosslessAPIByContext(sdkContext);
            DemoLosslessActionProvider demoLosslessActionProvider =
                    new DemoLosslessActionProvider(sdkContext, serviceKey, node, true);
            DefaultBaseInstance defaultBaseInstance = new DefaultBaseInstance();
            defaultBaseInstance.setNamespace(serviceKey.getNamespace());
            defaultBaseInstance.setService(serviceKey.getService());
            defaultBaseInstance.setHost(node.getHost());
            defaultBaseInstance.setPort(node.getPort());
            losslessAPI.setLosslessActionProvider(defaultBaseInstance, demoLosslessActionProvider);
            System.out.println("[testLosslessRegisterWithHealthCheck] start to do lossless register");
            losslessAPI.losslessRegister(defaultBaseInstance);
            Utils.sleepUninterrupted(10000);
            System.out.println("[testLosslessRegisterWithHealthCheck] start to query status");
            try {
                HttpInvokeUtils.ResponseWrapper wrapper = HttpInvokeUtils.sendRequest("http://127.0.0.1:38081/online", "GET");
                Assert.assertEquals(200, wrapper.getCode());
                Assert.assertEquals(RegisterStatus.REGISTERED.toString(), wrapper.getMessage());
            } catch (IOException e) {
                Assert.fail("e is " + e.getClass().getCanonicalName() + ", message " + e.getMessage());
            }
            System.out.println("[testLosslessRegisterWithHealthCheck] start to do lossless deregister");
            try {
                HttpInvokeUtils.ResponseWrapper wrapper = HttpInvokeUtils.sendRequest("http://127.0.0.1:38081/offline", "PUT");
                Assert.assertEquals(200, wrapper.getCode());
                Assert.assertEquals(LosslessPolicy.REPS_TEXT_OK, wrapper.getMessage());
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
            System.out.println("[testLosslessRegisterWithHealthCheck] start to query status");
            try {
                HttpInvokeUtils.ResponseWrapper wrapper = HttpInvokeUtils.sendRequest("http://127.0.0.1:38081/online", "GET");
                Assert.assertEquals(503, wrapper.getCode());
                Assert.assertEquals(RegisterStatus.UNREGISTERED.toString(), wrapper.getMessage());
            } catch (IOException e) {
                Assert.fail("e is " + e.getClass().getCanonicalName() + ", message " + e.getMessage());
            }
        }
    }

    @Test
    public void testLosslessDeregister() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
        ((LosslessConfigImpl) configurationImpl.getProvider().getLossless()).setEnable(true);
        configurationImpl.getGlobal().getAdmin().setPort(38082);
        try (SDKContext sdkContext = SDKContext.initContextByConfig(configuration)) {
            LosslessAPI losslessAPI = DiscoveryAPIFactory.createLosslessAPIByContext(sdkContext);
            DemoLosslessActionProvider demoLosslessActionProvider =
                    new DemoLosslessActionProvider(sdkContext, serviceKey, node, true);
            DefaultBaseInstance defaultBaseInstance = new DefaultBaseInstance();
            defaultBaseInstance.setNamespace(serviceKey.getNamespace());
            defaultBaseInstance.setService(serviceKey.getService());
            defaultBaseInstance.setHost(node.getHost());
            defaultBaseInstance.setPort(node.getPort());
            losslessAPI.setLosslessActionProvider(defaultBaseInstance, demoLosslessActionProvider);
            System.out.println("[testLosslessDeregister] start to do lossless register");
            losslessAPI.losslessRegister(defaultBaseInstance);
            Utils.sleepUninterrupted(10000);
            System.out.println("[testLosslessDeregister] start to query status");
            try {
                HttpInvokeUtils.ResponseWrapper wrapper = HttpInvokeUtils.sendRequest("http://127.0.0.1:38082/online", "GET");
                Assert.assertEquals(200, wrapper.getCode());
                Assert.assertEquals(RegisterStatus.REGISTERED.toString(), wrapper.getMessage());
            } catch (IOException e) {
                Assert.fail("e is " + e.getClass().getCanonicalName() + ", message " + e.getMessage());
            }
            System.out.println("[testLosslessDeregister] start to do lossless deregister");
            losslessAPI.losslessDeRegister(defaultBaseInstance);
            System.out.println("[testLosslessDeregister] start to query status");
            try {
                HttpInvokeUtils.ResponseWrapper wrapper = HttpInvokeUtils.sendRequest("http://127.0.0.1:38082/online", "GET");
                Assert.assertEquals(503, wrapper.getCode());
                Assert.assertEquals(RegisterStatus.UNREGISTERED.toString(), wrapper.getMessage());
            } catch (IOException e) {
                Assert.fail("e is " + e.getClass().getCanonicalName() + ", message " + e.getMessage());
            }
        }
    }

}
