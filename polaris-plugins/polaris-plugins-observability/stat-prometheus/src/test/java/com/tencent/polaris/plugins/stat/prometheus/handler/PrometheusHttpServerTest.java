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


import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import org.junit.After;
import org.junit.Test;

public class PrometheusHttpServerTest {

    private PrometheusHttpServer httpServer;

    @After
    public void tearDown() throws Exception {
        httpServer.stopServer();
    }

    @Test
    public void testHttpServerWithPort() throws IOException {
        httpServer = new PrometheusHttpServer("127.0.0.1", 28080);
        URL metricsUrl = new URL("http://127.0.0.1:28080/metrics");
        HttpURLConnection metricsConn = (HttpURLConnection) metricsUrl.openConnection();
        metricsConn.setRequestMethod("GET");
        metricsConn.connect();
        assertThat(metricsConn.getResponseCode()).isEqualTo(200);
        metricsConn.disconnect();
    }

    @Test
    public void testHttpServerRandomPort() throws IOException {
        httpServer = new PrometheusHttpServer("127.0.0.1", 0);
        int port = httpServer.getPort();
        assertThat(port).isGreaterThan(20000).isLessThan(65535);
        URL metricsUrl = new URL("http://127.0.0.1:" + port + "/metrics");
        HttpURLConnection metricsConn = (HttpURLConnection) metricsUrl.openConnection();
        metricsConn.setRequestMethod("GET");
        metricsConn.connect();
        assertThat(metricsConn.getResponseCode()).isEqualTo(200);
        metricsConn.disconnect();
    }

    @Test
    public void testHttpServerWithPath() throws IOException {
        httpServer = new PrometheusHttpServer("127.0.0.1", 28080, "/customMetrics");
        URL metricsUrl = new URL("http://127.0.0.1:28080/customMetrics");
        HttpURLConnection metricsConn = (HttpURLConnection) metricsUrl.openConnection();
        metricsConn.setRequestMethod("GET");
        metricsConn.connect();
        assertThat(metricsConn.getResponseCode()).isEqualTo(200);
        metricsConn.disconnect();
    }
}