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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tencent.polaris.api.utils.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.stream.Collectors;

public class HealthServer implements HttpHandler {

    private final boolean success;

    public HealthServer(boolean success) {
        this.success = success;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        Headers requestHeaders = httpExchange.getRequestHeaders();
        System.out.println(
                "received health check headers " + requestHeaders.keySet().stream()
                        .map(key -> key + "=" + requestHeaders.get(key))
                        .collect(Collectors.joining(", ", "{", "}")));
        InputStream requestBody = httpExchange.getRequestBody();
        byte[] buff = new byte[1024];
        int read = IOUtils.read(requestBody, buff, 0, 1024);
        String requestBodyStr = "";
        if (read > 0) {
            byte[] value = Arrays.copyOfRange(buff, 0, read);
            requestBodyStr = new String(value);
        }
        System.out.println("received health check body " + requestBodyStr);
        httpExchange.sendResponseHeaders(success ? 200 : 500, 0);
        OutputStream os = httpExchange.getResponseBody();
        os.write("OK".getBytes());
    }
}
