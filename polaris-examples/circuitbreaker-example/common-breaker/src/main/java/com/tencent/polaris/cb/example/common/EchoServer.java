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

package com.tencent.polaris.cb.example.common;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class EchoServer implements HttpHandler {

    private final InstanceRegisterRequest registerRequest;

    private final ServerType serverType;

    private final ProviderAPI providerAPI;

    private final AtomicBoolean abnormal = new AtomicBoolean(true);

    private final AtomicInteger requestCount = new AtomicInteger(0);

    public EchoServer(SDKContext sdkContext, InstanceRegisterRequest registerRequest, ServerType serverType) {
        this.registerRequest = registerRequest;
        this.serverType = serverType;
        this.providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(sdkContext);
    }

    private static Map<String, String> splitQuery(URI uri) {
        Map<String, String> query_pairs = new LinkedHashMap<>();
        String query = uri.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = "";
            String value = "";
            try {
                key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value)) {
                continue;
            }
            query_pairs.put(key, value);
        }
        return query_pairs;
    }

    private void handleNormal(HttpExchange exchange) throws IOException {
        Map<String, String> parameters = splitQuery(exchange.getRequestURI());
        String echoValue = parameters.get("value");
        String response = "echo: " + echoValue + ", port: " + registerRequest.getPort() + ", version " + registerRequest
                .getVersion();
        exchange.sendResponseHeaders(200, 0);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void handleConsecutive(HttpExchange exchange) throws IOException {
        if (abnormal.get()) {
            exchange.sendResponseHeaders(500, 0);
            String msg = "<p>consecutive error</p>";
            OutputStream os = exchange.getResponseBody();
            os.write(msg.getBytes());
            os.close();
        } else {
            handleNormal(exchange);
        }
    }

    private void handleErrorRate(HttpExchange exchange) throws IOException {
        if (abnormal.get() && requestCount.get() % 2 == 0) {
            exchange.sendResponseHeaders(500, 0);
            String msg = "<p>error rate error</p>";
            OutputStream os = exchange.getResponseBody();
            os.write(msg.getBytes());
            os.close();
        } else {
            handleNormal(exchange);
        }
    }

    // do the instance register
    public void register() {
        InstanceRegisterResponse registerResp = providerAPI.registerInstance(registerRequest);
        System.out.printf("register instance %s:%d to service %s(%s), id is %s%n",
                registerRequest.getHost(), registerRequest.getPort(), registerRequest.getService(),
                registerRequest.getNamespace(), registerResp.getInstanceId());
    }

    // do the instance deregister
    public void deregister() {
        InstanceDeregisterRequest deregisterRequest = new InstanceDeregisterRequest();
        deregisterRequest.setNamespace(registerRequest.getNamespace());
        deregisterRequest.setService(registerRequest.getService());
        deregisterRequest.setHost(registerRequest.getHost());
        deregisterRequest.setPort(registerRequest.getPort());
        providerAPI.deRegister(deregisterRequest);
        System.out.printf("deregister instance, address is %s:%d%n", registerRequest.getHost(),
                registerRequest.getPort());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        switch (serverType) {
            case NORMAL:
                handleNormal(exchange);
                break;
            case CONSECUTIVE:
                handleConsecutive(exchange);
                break;
            case ERROR_RATE:
                handleErrorRate(exchange);
                break;
            default:
                break;
        }
    }
}
