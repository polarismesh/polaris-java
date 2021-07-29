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

import com.tencent.polaris.api.plugin.stat.CircuitBreakGauge;
import com.tencent.polaris.api.plugin.stat.RateLimitGauge;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategy;
import com.tencent.polaris.plugins.stat.common.model.StatInfoHandler;
import com.tencent.polaris.plugins.stat.common.model.StatMetric;
import com.tencent.polaris.plugins.stat.common.model.StatInfoCollector;
import com.tencent.polaris.plugins.stat.common.model.StatInfoRevisionCollector;
import com.tencent.polaris.plugins.stat.common.model.StatInfoCollectorContainer;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategyCollections;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.PushGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricLabelOrder;
import static com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricName;
import static com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricValue.NULL_VALUE;

/**
 * 通过向Prometheus PushGateWay推送StatInfo消息来处理StatInfo。
 */
public class PrometheusPushHandler implements StatInfoHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PrometheusPushHandler.class);

    public static final int PUSH_INTERVALS = 30;
    public static final String PUSH_DEFAULT_ADDRESS = "127.0.0.1:9091";
    public static final String PUSH_DEFAULT_JOB_NAME = "defaultJobName";

    // schedule
    private final AtomicBoolean firstHandle = new AtomicBoolean(false);
    private ScheduledExecutorService scheduledPushTask;
    // storage
    private StatInfoCollectorContainer container;
    // push
    private final String callerIp;
    private final String jobName;
    private final long pushIntervalS;
    private final String pushAddress;
    private final CollectorRegistry promRegistry;
    private final Map<String, Gauge> sampleMapping;
    private final PushAddressProvider addressProvider;
    private PushGateway pushGateway;

    /**
     * 构造函数
     *
     * @param callerIp      调用者Ip
     * @param jobName       向PushGateWay推送使用任务名称
     * @param pushIntervalS 向PushGateWay推送的时间间隔
     * @param provider      push的地址提供者
     */
    public PrometheusPushHandler(String callerIp, String jobName, Long pushIntervalS, PushAddressProvider provider) {
        this.callerIp = callerIp;
        this.container = new StatInfoCollectorContainer();
        this.sampleMapping = new HashMap<>();
        this.promRegistry = new CollectorRegistry(true);
        this.addressProvider = provider;
        if (null != jobName) {
            this.jobName = jobName;
        } else {
            this.jobName = PUSH_DEFAULT_JOB_NAME;
        }
        if (null != provider && null != provider.getAddress()) {
            this.pushAddress = provider.getAddress();
        } else {
            this.pushAddress = PUSH_DEFAULT_ADDRESS;
        }
        if (null != pushIntervalS) {
            this.pushIntervalS = pushIntervalS;
        } else {
            this.pushIntervalS = PUSH_INTERVALS;
        }
        this.pushGateway = new PushGateway(this.pushAddress);
        this.scheduledPushTask = Executors.newSingleThreadScheduledExecutor();

        initSampleMapping(MetricValueAggregationStrategyCollections.SERVICE_CALL_STRATEGY,
                SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER);
        initSampleMapping(MetricValueAggregationStrategyCollections.RATE_LIMIT_STRATEGY,
                SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER);
        initSampleMapping(MetricValueAggregationStrategyCollections.CIRCUIT_BREAK_STRATEGY,
                SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER);
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
    public void handle(StatInfo statInfo) {
        if (firstHandle.compareAndSet(false, true)) {
            startSchedulePushTask();
        }

        if (null == statInfo) {
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
                    convertInsGaugeToLabels(instanceGauge),
                    MetricValueAggregationStrategyCollections.SERVICE_CALL_STRATEGY);
        }
    }

    public void handleRateLimitGauge(RateLimitGauge rateLimitGauge) {
        if (null != container && null != container.getRateLimitCollector()) {
            container.getRateLimitCollector().collectStatInfo(rateLimitGauge,
                    convertRateLimitGaugeToLabels(rateLimitGauge),
                    MetricValueAggregationStrategyCollections.RATE_LIMIT_STRATEGY);
        }
    }

    public void handleCircuitBreakGauge(CircuitBreakGauge circuitBreakGauge) {
        if (null != container && null != container.getCircuitBreakerCollector()) {
            container.getCircuitBreakerCollector().collectStatInfo(circuitBreakGauge,
                    convertCircuitBreakToLabels(circuitBreakGauge),
                    MetricValueAggregationStrategyCollections.CIRCUIT_BREAK_STRATEGY);
        }
    }

    @Override
    public void stopHandle() {
        if (container != null) {
            container = null;
        }

        if (scheduledPushTask != null) {
            scheduledPushTask.shutdown();
            scheduledPushTask = null;
        }
    }

    private void startSchedulePushTask() {
        if (null != container && null != scheduledPushTask && null != pushGateway && null != sampleMapping) {
            this.scheduledPushTask.scheduleWithFixedDelay(this::doPush,
                    pushIntervalS,
                    pushIntervalS,
                    TimeUnit.SECONDS);
        }
    }

    private void doPush() {
        try {
            putDataFromContainerInOrder(container.getInsCollector().getCollectedValues(),
                    SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER);
            putDataFromContainerInOrder(container.getRateLimitCollector().getCollectedValues(),
                    SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER);
            putDataFromContainerInOrder(container.getCircuitBreakerCollector().getCollectedValues(),
                    SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER);

            try {
                pushGateway.pushAdd(promRegistry, jobName);
                LOG.info("push result to push-gateway {} success", pushAddress);
            } catch (IOException exception) {
                LOG.error("push result to push-gateway {} encountered exception, exception:{}", pushAddress,
                        exception.getMessage());

                if (null != addressProvider) {
                    String newAddress = addressProvider.getAddress();
                    if (newAddress != null && !pushAddress.equals(newAddress)) {
                        pushGateway = new PushGateway(newAddress);
                    }
                }
                return;
            }

            for (StatInfoCollector<?, ? extends StatMetric> s : container.getCollectors()) {
                if (s instanceof StatInfoRevisionCollector<?>) {
                    long currentRevision = ((StatInfoRevisionCollector<?>) s).incRevision();
                    LOG.debug("RevisionCollector inc current revision to {}", currentRevision);
                }
            }
        } catch (Exception e) {
            LOG.error("push result to push-gateway {} encountered exception, exception:{}", pushAddress,
                    e.getMessage());
            e.printStackTrace();
        }
    }

    private void putDataFromContainerInOrder(Collection<? extends StatMetric> values, String[] order) {
        if (null == values) {
            return;
        }

        for (StatMetric s : values) {
            Gauge gauge = sampleMapping.get(s.getMetricName());
            if (null != gauge) {
                Gauge.Child child = gauge.labels(getOrderedMetricLabelValues(s.getLabels(), order));
                if (null != child) {
                    child.set(s.getValue());
                }
            }
        }
    }

    public static String[] getOrderedMetricLabelValues(Map<String, String> labels, String[] orderedKey) {
        String[] orderValue = new String[orderedKey.length];
        for (int i = 0; i < orderedKey.length; i++) {
            orderValue[i] = labels.getOrDefault(orderedKey[i], NULL_VALUE);
        }
        return orderValue;
    }

    protected Map<String, String> convertInsGaugeToLabels(InstanceGauge insGauge) {
        Map<String, String> labels = new HashMap<>();
        for (String labelName : SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER) {
            switch (labelName) {
                case SystemMetricName.CALLEE_NAMESPACE:
                    addLabel(labelName, insGauge.getNamespace(), labels);
                    break;
                case SystemMetricModel.SystemMetricName.CALLEE_SERVICE:
                    addLabel(labelName, insGauge.getService(), labels);
                    break;
                case SystemMetricName.CALLEE_SUBSET:
                    addLabel(labelName, insGauge.getSubset(), labels);
                    break;
                case SystemMetricModel.SystemMetricName.CALLEE_INSTANCE:
                    addLabel(labelName, buildAddress(insGauge.getHost(), insGauge.getPort()), labels);
                    break;
                case SystemMetricName.CALLEE_RET_CODE:
                    String retCodeStr =
                            null == insGauge.getRetCode() ? null : insGauge.getRetCode().toString();
                    addLabel(labelName, retCodeStr, labels);
                    break;
                case SystemMetricName.CALLER_LABELS:
                    addLabel(labelName, insGauge.getLabels(), labels);
                    break;
                case SystemMetricName.CALLER_NAMESPACE:
                    String namespace =
                            null == insGauge.getCallerService() ? null : insGauge.getCallerService().getNamespace();
                    addLabel(labelName, namespace, labels);
                    break;
                case SystemMetricName.CALLER_SERVICE:
                    String serviceName =
                            null == insGauge.getCallerService() ? null : insGauge.getCallerService().getService();
                    addLabel(labelName, serviceName, labels);
                    break;
                case SystemMetricName.CALLER_IP:
                    addLabel(labelName, callerIp, labels);
                    break;
                default:
            }
        }

        return labels;
    }

    protected Map<String, String> convertRateLimitGaugeToLabels(RateLimitGauge rateLimitGauge) {
        Map<String, String> labels = new HashMap<>();
        for (String labelName : SystemMetricModel.SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER) {
            switch (labelName) {
                case SystemMetricName.CALLEE_NAMESPACE:
                    addLabel(labelName, rateLimitGauge.getNamespace(), labels);
                    break;
                case SystemMetricName.CALLEE_SERVICE:
                    addLabel(labelName, rateLimitGauge.getService(), labels);
                    break;
                case SystemMetricName.CALLEE_METHOD:
                    addLabel(labelName, rateLimitGauge.getMethod(), labels);
                    break;
                case SystemMetricName.CALLER_LABELS:
                    addLabel(labelName, rateLimitGauge.getLabels(), labels);
                    break;
                default:
            }
        }

        return labels;
    }

    protected Map<String, String> convertCircuitBreakToLabels(CircuitBreakGauge gauge) {
        Map<String, String> labels = new HashMap<>();
        for (String labelName : SystemMetricModel.SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER) {
            switch (labelName) {
                case SystemMetricName.CALLEE_NAMESPACE:
                    addLabel(labelName, gauge.getNamespace(), labels);
                    break;
                case SystemMetricName.CALLEE_SERVICE:
                    addLabel(labelName, gauge.getService(), labels);
                    break;
                case SystemMetricName.CALLEE_METHOD:
                    addLabel(labelName, gauge.getMethod(), labels);
                    break;
                case SystemMetricName.CALLEE_SUBSET:
                    addLabel(labelName, gauge.getSubset(), labels);
                    break;
                case SystemMetricName.CALLEE_INSTANCE:
                    addLabel(labelName, buildAddress(gauge.getHost(), gauge.getPort()), labels);
                    break;
                case SystemMetricName.CALLER_NAMESPACE:
                    String namespace =
                            null == gauge.getCallerService() ? null : gauge.getCallerService().getNamespace();
                    addLabel(labelName, namespace, labels);
                    break;
                case SystemMetricName.CALLER_SERVICE:
                    String serviceName =
                            null == gauge.getCallerService() ? null : gauge.getCallerService().getService();
                    addLabel(labelName, serviceName, labels);
                    break;
                case SystemMetricName.CALLER_IP:
                    addLabel(labelName, callerIp, labels);
                    break;
                default:
            }
        }

        return labels;
    }

    private void addLabel(String key, String value, Map<String, String> target) {
        if (null == key) {
            return;
        }

        if (null == value) {
            value = NULL_VALUE;
        }

        target.put(key, value);
    }

    private static String buildAddress(String host, int port) {
        if (null == host) {
            host = NULL_VALUE;
        }

        return host + ":" + port;
    }

    protected void setPushGateway(PushGateway pushGateway) {
        this.pushGateway = pushGateway;
    }

    protected CollectorRegistry getPromRegistry() {
        return promRegistry;
    }
}
