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

package com.tencent.polaris.plugins.outlier.detector.tcp;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.TcpProtocolConfig;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TcpHealthCheckerTest {


    private TcpHealthChecker checker;

    private final int port = 30000;

    private DefaultInstance instance;

    {
        instance = new DefaultInstance();
        instance.setNamespace("mock_namespace");
        instance.setService("mock_service");
        instance.setHost("127.0.0.1");
        instance.setPort(port);
        instance.setProtocol("TCP");
        instance.setId(UUID.randomUUID().toString());
    }

    @Before
    public void before() {
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setDefault();
        checker = new TcpHealthChecker();
        checker.init(new MockInitContext(configuration));
    }

    @Test
    public void detectInstanceSuccess() throws Throwable {

        String body = "user_body";

        int curPort = port + 3;
        instance.setPort(curPort);
        NioTCPServer tcpServer = new NioTCPServer(curPort, new NioHandlerImpl());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                tcpServer.listenAndServ();
            }
        });
        thread.start();
        try {

            TimeUnit.SECONDS.sleep(5);

            FaultDetectRule faultDetectRule = FaultDetectRule
                    .newBuilder()
                    .setProtocol(Protocol.TCP)
                    .setPort(curPort)
                    .setTimeout(10 * 1000)
                    .setTcpConfig(TcpProtocolConfig
                            .newBuilder()
                            .setSend(body)
                            .addAllReceive(Collections.singletonList(body))
                            .build())
                    .build();

            DetectResult result = checker.detectInstance(instance, faultDetectRule);

            Assert.assertEquals(RetStatus.RetSuccess, result.getRetStatus());
        } finally {
            tcpServer.close();
            thread.join();
        }
    }

    @Test
    public void detectInstanceFail() throws Throwable {

        String body = "user_body";

        int curPort = port + 1;
        instance.setPort(curPort);
        NioTCPServer tcpServer = new NioTCPServer(curPort, new NioHandlerImpl());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                tcpServer.listenAndServ();
            }
        });
        thread.start();
        try {
            TimeUnit.SECONDS.sleep(5);

            FaultDetectRule faultDetectRule = FaultDetectRule
                    .newBuilder()
                    .setProtocol(Protocol.TCP)
                    .setPort(curPort)
                    .setTimeout(10 * 1000)
                    .setTcpConfig(TcpProtocolConfig
                            .newBuilder()
                            .setSend(body)
                            .addAllReceive(Collections.singletonList(body + "0"))
                            .build())
                    .build();

            DetectResult result = checker.detectInstance(instance, faultDetectRule);
            Assert.assertEquals(RetStatus.RetFail, result.getRetStatus());
        } finally {
            tcpServer.close();
            thread.join();
        }
    }

    @Test
    public void detectInstanceConnect() throws Throwable {

        int curPort = port + 2;
        instance.setPort(curPort);
        NioTCPServer tcpServer = new NioTCPServer(curPort, new NioHandlerImpl());

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                tcpServer.listenAndServ();
            }
        });
        thread.start();
        try {
            TimeUnit.SECONDS.sleep(5);
            FaultDetectRule faultDetectRule = FaultDetectRule
                    .newBuilder()
                    .setProtocol(Protocol.TCP)
                    .setTimeout(10 * 1000)
                    .setTcpConfig(TcpProtocolConfig
                            .newBuilder()
                            .build())
                    .build();

            DetectResult result = checker.detectInstance(instance, faultDetectRule);

            Assert.assertEquals(RetStatus.RetSuccess, result.getRetStatus());
        } finally {
            tcpServer.close();
            thread.join();
        }
    }

    public static class MockInitContext implements InitContext {

        private final Configuration configuration;

        public MockInitContext(Configuration configuration) {
            this.configuration = configuration;
        }

        @Override
        public Configuration getConfig() {
            return configuration;
        }

        @Override
        public Supplier getPlugins() {
            return null;
        }

        @Override
        public ValueContext getValueContext() {
            return null;
        }

        @Override
        public Collection<ServerServiceInfo> getServerServices() {
            return null;
        }
    }

}