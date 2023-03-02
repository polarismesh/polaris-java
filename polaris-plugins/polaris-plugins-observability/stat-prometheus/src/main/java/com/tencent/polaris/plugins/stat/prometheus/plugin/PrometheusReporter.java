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
import com.tencent.polaris.api.plugin.stat.CircuitBreakGauge;
import com.tencent.polaris.api.plugin.stat.RateLimitGauge;
import com.tencent.polaris.api.plugin.stat.ReporterMetaInfo;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategy;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategyCollections;
import com.tencent.polaris.plugins.stat.common.model.StatInfoCollector;
import com.tencent.polaris.plugins.stat.common.model.StatInfoCollectorContainer;
import com.tencent.polaris.plugins.stat.common.model.StatInfoRevisionCollector;
import com.tencent.polaris.plugins.stat.common.model.StatMetric;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricLabelOrder;
import com.tencent.polaris.plugins.stat.prometheus.handler.CommonHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHttpServer;
import com.tencent.polaris.version.Version;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;

/**
 * PrometheusReporter plugin
 *
 * @author wallezhang
 */
public class PrometheusReporter implements StatReporter, PluginConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusReporter.class);

    private PrometheusHandlerConfig config;

    private final AtomicBoolean firstHandle = new AtomicBoolean(false);

    private CollectorRegistry promRegistry;

    private Map<String, Gauge> sampleMapping;

    private StatInfoCollectorContainer container;

    private String callerIp;

    private String instanceID;

    private PrometheusHttpServer httpServer;

    private ScheduledExecutorService executorService;

    private PushGateway pushGateway;

    private Extensions extensions;

    public PrometheusReporter() {
        this.container = new StatInfoCollectorContainer();
        this.sampleMapping = new HashMap<>();
        this.promRegistry = new CollectorRegistry(true);
        initSampleMapping(MetricValueAggregationStrategyCollections.SERVICE_CALL_STRATEGY,
                SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER);
        initSampleMapping(MetricValueAggregationStrategyCollections.RATE_LIMIT_STRATEGY,
                SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER);
        initSampleMapping(MetricValueAggregationStrategyCollections.CIRCUIT_BREAK_STRATEGY,
                SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER);
    }

    @Override
    public void init(InitContext initContext) throws PolarisException {
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        this.extensions = extensions;
        this.config = extensions.getConfiguration().getGlobal().getStatReporter()
                .getPluginConfig(getName(), PrometheusHandlerConfig.class);
        this.instanceID = extensions.getValueContext().getClientId();
        this.callerIp = StringUtils.isBlank(config.getHost()) ? extensions.getValueContext().getHost() : config.getHost();
        this.initHandle();
    }

    void initHandle() {
        this.executorService = Executors.newScheduledThreadPool(4, new NamedThreadFactory(getName()));
        if (firstHandle.compareAndSet(false, true)) {
            if (Objects.equals(config.getType(), "push")) {
                startSchedulePushTask();
            } else {
                startScheduleAggregationTask();
                reportClient(extensions);
            }
        }
    }

    @Override
    public void reportStat(StatInfo statInfo) {
        if (Objects.isNull(statInfo)) {
            return;
        }
        handle(statInfo);
    }

    public void handle(StatInfo statInfo) {
        if (Objects.isNull(statInfo)) {
            return;
        }
        if (null != statInfo.getRouterGauge()) {
            handleRouterGauge(statInfo.getRouterGauge());
        }
        if (null != statInfo.getCircuitBreakGauge()) {
            handleCircuitBreakGauge(statInfo.getCircuitBreakGauge());
        }
        if (null != statInfo.getRateLimitGauge()) {
            handleRateLimitGauge(statInfo.getRateLimitGauge());
        }
    }

    public void handleRouterGauge(InstanceGauge instanceGauge) {
        if (null != container && null != container.getInsCollector()) {
            container.getInsCollector().collectStatInfo(instanceGauge,
                    CommonHandler.convertInsGaugeToLabels(instanceGauge, callerIp),
                    MetricValueAggregationStrategyCollections.SERVICE_CALL_STRATEGY);
        }
    }

    public void handleRateLimitGauge(RateLimitGauge rateLimitGauge) {
        if (null != container && null != container.getRateLimitCollector()) {
            container.getRateLimitCollector().collectStatInfo(rateLimitGauge,
                    CommonHandler.convertRateLimitGaugeToLabels(rateLimitGauge),
                    MetricValueAggregationStrategyCollections.RATE_LIMIT_STRATEGY);
        }
    }

    public void handleCircuitBreakGauge(CircuitBreakGauge circuitBreakGauge) {
        if (null != container && null != container.getCircuitBreakerCollector()) {
            container.getCircuitBreakerCollector().collectStatInfo(circuitBreakGauge,
                    CommonHandler.convertCircuitBreakToLabels(circuitBreakGauge, callerIp),
                    MetricValueAggregationStrategyCollections.CIRCUIT_BREAK_STRATEGY);
        }
    }

    private void initSampleMapping(MetricValueAggregationStrategy<?>[] strategies, String[] order) {
        for (MetricValueAggregationStrategy<?> strategy : strategies) {
            Gauge strategyGauge = new Gauge.Builder()
                    .name(strategy.getStrategyName())
                    .help(strategy.getStrategyDescription())
                    .labelNames(order)
                    .create()
                    .register(promRegistry);
            sampleMapping.put(strategy.getStrategyName(), strategyGauge);
        }
    }

    @Override
    public ReporterMetaInfo metaInfo() {
        if (Objects.equals(config.getType(), "push")) {
            return ReporterMetaInfo.builder().
                    build();
        }
        return ReporterMetaInfo.builder().
                protocol("http").
                path(httpServer.getPath()).
                host(httpServer.getHost()).
                port(httpServer.getPort()).
                target(getName()).
                build();
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
        if (Objects.isNull(config)) {
            return;
        }
        if (Objects.nonNull(executorService)) {
            executorService.shutdown();
        }
        if (Objects.nonNull(httpServer)) {
            httpServer.stopServer();
        }
    }

    /**
     * Report prometheus http server metadata periodic
     *
     * @param extensions extensions
     */
    private void reportClient(Extensions extensions) {
        if (executorService != null) {
            executorService.scheduleAtFixedRate(() -> {
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

    private void startScheduleAggregationTask() {
        // If port is -1, then disable prometheus http server
        if (config.getPort() == -1) {
            LOGGER.info("[Metrics][Prometheus] port == -1, disable run prometheus http-server");
            return;
        }
        httpServer = new PrometheusHttpServer(config.getHost(), config.getPort(), promRegistry);
        if (null != container && null != executorService && null != sampleMapping) {
            this.executorService.scheduleWithFixedDelay(this::doAggregation,
                    CommonHandler.DEFAULT_INTERVAL_MILLI,
                    CommonHandler.DEFAULT_INTERVAL_MILLI,
                    TimeUnit.MILLISECONDS);
            LOGGER.info("start schedule metric aggregation task, task interval {}", CommonHandler.DEFAULT_INTERVAL_MILLI);
        }
    }

    private void doAggregation() {
        CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getInsCollector(),
                container.getInsCollector().getCurrentRevision(),
                SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER);
        CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getRateLimitCollector(),
                container.getRateLimitCollector().getCurrentRevision(),
                SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER);
        CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getCircuitBreakerCollector(),
                0,
                SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER);

        for (StatInfoCollector<?, ? extends StatMetric> s : container.getCollectors()) {
            if (s instanceof StatInfoRevisionCollector<?>) {
                long currentRevision = ((StatInfoRevisionCollector<?>) s).incRevision();
                LOGGER.debug("RevisionCollector inc current revision to {}", currentRevision);
            }
        }
    }


    private void startSchedulePushTask() {
        if (StringUtils.isBlank(config.getAddress())) {
            List<String> addresses = extensions.getConfiguration().getGlobal().getServerConnector().getAddresses();
            if (CollectionUtils.isNotEmpty(addresses)) {
                String address = addresses.get(0);
                config.setAddress(address.split(":")[0] + ":" + 9091);
            }
        }

        if (null != container && null != executorService && null != sampleMapping) {
            this.executorService.scheduleWithFixedDelay(this::doPush,
                    config.getPushInterval(),
                    config.getPushInterval(),
                    TimeUnit.MILLISECONDS);
            LOGGER.info("start schedule push task, task interval {}", config.getPushInterval());
        }
    }

    private void doPush() {
        try {
            CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getInsCollector(),
                    container.getInsCollector().getCurrentRevision(),
                    SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER);
            CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getRateLimitCollector(),
                    container.getRateLimitCollector().getCurrentRevision(),
                    SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER);
            CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getCircuitBreakerCollector(),
                    0,
                    SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER);

            try {
                if (Objects.isNull(pushGateway)) {
                    LOGGER.info("init push-gateway {} ", config.getAddress());
                    pushGateway = new PushGateway(config.getAddress());
                }

                pushGateway.pushAdd(promRegistry, CommonHandler.PUSH_DEFAULT_JOB_NAME,
                        Collections.singletonMap(CommonHandler.PUSH_GROUP_KEY, instanceID));
                LOGGER.info("push result to push-gateway {} success", config.getAddress());
            } catch (IOException exception) {
                LOGGER.error("push result to push-gateway {} encountered exception, exception:{}", config.getAddress(),
                        exception.getMessage());
                pushGateway = null;
                return;
            }

            for (StatInfoCollector<?, ? extends StatMetric> s : container.getCollectors()) {
                if (s instanceof StatInfoRevisionCollector<?>) {
                    long currentRevision = ((StatInfoRevisionCollector<?>) s).incRevision();
                    LOGGER.debug("RevisionCollector inc current revision to {}", currentRevision);
                }
            }
        } catch (Exception e) {
            LOGGER.error("push result to push-gateway {} encountered exception, exception:{}", config.getAddress(),
                    e.getMessage());
        }
    }

    public PrometheusHandlerConfig getConfig() {
        return config;
    }

    public void setConfig(PrometheusHandlerConfig config) {
        this.config = config;
    }

    public CollectorRegistry getPromRegistry() {
        return promRegistry;
    }

    public void setPromRegistry(CollectorRegistry promRegistry) {
        this.promRegistry = promRegistry;
    }

    public PushGateway getPushGateway() {
        return pushGateway;
    }

    public void setPushGateway(PushGateway pushGateway) {
        this.pushGateway = pushGateway;
    }

    public String getCallerIp() {
        return callerIp;
    }

    public void setCallerIp(String callerIp) {
        this.callerIp = callerIp;
    }
}
