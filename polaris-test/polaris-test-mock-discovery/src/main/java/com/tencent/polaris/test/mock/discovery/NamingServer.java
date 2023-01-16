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

package com.tencent.polaris.test.mock.discovery;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.test.mock.discovery.NamingService.InstanceParameter;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;

public class NamingServer {

    private static final Logger LOG = LoggerFactory.getLogger(NamingServer.class);
    /**
     * The minimal random port
     */
    private static final int MIN_RANDOM_PORT = 20000;
    /**
     * The maximum random port
     */
    private static final int MAX_RANDOM_PORT = 65535;

    private final Server server;

    private final int port;

    private final NamingService namingService;

    public NamingServer(int port) {
        this(port, null);
    }

    public NamingServer(int port, ServerInterceptor[] interceptors) {
        this.port = port;
        namingService = new NamingService();
        if (null != interceptors && interceptors.length > 0) {
            server = ServerBuilder.forPort(port).addService(ServerInterceptors.intercept(namingService, interceptors))
                    .build();
        } else {
            server = ServerBuilder.forPort(port).addService(ServerInterceptors.intercept(namingService)).build();
        }
    }

    public static NamingServer startNamingServer(int port) throws IOException {
        if (port <= 0) {
            port = selectRandomPort();
        }
        NamingServer namingServer = new NamingServer(port);
        namingServer.start();
        Node node = new Node("127.0.0.1", port);
        InstanceParameter parameter = new InstanceParameter();
        parameter.setHealthy(true);
        parameter.setIsolated(false);
        parameter.setProtocol("grpc");
        parameter.setWeight(100);
        // 注册系统集群地址
        namingServer.getNamingService().addInstance(
                new ServiceKey("Polaris", "polaris.discover"), node, parameter);
        namingServer.getNamingService().addInstance(
                new ServiceKey("Polaris", "polaris.healthcheck"), node, parameter);
        return namingServer;
    }

    public static int selectRandomPort() {
        int randomPort = ThreadLocalRandom.current().nextInt(MIN_RANDOM_PORT, MAX_RANDOM_PORT);
        while (!isPortAvailable(randomPort)) {
            randomPort = ThreadLocalRandom.current().nextInt(MIN_RANDOM_PORT, MAX_RANDOM_PORT);
        }
        return randomPort;
    }

    private static boolean isPortAvailable(int port) {
        try {
            bindPort("0.0.0.0", port);
            bindPort(InetAddress.getLocalHost().getHostAddress(), port);
            bindPort(InetAddress.getLoopbackAddress().getHostAddress(), port);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void bindPort(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.bind(new InetSocketAddress(host, port));
        }
    }

    public void start() throws IOException {
        server.start();
        LOG.info(String.format("server start listening on %d", port));
    }

    public int getPort() {
        return port;
    }

    public void terminate() {
        server.shutdown();
    }

    public NamingService getNamingService() {
        return namingService;
    }
}
