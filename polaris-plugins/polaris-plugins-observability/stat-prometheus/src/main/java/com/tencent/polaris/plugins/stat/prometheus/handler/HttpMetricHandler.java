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

package com.tencent.polaris.plugins.stat.prometheus.handler;

import static com.tencent.polaris.api.config.global.StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.slf4j.Logger;

/**
 * Handle prometheus http server metric requests
 *
 * @author wallezhang
 */
public class HttpMetricHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(HttpMetricHandler.class);

    private final CollectorRegistry collectorRegistry;
    private final LocalByteArray response = new LocalByteArray();

    public HttpMetricHandler() {
        this.collectorRegistry = CollectorRegistry.defaultRegistry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String contextPath = exchange.getHttpContext().getPath();
        ByteArrayOutputStream response = this.response.get();
        response.reset();
        OutputStreamWriter osw = new OutputStreamWriter(response, StandardCharsets.UTF_8);
        if ("/-/healthy".equals(contextPath)) {
            osw.write("Exporter is Healthy.");
        } else {
            String contentType = TextFormat.chooseContentType(exchange.getRequestHeaders().getFirst("Accept"));
            exchange.getResponseHeaders().set("Content-Type", contentType);
            TextFormat.writeFormat(contentType, osw,
                    this.collectorRegistry.filteredMetricFamilySamples(parseQuery(query)));
        }

        osw.close();
        if (shouldUseCompression(exchange)) {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, 0L);
            GZIPOutputStream os = new GZIPOutputStream(exchange.getResponseBody());

            try {
                response.writeTo(os);
            } finally {
                os.close();
            }
        } else {
            exchange.getResponseHeaders().set("Content-Length", String.valueOf(response.size()));
            exchange.sendResponseHeaders(200, response.size());
            response.writeTo(exchange.getResponseBody());
        }
        LOG.info("Metrics is pulled by " + DEFAULT_REPORTER_PROMETHEUS);
        exchange.close();
    }

    private boolean shouldUseCompression(HttpExchange exchange) {
        List<String> encodingHeaders = exchange.getRequestHeaders().get("Accept-Encoding");
        if (encodingHeaders != null) {
            for (String encodingHeader : encodingHeaders) {
                String[] encodings = encodingHeader.split(",");
                for (String encoding : encodings) {
                    if ("gzip".equalsIgnoreCase(encoding.trim())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Set<String> parseQuery(String query) throws IOException {
        Set<String> names = new HashSet<>();
        if (StringUtils.isNotBlank(query)) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String decodeKey = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                if (idx != -1 && "name[]".equals(decodeKey)) {
                    names.add(URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
                }
            }
        }

        return names;
    }

    private static class LocalByteArray extends ThreadLocal<ByteArrayOutputStream> {

        private LocalByteArray() {
        }

        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(1048576);
        }
    }
}
