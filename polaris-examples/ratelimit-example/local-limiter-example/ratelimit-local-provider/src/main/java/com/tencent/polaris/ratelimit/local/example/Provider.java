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

package com.tencent.polaris.ratelimit.local.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Provider {

    private static final String NAMESPACE_DEFAULT = "default";

    private static final String ECHO_SERVICE_NAME = "RateLimitServiceJava";

    private static final int TTL = 5;

    private static final int LISTEN_PORT = 0;

    private static final ScheduledExecutorService HEARTBEAT_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws Exception {

        String namespace = NAMESPACE_DEFAULT;
        String service = ECHO_SERVICE_NAME;

        HttpServer server = HttpServer.create(new InetSocketAddress(LISTEN_PORT), 0);
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        SDKContext sdkContext = SDKContext.initContextByConfig(configuration);
        LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByContext(sdkContext);
        server.createContext("/echo", new EchoServerHandler(limitAPI));

        String localHost = getLocalHost(configuration);
        int localPort = server.getAddress().getPort();

        ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(sdkContext);
        HEARTBEAT_EXECUTOR
                .schedule(new RegisterTask(namespace, service, localHost, localPort, providerAPI),
                        500,
                        TimeUnit.MILLISECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            HEARTBEAT_EXECUTOR.shutdown();
            server.stop(1);
            deregister(namespace, service, localHost, localPort, providerAPI);
            sdkContext.close();
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
        InstanceRegisterResponse registerResp = providerAPI.registerInstance(registerRequest);
        System.out.printf("register instance %s:%d to service %s(%s), id is %s%n",
                host, port, service, namespace, registerResp.getInstanceId());
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
        }
    }

    private static class EchoServerHandler implements HttpHandler {

        private final LimitAPI limitAPI;

        private long lastTimestamp = 0;

        public EchoServerHandler(LimitAPI limitAPI) {
            this.limitAPI = limitAPI;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> parameters = splitQuery(exchange.getRequestURI());
            QuotaRequest quotaRequest = new QuotaRequest();
            quotaRequest.setNamespace(NAMESPACE_DEFAULT);
            quotaRequest.setService(ECHO_SERVICE_NAME);
            quotaRequest.setMethod("/echo");
            Set<Argument> matchArgumentSet = new HashSet<>();
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                Argument matchArgument = Argument.buildQuery(entry.getKey(), entry.getValue());
                matchArgumentSet.add(matchArgument);
            }
            quotaRequest.setArguments(matchArgumentSet);
            quotaRequest.setCount(1);
            QuotaResponse quotaResponse = limitAPI.getQuota(quotaRequest);
            OutputStream os = exchange.getResponseBody();
            if (quotaResponse.getCode() == QuotaResultCode.QuotaResultOk) {
                // 匀速排队等待
                if (quotaResponse.getWaitMs() > 0) {
                    try {
                        Thread.sleep(quotaResponse.getWaitMs());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                String echoValue = parameters.get("value");
                String response = "echo: " + echoValue;
                exchange.sendResponseHeaders(200, 0);
                os.write(response.getBytes());
                long currentTimestamp = System.currentTimeMillis();
                if (lastTimestamp != 0) {
                    System.out.println(
                            "未被限流，当前时间：" + currentTimestamp + ",与上次差值：" + (currentTimestamp - lastTimestamp));
                } else {
                    System.out.println("未被限流，当前时间：" + currentTimestamp);
                }
                lastTimestamp = currentTimestamp;

            } else {
                exchange.sendResponseHeaders(429, 0);
                os.write("request limited".getBytes());
                System.out.println("被限流，当前时间：" + System.currentTimeMillis());
            }
            os.close();
        }
    }

}
