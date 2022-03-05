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
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class Consumer {

    private static final String NAMESPACE_DEFAULT = "default";

    private static final String ECHO_SERVICE_NAME = "EchoServerJava";

    private static final int LISTEN_PORT = 15700;

    private static final String PATH = "/echo";

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(LISTEN_PORT), 0);
        ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPI();

        server.createContext(PATH, new EchoClientHandler(NAMESPACE_DEFAULT, ECHO_SERVICE_NAME, consumerAPI));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            consumerAPI.close();
        }));
        server.start();
    }

    private static String invokeByNameResolution(String namespace, String service, String value,
            ConsumerAPI consumerAPI) {
        System.out.println("namespace " + namespace + ", service " + service);
        // 1. we need to do naming resolution to get a load balanced host and port
        GetOneInstanceRequest getOneInstanceRequest = new GetOneInstanceRequest();
        getOneInstanceRequest.setNamespace(namespace);
        getOneInstanceRequest.setService(service);
        InstancesResponse oneInstance = consumerAPI.getOneInstance(getOneInstanceRequest);
        Instance[] instances = oneInstance.getInstances();
        System.out.println("instances count is " + instances.length);
        Instance targetInstance = instances[0];
        System.out.printf("target instance is %s:%d%n", targetInstance.getHost(), targetInstance.getPort());

        // 2. invoke the server by the resolved address
        String urlStr = String
                .format("http://%s:%d%s?value=%s", targetInstance.getHost(), targetInstance.getPort(), PATH, value);
        long startMillis = System.currentTimeMillis();
        HttpResult httpResult = httpGet(urlStr);
        long delay = System.currentTimeMillis() - startMillis;
        System.out.printf("invoke %s, code is %d, delay is %d%n", urlStr, httpResult.code, delay);

        // 3. report the invoke result to polaris-java, to eliminate the fail address
        RetStatus status = RetStatus.RetSuccess;
        if (httpResult.code != 200) {
            status = RetStatus.RetFail;
        }
        ServiceCallResult result = new ServiceCallResult();
        result.setNamespace(namespace);
        result.setService(service);
        result.setHost(targetInstance.getHost());
        result.setPort(targetInstance.getPort());
        result.setRetCode(httpResult.code);
        result.setDelay(delay);
        result.setRetStatus(status);
        consumerAPI.updateServiceCallResult(result);
        System.out.println("success to call updateServiceCallResult");
        return httpResult.message;
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


    private static class EchoClientHandler implements HttpHandler {

        private final String namespace;

        private final String service;

        private final ConsumerAPI consumerAPI;

        public EchoClientHandler(String namespace, String service, ConsumerAPI consumerAPI) {
            this.namespace = namespace;
            this.service = service;
            this.consumerAPI = consumerAPI;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> parameters = splitQuery(exchange.getRequestURI());
            String echoValue = parameters.get("value");
            String response = invokeByNameResolution(namespace, service, echoValue, consumerAPI);
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static class HttpResult {

        private final int code;

        private final String message;

        public HttpResult(int code, String message) {
            this.code = code;
            this.message = message;
        }
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

}
