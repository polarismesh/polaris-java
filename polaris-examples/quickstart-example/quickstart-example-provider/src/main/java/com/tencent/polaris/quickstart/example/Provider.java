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

package com.tencent.polaris.quickstart.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Provider {

    private static final String NAMESPACE_DEFAULT = "default";

    private static final String ECHO_SERVICE_NAME = "EchoServerJava";

    private static final int TTL = 5;

    private static final int LISTEN_PORT = 15800;

    private static final ScheduledExecutorService HEARTBEAT_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws Exception {

        String namespace = NAMESPACE_DEFAULT;
        String service = ECHO_SERVICE_NAME;

        HttpServer server = HttpServer.create(new InetSocketAddress(LISTEN_PORT), 0);
        server.createContext("/echo", new EchoServerHandler());

        Configuration configuration = ConfigAPIFactory.defaultConfig();
        String localHost = getLocalHost(configuration);
        int localPort = server.getAddress().getPort();

//        List<ServerConnectorConfigImpl> connectorConfigList = configuration.getGlobal().getServerConnectors();
//        for (ServerConnectorConfigImpl serverConnectorConfig : connectorConfigList) {
//            if (DefaultPlugins.SERVER_CONNECTOR_CONSUL.equals(serverConnectorConfig.getProtocol())) {
//                Map<String, String> metadata = serverConnectorConfig.getMetadata();
//                metadata.put(MetadataMapKey.INSTANCE_ID_KEY, "EJ-111");
//                metadata.put(MetadataMapKey.IP_ADDRESS_KEY, "localhost");
//                metadata.put(MetadataMapKey.PREFER_IP_ADDRESS_KEY, "true");
//            }
//        }

        ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPIByConfig(configuration);
        HEARTBEAT_EXECUTOR
                .schedule(new RegisterTask(namespace, service, localHost, localPort, providerAPI),
                        500,
                        TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            HEARTBEAT_EXECUTOR.shutdown();
            server.stop(1);
            deregister(namespace, service, localHost, localPort, providerAPI);
            providerAPI.close();
        }));
        server.start();
    }

    // do the instance register
    private static void register(String namespace, String service, String host, int port,
            ProviderAPI providerAPI) {
        InstanceRegisterRequest registerRequest = new InstanceRegisterRequest();
        registerRequest.setNamespace(namespace);
        registerRequest.setService(service);
        registerRequest.setHost(host);
        registerRequest.setPort(port);
        registerRequest.setTtl(TTL);
        InstanceRegisterResponse registerResp = providerAPI.register(registerRequest);
        System.out.printf("register instance %s:%d to service %s(%s), id is %s%n",
                host, port, service, namespace, registerResp.getInstanceId());
    }

    // do the instance heartbeat
    private static void heartbeat(String namespace, String service, String host, int port,
            ProviderAPI providerAPI) {
        // do heartbeat
        InstanceHeartbeatRequest heartbeatRequest = new InstanceHeartbeatRequest();
        heartbeatRequest.setNamespace(namespace);
        heartbeatRequest.setService(service);
        heartbeatRequest.setHost(host);
        heartbeatRequest.setPort(port);
        providerAPI.heartbeat(heartbeatRequest);
        System.out.printf("heartbeat instance, address is %s:%d%n", host, port);
    }

    // do the instance deregister
    private static void deregister(String namespace, String service, String host, int port,
            ProviderAPI providerAPI) {
        InstanceDeregisterRequest deregisterRequest = new InstanceDeregisterRequest();
        deregisterRequest.setNamespace(namespace);
        deregisterRequest.setService(service);
        deregisterRequest.setHost(host);
        deregisterRequest.setPort(port);
        providerAPI.deRegister(deregisterRequest);
        System.out.printf("deregister instance, address is %s:%d%n", host, port);
    }

    private static Map<String, String> splitQuery(URI uri) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = uri.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    private static String getLocalHost(Configuration configuration) throws Exception {
        String serverAddress = configuration.getGlobal().getServerConnector().getAddresses().get(0);
        String[] tokens = serverAddress.split(":");
        try (Socket socket = new Socket(tokens[0], Integer.parseInt(tokens[1]))) {
            return socket.getLocalAddress().getHostAddress();
        }
    }

    private static class RegisterTask implements Runnable {

        private final String namespace;

        private final String service;

        private final String host;

        private final int port;

        private final ProviderAPI providerAPI;

        public RegisterTask(String namespace, String service, String host, int port,
                ProviderAPI providerAPI) {
            this.namespace = namespace;
            this.service = service;
            this.host = host;
            this.port = port;
            this.providerAPI = providerAPI;
        }

        @Override
        public void run() {
            Provider.register(namespace, service, host, port, providerAPI);
            // register successfully, then start to do heartbeat
            Provider.HEARTBEAT_EXECUTOR
                    .scheduleWithFixedDelay(new HeartbeatTask(namespace, service, host, port, providerAPI), TTL, TTL,
                            TimeUnit.SECONDS);
        }
    }

    private static class HeartbeatTask implements Runnable {

        private final String namespace;

        private final String service;

        private final String host;

        private final int port;

        private final ProviderAPI providerAPI;

        public HeartbeatTask(String namespace, String service, String host, int port,
                ProviderAPI providerAPI) {
            this.namespace = namespace;
            this.service = service;
            this.host = host;
            this.port = port;
            this.providerAPI = providerAPI;
        }

        @Override
        public void run() {
            Provider.heartbeat(namespace, service, host, port, providerAPI);
        }
    }

    private static class EchoServerHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> parameters = splitQuery(exchange.getRequestURI());
            String echoValue = parameters.get("value");
            String response = "echo: " + echoValue;
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
