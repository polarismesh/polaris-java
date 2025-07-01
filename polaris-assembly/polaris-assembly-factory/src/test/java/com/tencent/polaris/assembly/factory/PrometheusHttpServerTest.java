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

package com.tencent.polaris.assembly.factory;


import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.config.global.AdminConfigImpl;
import com.tencent.polaris.factory.config.global.StatReporterConfigImpl;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;
import com.tencent.polaris.test.common.TestUtils;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class PrometheusHttpServerTest {

    @Test
    public void testHttpServerWithPort() throws IOException {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ((AdminConfigImpl) configuration.getGlobal().getAdmin()).setHost("0.0.0.0");
        ((AdminConfigImpl) configuration.getGlobal().getAdmin()).setPort(18080);
        StatReporterConfigImpl statReporterConfig = (StatReporterConfigImpl) configuration.getGlobal().getStatReporter();
        PrometheusHandlerConfig prometheusHandlerConfig = new PrometheusHandlerConfig();
        prometheusHandlerConfig.setPath("/metric");
        prometheusHandlerConfig.setType("pull");
        statReporterConfig.setPluginConfig(StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS, prometheusHandlerConfig);
        try (SDKContext sdkContext = SDKContext.initContextByConfig(configuration)) {
            sdkContext.init();
            URL metricsUrl = new URL("http://127.0.0.1:18080/metrics");
            HttpURLConnection metricsConn = (HttpURLConnection) metricsUrl.openConnection();
            metricsConn.setRequestMethod("GET");
            metricsConn.connect();
            assertThat(metricsConn.getResponseCode()).isEqualTo(200);
            metricsConn.disconnect();
        }
    }

    @Test
    public void testHttpServerRandomPort() throws IOException {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ((AdminConfigImpl) configuration.getGlobal().getAdmin()).setHost("0.0.0.0");
        ((AdminConfigImpl) configuration.getGlobal().getAdmin()).setPort(0);
        StatReporterConfigImpl statReporterConfig = (StatReporterConfigImpl) configuration.getGlobal().getStatReporter();
        PrometheusHandlerConfig prometheusHandlerConfig = new PrometheusHandlerConfig();
        prometheusHandlerConfig.setPath("/metric");
        prometheusHandlerConfig.setType("pull");
        statReporterConfig.setPluginConfig(StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS, prometheusHandlerConfig);
        try (SDKContext sdkContext = SDKContext.initContextByConfig(configuration)) {
            sdkContext.init();
            for (int i = 0; i < 3; i++) {
                URL metricsUrl = new URL(String.format("http://127.0.0.1:%d/metrics", i));
                HttpURLConnection metricsConn = (HttpURLConnection) metricsUrl.openConnection();
                metricsConn.setRequestMethod("GET");
                try {
                    metricsConn.connect();
                } catch (IOException e) {
                    continue;
                }
                assertThat(metricsConn.getResponseCode()).isEqualTo(200);
                metricsConn.disconnect();
            }
        }
    }

    @Test
    public void testHttpServerWithPath() throws IOException {
        Configuration configuration = TestUtils.configWithEnvAddress();
        ((AdminConfigImpl) configuration.getGlobal().getAdmin()).setHost("0.0.0.0");
        ((AdminConfigImpl) configuration.getGlobal().getAdmin()).setPort(18081);
        StatReporterConfigImpl statReporterConfig = (StatReporterConfigImpl) configuration.getGlobal().getStatReporter();
        PrometheusHandlerConfig prometheusHandlerConfig = new PrometheusHandlerConfig();
        prometheusHandlerConfig.setPath("/customMetrics");
        prometheusHandlerConfig.setType("pull");
        statReporterConfig.setPluginConfig(StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS, prometheusHandlerConfig);
        try (SDKContext sdkContext = SDKContext.initContextByConfig(configuration)) {
            sdkContext.init();
            URL metricsUrl = new URL("http://127.0.0.1:18081/customMetrics");
            HttpURLConnection metricsConn = (HttpURLConnection) metricsUrl.openConnection();
            metricsConn.setRequestMethod("GET");
            metricsConn.connect();
            assertThat(metricsConn.getResponseCode()).isEqualTo(200);
            metricsConn.disconnect();
        }
    }
}