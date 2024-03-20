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

package com.tencent.polaris;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RouteArgument;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import com.tencent.polaris.metadata.core.manager.MetadataManager;
import com.tencent.polaris.metadata.core.manager.MetadataManagerHolder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class Main {

    public static void main(String[] args) throws Exception {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(configuration);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/echo", new EchoServerHandler(consumerAPI));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
        }));
        server.start();
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

    private static class EchoServerHandler implements HttpHandler {

        private final ConsumerAPI consumerAPI;

        public EchoServerHandler(ConsumerAPI consumerAPI) {
            this.consumerAPI = consumerAPI;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> parameters = splitQuery(exchange.getRequestURI());
            Set<RouteArgument> arguments = new HashSet<>();

            exchange.getRequestHeaders().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    arguments.add(RouteArgument.buildHeader(key.toLowerCase(), values.get(0)));
                }
            });
            parameters.forEach((key, value) -> arguments.add(RouteArgument.buildQuery(key.toLowerCase(), value)));

            String response = Consumer.invokeByNameResolution(arguments, parameters.get("value"), consumerAPI);
            String ret = "[Gateway]" + " --> " + response;

            MetadataManager metadataManager = MetadataManagerHolder.get();
            MetadataContainer container = metadataManager.getMetadataContainer(MetadataType.MESSAGE, false);
            container.getAllTransitiveKeyValues().forEach((headerKey, headerValue) -> exchange.getRequestHeaders().add(headerKey, headerValue));
            exchange.sendResponseHeaders(200, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(ret.getBytes());
            os.close();
        }
    }
}