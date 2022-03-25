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
import io.grpc.ServerInterceptors;
import java.io.IOException;
import org.slf4j.Logger;

public class NamingServer {

    private static final Logger LOG = LoggerFactory.getLogger(NamingServer.class);

    private final Server server;

    private final int port;

    private final NamingService namingService;

    public NamingServer(int port) {
        namingService = new NamingService();
        server = ServerBuilder.forPort(port).addService(
                ServerInterceptors.intercept(namingService, new HeaderInterceptor())).build();
        this.port = port;
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

    public static NamingServer startNamingServer(int port) throws IOException {
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
}
