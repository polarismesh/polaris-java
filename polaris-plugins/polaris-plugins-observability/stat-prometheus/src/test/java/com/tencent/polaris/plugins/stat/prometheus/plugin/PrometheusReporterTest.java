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

package com.tencent.polaris.plugins.stat.prometheus.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHttpServer;
import io.prometheus.client.CollectorRegistry;
import java.util.concurrent.ScheduledExecutorService;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PrometheusReporterTest {

    @Mock
    private StatReporterConfig statReporterConfig;
    private PrometheusHandlerConfig handlerConfig;
    private PrometheusReporter reporter;
    @Mock
    private Extensions extensions;

    @Before
    public void setUp() throws Exception {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        ValueContext valueContext = new ValueContext();
        handlerConfig = new PrometheusHandlerConfig();
        valueContext.setHost(SDKContext.parseHost(configuration));
        valueContext.setServerConnectorProtocol(SDKContext.parseServerConnectorProtocol(configuration));
        when(extensions.getConfiguration()).thenReturn(configuration);
        when(extensions.getValueContext()).thenReturn(valueContext);
        reporter = new PrometheusReporter();
    }

    @After
    public void tearDown() throws Exception {
        CollectorRegistry.defaultRegistry.clear();
        reporter.destroy();
    }

    @Test
    public void testDefaultPort() {
        reporter.postContextInit(extensions);
        assertThat(reporter).extracting("httpServer").isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.type(PrometheusHttpServer.class))
                .hasFieldOrPropertyWithValue("port", 28080).hasFieldOrPropertyWithValue("host", "127.0.0.1");
        assertThat(reporter).extracting("statInfoHandler").isNotNull()
                .asInstanceOf(InstanceOfAssertFactories.type(PrometheusHandler.class))
                .hasFieldOrPropertyWithValue("callerIp", "127.0.0.1");
        assertThat(reporter).extracting("reportClientExecutor").isNotNull().asInstanceOf(InstanceOfAssertFactories.type(
                ScheduledExecutorService.class));
    }
}