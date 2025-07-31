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

package com.ecwid.consul.transport;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * Transport for TTL.
 *
 * @author Haotian Zhang
 */
public class TtlHttpTransport extends AbstractHttpTransport {

    private final HttpClient httpClient;

    static final int DEFAULT_TTL_READ_TIMEOUT = 5 * 1000; // 5s
    static final int DEFAULT_CONNECTION_TIMEOUT = 5 * 1000; // 5s

    public TtlHttpTransport() {
        this(DEFAULT_TTL_READ_TIMEOUT);
    }

    public TtlHttpTransport(int ttlReadTimeout) {
        if (ttlReadTimeout == 0) {
            ttlReadTimeout = DEFAULT_TTL_READ_TIMEOUT;
        }
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(DEFAULT_MAX_CONNECTIONS);
        connectionManager.setDefaultMaxPerRoute(DEFAULT_MAX_PER_ROUTE_CONNECTIONS);

        RequestConfig requestConfig = RequestConfig.custom().
                setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT).
                setConnectionRequestTimeout(DEFAULT_CONNECTION_TIMEOUT).
                setSocketTimeout(ttlReadTimeout).
                build();

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().
                setConnectionManager(connectionManager).
                setDefaultRequestConfig(requestConfig).
                useSystemProperties();

        this.httpClient = httpClientBuilder.build();
    }

    @Override
    public HttpClient getHttpClient() {
        return httpClient;
    }
}
