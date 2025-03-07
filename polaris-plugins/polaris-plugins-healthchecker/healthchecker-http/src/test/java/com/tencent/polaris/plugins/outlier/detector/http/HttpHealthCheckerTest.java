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

package com.tencent.polaris.plugins.outlier.detector.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.HttpProtocolConfig;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.HttpProtocolConfig.MessageHeader;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;

public class HttpHealthCheckerTest {

    private NamingServer namingServer;

    private HttpHealthChecker checker;

    private int port = 30000;

    private DefaultInstance instance;

    {
        instance = new DefaultInstance();
        instance.setNamespace("mock_namespace");
        instance.setService("mock_service");
        instance.setHost("127.0.0.1");
        instance.setPort(port);
        instance.setProtocol("http");
        instance.setId(UUID.randomUUID().toString());
    }

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(-1);
            System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
            Configuration configuration = TestUtils.configWithEnvAddress();
            checker = new HttpHealthChecker();
            checker.init(new MockInitContext(configuration));
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void detectInstanceSuccess() throws Throwable {

        String headerKey = "user_key";
        String headerValue = "user_value";
        String body = "user_body";

        Map<String, String> detectInfo = new ConcurrentHashMap<>();

        MockHttpServer httpServer = new MockHttpServer(port, httpExchange -> {
            try {
                httpExchange.getRequestHeaders().forEach((header, values) -> {
                    System.out.println("header: " + header + ", values: " + values);
                    if (Objects.equals(header.toLowerCase(), headerKey.toLowerCase()) && CollectionUtils.isNotEmpty(values)) {
                        detectInfo.put(header.toLowerCase(), values.get(0));
                    }
                });
                httpExchange.sendResponseHeaders(200, 0);
                OutputStream os = httpExchange.getResponseBody();
                os.write(body.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            httpServer.run();
            TimeUnit.SECONDS.sleep(5);

            FaultDetectRule faultDetectRule = FaultDetectRule
                    .newBuilder()
                    .setProtocol(Protocol.HTTP)
                    .setHttpConfig(HttpProtocolConfig
                            .newBuilder()
                            .setMethod("GET")
                            .setUrl("/echo")
                            .addHeaders(MessageHeader
                                    .newBuilder()
                                    .setKey(headerKey)
                                    .setValue(headerValue)
                                    .build())
                            .setBody(body)
                            .build())
                    .build();

            DetectResult result = checker.detectInstance(instance, faultDetectRule);
            Assert.assertEquals(RetStatus.RetSuccess, result.getRetStatus());
            Assert.assertEquals(detectInfo.get(headerKey.toLowerCase()), headerValue);
        } finally {
            httpServer.close();
        }
    }

    @Test
    public void detectInstanceFail() throws Throwable {

        String headerKey = "user_key";
        String headerValue = "user_value";
        String body = "user_body";

        Map<String, String> detectInfo = new ConcurrentHashMap<>();

        MockHttpServer httpServer = new MockHttpServer(port + 100, httpExchange -> {
            try {
                httpExchange.getRequestHeaders().forEach((header, values) -> {
                    System.out.println("header: " + header + ", values: " + values);
                    if (Objects.equals(header.toLowerCase(), headerKey.toLowerCase()) && CollectionUtils.isNotEmpty(values)) {
                        detectInfo.put(header.toLowerCase(), values.get(0));
                    }
                });
                httpExchange.sendResponseHeaders(500, 0);
                OutputStream os = httpExchange.getResponseBody();
                os.write(body.getBytes());
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        try {
            httpServer.run();
            TimeUnit.SECONDS.sleep(5);

            FaultDetectRule faultDetectRule = FaultDetectRule
                    .newBuilder()
                    .setProtocol(Protocol.HTTP)
                    .setPort(port + 100)
                    .setHttpConfig(HttpProtocolConfig
                            .newBuilder()
                            .setMethod("GET")
                            .setUrl("/echo")
                            .addHeaders(MessageHeader
                                    .newBuilder()
                                    .setKey(headerKey)
                                    .setValue(headerValue)
                                    .build())
                            .setBody(body)
                            .build())
                    .build();

            DetectResult result = checker.detectInstance(instance, faultDetectRule);
            Assert.assertEquals(RetStatus.RetFail, result.getRetStatus());
        } finally {
            httpServer.close();
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

    public static class MockHttpServer implements HttpHandler, Closeable {

        private final int port;

        private final Consumer<HttpExchange> consumer;

        private Thread thread;

        public MockHttpServer(int port, Consumer<HttpExchange> consumer) {
            this.port = port;
            this.consumer = consumer;
        }

        public void run() {
            this.thread = new Thread(() -> {
                HttpServer abnormalServer = null;
                try {
                    abnormalServer = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0);
                    abnormalServer.createContext("/", MockHttpServer.this);
                    abnormalServer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            this.thread.start();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            this.consumer.accept(exchange);
        }

        @Override
        public void close() throws IOException {
            if (this.thread != null && this.thread.isAlive()) {
                this.thread.interrupt();
            }
        }
    }
}