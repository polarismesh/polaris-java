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

package com.tencent.polaris.plugins.connector.grpc;

import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_CIRCUIT_BREAKER;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.plugins.connector.grpc.Connection.ConnID;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConnectionManagerTest {

    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(TestUtils.SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        ServiceKey serviceKey = new ServiceKey(NAMESPACE_TEST, SERVICE_PROVIDER);
        InstanceParameter parameter = new InstanceParameter();
        parameter.setHealthy(true);
        parameter.setIsolated(false);
        parameter.setWeight(100);
        namingServer.getNamingService().batchAddInstances(serviceKey, 10012, 1, parameter);
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    @Test
    public void testSwitchClient() {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ((ConfigurationImpl)configuration).getGlobal().getServerConnector().setServerSwitchInterval(1000L);
        AtomicBoolean switched = new AtomicBoolean(false);
        try (SDKContext sdkContext = SDKContext.initContextByConfig(configuration)) {
            ServerConnector serverConnector = (ServerConnector) sdkContext.getPlugins().getPlugin(
                    PluginTypes.SERVER_CONNECTOR.getBaseType(), DefaultValues.DEFAULT_DISCOVER_PROTOCOL);
            GrpcConnector grpcConnector = (GrpcConnector) serverConnector;
            ConnectionManager connectionManager = grpcConnector.getConnectionManager();
            Extensions extensions = new Extensions();
            connectionManager.setExtensions(extensions);
            connectionManager.setCallbackOnSwitched(new Consumer<ConnID>() {
                @Override
                public void accept(ConnID connID) {
                    if (switched.compareAndSet(false, true)) {
                        System.out.println("server switched to " + connID);
                    }
                }
            });
            Connection testConn = connectionManager.getConnection("test", ClusterType.BUILTIN_CLUSTER);
            Assert.assertNotNull(testConn);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Assert.assertTrue(switched.get());
    }

    @Test
    public void testNoSwitchClientOnFailNetworkError() {
        System.setProperty(TestUtils.SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d",
                namingServer.getPort()));
        Configuration configuration = TestUtils.configWithEnvAddress();
        commonSwitchClientOnFail(configuration, ErrorCode.NETWORK_ERROR, 5, switched -> {
            Assert.assertTrue(switched >= 1);
        });
    }

    @Test
    public void testSwitchClientOnFailBusinessError() {
        System.setProperty(TestUtils.SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d",
                namingServer.getPort()));
        Configuration configuration = TestUtils.configWithEnvAddress();
        commonSwitchClientOnFail(configuration, ErrorCode.INVALID_SERVER_RESPONSE, 5, switched -> {
            Assert.assertEquals(0, (int) switched);
        });
    }

    private void commonSwitchClientOnFail(Configuration configuration, ErrorCode errorCode, int reportFailCnt, Consumer<Integer> predicate) {
        ((ConfigurationImpl) configuration).getGlobal().getServerConnector().setServerSwitchInterval(TimeUnit.MINUTES.toMillis(10));
        AtomicInteger switched = new AtomicInteger(0);
        try (SDKContext sdkContext = SDKContext.initContextByConfig(configuration)) {
            ServerConnector serverConnector = (ServerConnector) sdkContext.getPlugins().getPlugin(
                    PluginTypes.SERVER_CONNECTOR.getBaseType(), DefaultValues.DEFAULT_DISCOVER_PROTOCOL);
            GrpcConnector grpcConnector = (GrpcConnector) serverConnector;
            ConnectionManager connectionManager = grpcConnector.getConnectionManager();
            Extensions extensions = new Extensions();
            connectionManager.setExtensions(extensions);
            connectionManager.setCallbackOnSwitched(connID -> {
                switched.incrementAndGet();
                System.out.println("server switched to " + connID);
            });
            for (int i = 0; i < reportFailCnt; i ++) {
                Connection testConn = connectionManager.getConnection("test", ClusterType.BUILTIN_CLUSTER);
                Assert.assertNotNull(testConn);
                testConn.reportFail(errorCode);
            }
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        predicate.accept(switched.get());
    }

}
