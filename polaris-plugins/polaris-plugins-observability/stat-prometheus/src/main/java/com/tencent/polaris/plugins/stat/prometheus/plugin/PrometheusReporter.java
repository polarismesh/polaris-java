/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.sun.net.httpserver.HttpHandler;
import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.HttpServerAware;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.stat.*;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.remote.ServiceAddressRepository;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.stat.common.model.*;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricLabelOrder;
import com.tencent.polaris.plugins.stat.prometheus.exporter.PushGateway;
import com.tencent.polaris.plugins.stat.prometheus.handler.CommonHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.HttpMetricHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * PrometheusReporter plugin
 *
 * @author wallezhang
 */
public class PrometheusReporter implements StatReporter, PluginConfigProvider, HttpServerAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusReporter.class);

    public static final Integer DEFAULT_MIN_PULL_PORT = 28080;

    private static final String DEFAULT_PATH = "/metrics";

    private PrometheusHandlerConfig config;

    private final AtomicBoolean firstHandle = new AtomicBoolean(false);

    private CollectorRegistry promRegistry;

    private Map<String, Gauge> sampleMapping;

    private StatInfoCollectorContainer container;

    private String sdkIP;

    private String instanceID;

    private ScheduledExecutorService executorService;

    private Extensions extensions;

    private boolean enable;

    private int port;

    private String path;

    private Map<String, HttpHandler> handlers;

    private ServiceAddressRepository serviceAddressRepository;

    private final Map<String, PushGateway> pushGatewayMap = new ConcurrentHashMap<>();

    private final List<Pattern> pathRegexPatternList = new ArrayList<>();

    public PrometheusReporter() {
        this.container = new StatInfoCollectorContainer();
        this.sampleMapping = new HashMap<>();
        this.promRegistry = new CollectorRegistry(true);
        initSampleMapping(MetricValueAggregationStrategyCollections.SERVICE_CALL_STRATEGY,
                SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER);
        initSampleMapping(MetricValueAggregationStrategyCollections.CIRCUIT_BREAK_STRATEGY,
                SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER);
        initSampleMapping(MetricValueAggregationStrategyCollections.RATE_LIMIT_STRATEGY,
                SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER);
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
        this.sdkIP = extensions.getValueContext().getHost();
        this.enable = extensions.getConfiguration().getGlobal().getStatReporter().isEnable();
        this.executorService = Executors.newScheduledThreadPool(4, new NamedThreadFactory(getName()));
        this.port = extensions.getConfiguration().getGlobal().getAdmin().getPort();

        this.serviceAddressRepository = new ServiceAddressRepository(Collections.singletonList(this.config.getAddress()),
                extensions.getValueContext().getClientId(), extensions, new ServiceKey(config.getNamespace(), config.getService()));

        if (CollectionUtils.isNotEmpty(config.getPathRegexList())) {
            for (String pathRegex : config.getPathRegexList()) {
                pathRegexPatternList.add(Pattern.compile(pathRegex));
            }
        }

        this.initHandle();
    }

    void initHandle() {
        if (!enable) {
            return;
        }
        if (firstHandle.compareAndSet(false, true)) {
            if (Objects.equals(config.getType(), "push")) {
                startSchedulePushTask();
            } else {
                startScheduleAggregationTask();
            }
        }
    }

    @Override
    public void reportStat(StatInfo statInfo) {
        if (!enable) {
            return;
        }
        if (Objects.isNull(statInfo)) {
            return;
        }
        handle(statInfo);
    }

    public void handle(StatInfo statInfo) {
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
                    CommonHandler.convertInsGaugeToLabels(instanceGauge, sdkIP, pathRegexPatternList),
                    MetricValueAggregationStrategyCollections.SERVICE_CALL_STRATEGY);
        }
    }

    public void handleCircuitBreakGauge(CircuitBreakGauge circuitBreakGauge) {
        if (null != container && null != container.getCircuitBreakerCollector()) {
            container.getCircuitBreakerCollector().collectStatInfo(circuitBreakGauge,
                    CommonHandler.convertCircuitBreakToLabels(circuitBreakGauge, sdkIP, pathRegexPatternList),
                    MetricValueAggregationStrategyCollections.CIRCUIT_BREAK_STRATEGY);
        }
    }

    public void handleRateLimitGauge(RateLimitGauge rateLimitGauge) {
        if (null != container && null != container.getRateLimitCollector()) {
            container.getRateLimitCollector().collectStatInfo(rateLimitGauge,
                    CommonHandler.convertRateLimitGaugeToLabels(rateLimitGauge, pathRegexPatternList),
                    MetricValueAggregationStrategyCollections.RATE_LIMIT_STRATEGY);
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
        if (!enable || Objects.equals(config.getType(), "push") || Objects.isNull(handlers)) {
            return ReporterMetaInfo.builder().build();
        }
        Node httpServerNode = extensions.getHttpServerNode();
        return ReporterMetaInfo.builder().
                protocol("http").
                path(path).
                host(IPAddressUtils.getIpCompatible(httpServerNode.getHost())).
                port(httpServerNode.getPort()).
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
    }

    private void startScheduleAggregationTask() {
        // If port is -1, then disable prometheus http server and metrics report
        if (port < 0) {
            // 如果启用了 pull 模式，但是没有对外暴露 prometheus http-server，则效果还是 disable 状态
            enable = false;
            LOGGER.info("[Metrics][Prometheus] port < 0, disable run prometheus http-server and metrics report");
            return;
        }
        if (port == 0) {
            port = DEFAULT_MIN_PULL_PORT;
        }
        handlers = new HashMap<>();
        HttpMetricHandler metricHandler = new HttpMetricHandler(promRegistry);
        handlers.put("/-/healthy", metricHandler);
        path = config.getPath();
        if (StringUtils.isBlank(path)) {
            path = DEFAULT_PATH;
        }
        handlers.put(path, metricHandler);
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
        CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getCircuitBreakerCollector(),
                0,
                SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER);
        CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getRateLimitCollector(),
                container.getRateLimitCollector().getCurrentRevision(),
                SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER);
        for (StatInfoCollector<?, ? extends StatMetric> s : container.getCollectors()) {
            if (s instanceof StatInfoRevisionCollector<?>) {
                long currentRevision = ((StatInfoRevisionCollector<?>) s).incRevision();
                LOGGER.debug("RevisionCollector inc current revision to {}", currentRevision);
            }
        }
    }


    private void startSchedulePushTask() {
        if (null != serviceAddressRepository && null != container && null != executorService && null != sampleMapping) {
            this.executorService.scheduleWithFixedDelay(this::doPush,
                    config.getPushInterval(),
                    config.getPushInterval(),
                    TimeUnit.MILLISECONDS);
            LOGGER.info("start schedule push task, task interval {}", config.getPushInterval());
        }
    }

    private void doPush() {
        String address = serviceAddressRepository.getServiceAddress();
        try {
            CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getInsCollector(),
                    container.getInsCollector().getCurrentRevision(),
                    SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER);
            CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getCircuitBreakerCollector(),
                    0,
                    SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER);
            CommonHandler.putDataFromContainerInOrder(sampleMapping, container.getRateLimitCollector(),
                    container.getRateLimitCollector().getCurrentRevision(),
                    SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER);
            try {
                PushGateway pushGateway;
                if (pushGatewayMap.containsKey(address)) {
                    pushGateway = pushGatewayMap.get(address);
                } else {
                    pushGateway = pushGatewayMap.computeIfAbsent(address, k -> new PushGateway(address));
                    LOGGER.info("init push-gateway {} ", address);
                }
                if (config.isOpenGzip()) {
                    pushGateway.pushAddByGzip(promRegistry, CommonHandler.PUSH_DEFAULT_JOB_NAME,
                            Collections.singletonMap(CommonHandler.PUSH_GROUP_KEY, instanceID));
                } else {
                    pushGateway.pushAdd(promRegistry, CommonHandler.PUSH_DEFAULT_JOB_NAME,
                            Collections.singletonMap(CommonHandler.PUSH_GROUP_KEY, instanceID));
                }

                LOGGER.info("push result to push-gateway {} success, open gzip {}", address, config.isOpenGzip());
            } catch (IOException exception) {
                LOGGER.error("push result to push-gateway {} open gzip {} encountered exception, exception:{}",
                        address, config.isOpenGzip(), exception.getMessage());
                pushGatewayMap.remove(address);
                return;
            }

            for (StatInfoCollector<?, ? extends StatMetric> s : container.getCollectors()) {
                if (s instanceof StatInfoRevisionCollector<?>) {
                    long currentRevision = ((StatInfoRevisionCollector<?>) s).incRevision();
                    LOGGER.debug("RevisionCollector inc current revision to {}", currentRevision);
                }
            }
        } catch (Exception e) {
            LOGGER.error("push result to push-gateway {} open gzip {} encountered exception, exception:{}",
                    address, config.isOpenGzip(), e.getMessage());
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

    String getSdkIP() {
        return sdkIP;
    }

    void setSdkIP(String sdkIP) {
        this.sdkIP = sdkIP;
    }

    void setEnable(boolean enable) {
        this.enable = enable;
    }

    void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public Map<String, HttpHandler> getHandlers() {
        if (!enable) {
            return Collections.emptyMap();
        }
        return handlers;
    }

    @JustForTest
    void setServiceAddressRepository(ServiceAddressRepository serviceAddressRepository) {
        this.serviceAddressRepository = serviceAddressRepository;
    }

    @JustForTest
    Map<String, PushGateway> getPushGatewayMap() {
        return pushGatewayMap;
    }
}
