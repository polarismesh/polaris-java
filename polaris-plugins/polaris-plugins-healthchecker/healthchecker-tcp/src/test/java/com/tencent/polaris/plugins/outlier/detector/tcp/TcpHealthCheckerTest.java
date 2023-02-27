///*
// * Tencent is pleased to support the open source community by making Polaris available.
// *
// * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
// *
// * Licensed under the BSD 3-Clause License (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://opensource.org/licenses/BSD-3-Clause
// *
// * Unless required by applicable law or agreed to in writing, software distributed
// * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
// * CONDITIONS OF ANY KIND, either express or implied. See the License for the
// * specific language governing permissions and limitations under the License.
// */
//
//package com.tencent.polaris.plugins.outlier.detector.tcp;
//
//import com.tencent.polaris.api.config.Configuration;
//import com.tencent.polaris.api.plugin.Supplier;
//import com.tencent.polaris.api.plugin.common.InitContext;
//import com.tencent.polaris.api.plugin.common.ValueContext;
//import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
//import com.tencent.polaris.api.pojo.DefaultInstance;
//import com.tencent.polaris.api.pojo.DetectResult;
//import com.tencent.polaris.api.pojo.RetStatus;
//import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule;
//import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;
//import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.TcpProtocolConfig;
//import com.tencent.polaris.test.common.TestUtils;
//import com.tencent.polaris.test.mock.discovery.NamingServer;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//
//import java.io.BufferedReader;
//import java.io.Closeable;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.net.InetSocketAddress;
//import java.net.ServerSocket;
//import java.net.Socket;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Map;
//import java.util.Objects;
//import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Consumer;
//
//import static com.tencent.polaris.test.common.TestUtils.SERVER_ADDRESS_ENV;
//
//public class TcpHealthCheckerTest {
//
//
//    private NamingServer namingServer;
//
//    private TcpHealthChecker checker;
//
//    private int port = 30000;
//
//    private DefaultInstance instance;
//
//    {
//        instance = new DefaultInstance();
//        instance.setNamespace("mock_namespace");
//        instance.setService("mock_service");
//        instance.setHost("127.0.0.1");
//        instance.setPort(port);
//        instance.setProtocol("TCP");
//        instance.setId(UUID.randomUUID().toString());
//    }
//
//    @Before
//    public void before() {
//        try {
//            namingServer = NamingServer.startNamingServer(-1);
//            System.setProperty(SERVER_ADDRESS_ENV, String.format("127.0.0.1:%d", namingServer.getPort()));
//            Configuration configuration = TestUtils.configWithEnvAddress();
//            checker = new TcpHealthChecker();
//            checker.init(new MockInitContext(configuration));
//        } catch (IOException e) {
//            Assert.fail(e.getMessage());
//        }
//    }
//
//    @Test
//    public void detectInstanceSuccess() throws Throwable {
//
//        String body = "user_body";
//
//        Map<String, String> detectInfo = new ConcurrentHashMap<>();
//
//        MockTcpServer tcpServer = new MockTcpServer(port, socket -> {
//            InputStream in = null;
//            OutputStream out = null;
//            try {
//                in = socket.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//                String receiveMsg = reader.lines().reduce((s, s2) -> s + s2).orElse("");
//                System.out.println("receive client send msg " + receiveMsg);
//                detectInfo.put("body", receiveMsg);
//                out = socket.getOutputStream();
//                out.write(receiveMsg.getBytes());
//                out.flush();
//                System.out.println("flush msg to client");
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (Objects.nonNull(in)) {
//                    try {
//                        in.close();
//                    } catch (Exception ee) {
//                        ee.printStackTrace();
//                    }
//                }
//                if (Objects.nonNull(out)) {
//                    try {
//                        out.close();
//                    } catch (Exception ee) {
//                        ee.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        try {
//            tcpServer.run();
//            TimeUnit.SECONDS.sleep(5);
//
//            FaultDetectRule faultDetectRule = FaultDetectRule
//                    .newBuilder()
//                    .setProtocol(Protocol.TCP)
//                    .setPort(port)
//                    .setTimeout(10*1000)
//                    .setTcpConfig(TcpProtocolConfig
//                            .newBuilder()
//                            .setSend(body)
//                            .addAllReceive(Collections.singletonList(body))
//                            .build())
//                    .build();
//
//            DetectResult result = checker.detectInstance(instance, faultDetectRule);
//
//
//            new Thread(() -> {
//                try {
//                    Socket socket = new Socket(instance.getHost(), instance.getPort());
//                    socket.setSoTimeout(10*1000);
//                    OutputStream os = socket.getOutputStream();
//                    //发包
//                    os.write("sendBytes".getBytes());
//                    os.flush();
//                    os.close();
//                    BufferedReader reader = new BufferedReader(
//                            new InputStreamReader(socket.getInputStream(), "UTF-8"));
//                    String line;
//                    while((line = reader.readLine()) != null){
//                        System.out.println(line);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }).start();
//            TimeUnit.SECONDS.sleep(10);
//
//            Assert.assertEquals(RetStatus.RetSuccess, result.getRetStatus());
//            Assert.assertEquals(detectInfo.get("body"), body);
//        } finally {
//            tcpServer.close();
//        }
//    }
//
//    @Test
//    public void detectInstanceFail() throws Throwable {
//
//        String body = "user_body";
//
//        Map<String, String> detectInfo = new ConcurrentHashMap<>();
//
//        MockTcpServer tcpServer = new MockTcpServer(port + 100, socket -> {
//            InputStream in = null;
//            OutputStream out = null;
//            try {
//                in = socket.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//                String receiveMsg = reader.lines().reduce((s, s2) -> s + s2).orElse("");
//                System.out.println("receive client send msg " + receiveMsg);
//                detectInfo.put("body", receiveMsg);
//                out = socket.getOutputStream();
//                out.write(receiveMsg.getBytes());
//                out.flush();
//                out.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (Objects.nonNull(in)) {
//                    try {
//                        in.close();
//                    } catch (Exception ee) {
//                        ee.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        try {
//            tcpServer.run();
//            TimeUnit.SECONDS.sleep(5);
//
//            FaultDetectRule faultDetectRule = FaultDetectRule
//                    .newBuilder()
//                    .setProtocol(Protocol.TCP)
//                    .setPort(port + 100)
//                    .setTimeout(10*1000)
//                    .setTcpConfig(TcpProtocolConfig
//                            .newBuilder()
//                            .setSend(body)
//                            .addAllReceive(Collections.singletonList(body))
//                            .build())
//                    .build();
//
//            DetectResult result = checker.detectInstance(instance, faultDetectRule);
//            Assert.assertEquals(RetStatus.RetFail, result.getRetStatus());
//            Assert.assertNull(detectInfo.get("body"));
//        } finally {
//            tcpServer.close();
//        }
//    }
//
//    public static class MockInitContext implements InitContext {
//
//        private final Configuration configuration;
//
//        public MockInitContext(Configuration configuration) {
//            this.configuration = configuration;
//        }
//
//        @Override
//        public Configuration getConfig() {
//            return configuration;
//        }
//
//        @Override
//        public Supplier getPlugins() {
//            return null;
//        }
//
//        @Override
//        public ValueContext getValueContext() {
//            return null;
//        }
//
//        @Override
//        public Collection<ServerServiceInfo> getServerServices() {
//            return null;
//        }
//    }
//
//    public static class MockTcpServer implements Closeable {
//
//        private final int port;
//
//        private final Consumer<Socket> consumer;
//
//        private final ExecutorService executor = Executors.newCachedThreadPool();
//
//        private Thread thread;
//
//        private volatile boolean run = true;
//
//        public MockTcpServer(int port, Consumer<Socket> consumer) {
//            this.port = port;
//            this.consumer = consumer;
//        }
//
//        public void run() {
//            this.thread = new Thread(() -> {
//                ServerSocket serverSocket = null;
//                try {
//                    serverSocket = new ServerSocket();
//                    serverSocket.bind(new InetSocketAddress("0.0.0.0", port));
//                    serverSocket.setSoTimeout(20*1000);
//                    while (run) {
//                        Socket socket = serverSocket.accept();
//                        new Thread(() -> {
//                            System.out.println("receive new client "+ socket.toString());
//                            try {
//                                consumer.accept(socket);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        }).start();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//                    if (Objects.nonNull(serverSocket)) {
//                        try {
//                            serverSocket.close();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            });
//            this.thread.start();
//        }
//
//        @Override
//        public void close() throws IOException {
//            this.run = false;
//            this.executor.shutdownNow();
//            this.thread.stop();
//        }
//    }
//
//}