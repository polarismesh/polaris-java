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

import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.server.ReportClientRequest;
import com.tencent.polaris.api.plugin.server.ReportClientResponse;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.plugin.stat.ReporterMetaInfo;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.stat.common.model.StatInfoHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHttpServer;
import com.tencent.polaris.version.Version;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * PrometheusReporter plugin
 *
 * @author wallezhang
 */
public class PrometheusReporter implements StatReporter, PluginConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusReporter.class);
    private PrometheusHandler statInfoHandler;
    private PrometheusHttpServer httpServer;
    private ScheduledExecutorService reportClientExecutor;

    @Override
    public void init(InitContext initContext) throws PolarisException {
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        if (extensions.getConfiguration().getGlobal().getStatReporter().isEnable()) {
            PrometheusHandlerConfig config = extensions.getConfiguration().getGlobal().getStatReporter()
                    .getPluginConfig(getName(), PrometheusHandlerConfig.class);
            // If port is -1, then disable prometheus http server
            if (config.getPort() == -1) {
                return;
            }
            String host =
                    StringUtils.isBlank(config.getHost()) ? extensions.getValueContext().getHost() : config.getHost();
            statInfoHandler = new PrometheusHandler(host);
            httpServer = new PrometheusHttpServer(host, config.getPort(), statInfoHandler.getPromRegistry());
            reportClientExecutor = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory(getName()));
            reportClient(extensions);
        }
    }

    @Override
    public void reportStat(StatInfo statInfo) {
        if (null != statInfoHandler && null != statInfo) {
            statInfoHandler.handle(statInfo);
        }
    }

    @Override
    public ReporterMetaInfo metaInfo() {
        return ReporterMetaInfo.builder().protocol("http").path(httpServer.getPath()).host(httpServer.getHost())
                .port(httpServer.getPort()).target(getName()).build();
    }

    @Override
    public String getName() {
        return StatReporterConfig.DEFAULT_REPORTER_PROMETHEUS;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return PrometheusHandlerConfig.class;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.STAT_REPORTER.getBaseType();
    }

    @Override
    public void destroy() {
        if (null != statInfoHandler) {
            statInfoHandler.stopHandle();
            statInfoHandler = null;
        }
        if (reportClientExecutor != null) {
            reportClientExecutor.shutdown();
            reportClientExecutor = null;
        }
        if (httpServer != null) {
            httpServer.stopServer();
            httpServer = null;
        }
    }

    /**
     * Report prometheus http server metadata periodic
     *
     * @param extensions extensions
     */
    private void reportClient(Extensions extensions) {
        if (reportClientExecutor != null) {
            reportClientExecutor.scheduleAtFixedRate(() -> {
                ServerConnector serverConnector = extensions.getServerConnector();
                ReportClientRequest reportClientRequest = new ReportClientRequest();
                reportClientRequest.setClientHost(extensions.getValueContext().getHost());
                reportClientRequest.setVersion(Version.VERSION);
                reportClientRequest.setReporterMetaInfos(Collections.singletonList(metaInfo()));
                try {
                    ReportClientResponse reportClientResponse = serverConnector.reportClient(reportClientRequest);
                    LOGGER.debug("Report prometheus http server metadata success, response:{}", reportClientResponse);
                } catch (PolarisException e) {
                    LOGGER.error("Report prometheus http server info exception.", e);
                }
            }, 0L, 60L, TimeUnit.SECONDS);
        }

    }
}
