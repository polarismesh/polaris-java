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

package com.tencent.polaris.client.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HttpServerUtils {

    public static void writeTextToHttpServer(HttpExchange exchange, String text, int code) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            byteArrayOutputStream.write(bytes, 0, bytes.length);
            exchange.sendResponseHeaders(code, byteArrayOutputStream.size());
            byteArrayOutputStream.writeTo(exchange.getResponseBody());
            exchange.close();
        }
    }
}
