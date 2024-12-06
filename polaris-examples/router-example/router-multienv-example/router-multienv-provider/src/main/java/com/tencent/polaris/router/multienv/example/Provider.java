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

package com.tencent.polaris.router.multienv.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.SourceService;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

public class Provider {

    private static final String NAMESPACE_DEFAULT = "default";

    private static final String PATH = "/echo";

    private static final int TTL = 5;

    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("service", "service name", true, "service name for instance");
        options.addOption("env", "env name", true, "env name for instance");
        options.addOption("nextService",
                "next service name", true, "next service name for instance");
        CommandLine commandLine = parser.parse(options, args);
        String service = commandLine.getOptionValue("service");
        String env = commandLine.getOptionValue("env");
        if (StringUtils.isBlank(service) || StringUtils.isBlank(env)) {
            System.out.println("service and env is required");
            return;
        }
        String nextService = commandLine.getOptionValue("nextService");

        String namespace = NAMESPACE_DEFAULT;

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/echo", new EchoServerHandler(service, env, nextService));

        Configuration configuration = ConfigAPIFactory.defaultConfig();
        String localHost = getLocalHost(configuration);
        int localPort = server.getAddress().getPort();

        ProviderAPI providerAPI = DiscoveryAPIFactory.createProviderAPIByConfig(configuration);
        register(namespace, service, localHost, localPort, env, providerAPI);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            deregister(namespace, service, localHost, localPort, providerAPI);
            providerAPI.close();
        }));
        server.start();
    }

    // do the instance register
    private static void register(String namespace, String service, String host, int port, String env,
            ProviderAPI providerAPI) {
        InstanceRegisterRequest registerRequest = new InstanceRegisterRequest();
        registerRequest.setNamespace(namespace);
        registerRequest.setService(service);
        registerRequest.setHost(host);
        registerRequest.setPort(port);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("env", env);
        registerRequest.setMetadata(metadata);
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

    private static class EchoServerHandler implements HttpHandler {

        private final String service;

        private final String env;

        private final String nextService;

        private final ConsumerAPI consumerAPI;

        public EchoServerHandler(String service, String env, String nextService) {
            this.service = service;
            this.env = env;
            this.nextService = nextService;
            consumerAPI = DiscoveryAPIFactory.createConsumerAPI();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> parameters = splitQuery(exchange.getRequestURI());
            String echoValue = parameters.get("value");
            String respValue;
            if (StringUtils.isBlank(nextService)) {
                respValue = "[service: " + service + ", env: " + env + "]";
            } else {
                Map<String, String> metadata = new HashMap<>(parameters);
                if (!metadata.containsKey("env")) {
                    metadata.put("env", env);
                }
                // 1. we need to do naming resolution to get a load balanced host and port
                GetOneInstanceRequest getOneInstanceRequest = new GetOneInstanceRequest();
                getOneInstanceRequest.setNamespace(NAMESPACE_DEFAULT);
                getOneInstanceRequest.setService(nextService);
                SourceService serviceInfo = new SourceService();
                serviceInfo.setNamespace(NAMESPACE_DEFAULT);
                serviceInfo.setService(service);
                serviceInfo.setMetadata(metadata);
                getOneInstanceRequest.setServiceInfo(serviceInfo);
                InstancesResponse oneInstance = consumerAPI.getOneInstance(getOneInstanceRequest);
                Instance[] instances = oneInstance.getInstances();
                System.out.println("instances count is " + instances.length);
                Instance targetInstance = instances[0];
                System.out.printf("target instance is %s:%d%n", targetInstance.getHost(), targetInstance.getPort());

                String urlStr = String
                        .format("http://%s:%d%s", targetInstance.getHost(), targetInstance.getPort(), PATH);
                String metadataStr = buildMetadataStr(metadata);
                if (!StringUtils.isBlank(metadataStr)) {
                    urlStr = urlStr + "?" + metadataStr;
                }
                HttpResult httpResult = httpGet(urlStr);
                if (httpResult.code == 200) {
                    respValue = "[service: " + service + ", env: " + env + "]" + " -> " +
                            httpResult.message;
                } else {
                    respValue = "[service: " + service + ", env: " + env + "]" + " -> " +
                            "[http fail: " + httpResult.code + "]";
                }
            }
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(respValue.getBytes());
            os.close();
        }
    }

    private static String buildMetadataStr(Map<String, String> metadata) {
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            values.add(entry.getKey() + "=" + entry.getValue());
        }
        if (values.isEmpty()) {
            return "";
        }
        return String.join("&", values);
    }

    private static HttpResult httpGet(String urlStr) {
        HttpURLConnection connection = null;
        String respMessage;
        int code = -1;
        BufferedReader bufferedReader = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            code = connection.getResponseCode();
            bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            respMessage = bufferedReader.readLine();

        } catch (IOException e) {
            e.printStackTrace();
            respMessage = e.getMessage();
        } finally {
            if (null != connection) {
                connection.disconnect();
            }
            if (null != bufferedReader) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return new HttpResult(code, respMessage);
    }

    private static class HttpResult {

        private final int code;

        private final String message;

        public HttpResult(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
