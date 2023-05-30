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

import com.tencent.polaris.api.plugin.stat.CircuitBreakGauge;
import com.tencent.polaris.api.plugin.stat.DefaultCircuitBreakResult;
import com.tencent.polaris.api.plugin.stat.DefaultRateLimitResult;
import com.tencent.polaris.api.plugin.stat.RateLimitGauge;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategy;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategyCollections;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricLabelOrder;
import com.tencent.polaris.plugins.stat.prometheus.exporter.PushGateway;
import com.tencent.polaris.plugins.stat.prometheus.handler.CommonHandler;
import com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusHandlerConfig;
import io.prometheus.client.Collector;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PrometheusReporterTest {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusReporterTest.class);

    // Avoid pushing to local push gateway.
    private static final String PUSH_DEFAULT_ADDRESS = "127.0.0.1:19091";

    private final Random random = new Random();
    private PrometheusReporter handler;
    private long pushInterval;

    @Before
    public void setUp() {
        pushInterval = 2 * 1000;
        PrometheusHandlerConfig config = new PrometheusHandlerConfig();
        config.setType("push");
        config.setPushInterval(pushInterval);
        config.setAddress(PUSH_DEFAULT_ADDRESS);
        MockPushGateway pgw = new MockPushGateway(PUSH_DEFAULT_ADDRESS);
        handler = new PrometheusReporter();
        handler.setEnable(true);
        handler.setSdkIP("127.0.0.1");
        handler.setConfig(config);
        handler.setPushGateway(pgw);
        handler.initHandle();
    }

    @Test
    public void testPushNullStatInfo() throws InterruptedException {
        StatInfo statInfo = new StatInfo();
        handler.reportStat(statInfo);
        handler.reportStat(null);

        Thread.sleep(pushInterval + 1000);
        handler.destroy();
    }

    @Test
    public void testPushInstanceGaugeConcurrency() throws InterruptedException {
        batchDone(() -> {
            StatInfo statInfo = new StatInfo();
            statInfo.setRouterGauge(mockServiceCallResult());
            handler.reportStat(statInfo);
        }, 10);

        Thread.sleep(pushInterval + 1000);
        handler.destroy();
    }

    @Test
    public void testExpiredDataClean() throws InterruptedException {
        int count = 5;
        StatInfo statInfo = new StatInfo();
        ServiceCallResult callResult = mockServiceCallResult();
        statInfo.setRouterGauge(callResult);
        batchDone(() -> handler.reportStat(statInfo), count);

        // mock push
        LOG.info("first mock push finish...");
        Thread.sleep(pushInterval + 1000);
        Double result = getServiceCallTotalResult(callResult);
        Assert.assertEquals(new Double(count), result);

        // mock next push
        LOG.info("second mock push finish...");
        Thread.sleep(pushInterval + 1000);
        result = getServiceCallTotalResult(callResult);
        Assert.assertEquals(new Double(0), result);

        LOG.info("mock sleep {} times end...", CommonHandler.REVISION_MAX_SCOPE);
        Thread.sleep(pushInterval * CommonHandler.REVISION_MAX_SCOPE + 1000);
        result = getServiceCallTotalResult(callResult);
        Assert.assertNull(result);

        // stop handle
        handler.destroy();
    }

    @Test
    public void testServiceCallTotalStrategy() throws InterruptedException {
        StatInfo statInfo = new StatInfo();
        ServiceCallResult callResult = mockServiceCallResult();
        statInfo.setRouterGauge(callResult);
        int count = 10;
        batchDone(() -> handler.reportStat(statInfo), count);

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.destroy();

        Double result = getServiceCallTotalResult(callResult);
        Assert.assertEquals(new Double(count), result);
    }

    @Test
    public void testServiceCallSuccessStrategy() throws InterruptedException {
        int count = 10;
        int expected = 3;
        CountDownLatch latch = new CountDownLatch(2);
        new Thread(() -> {
            try {
                batchDone(() -> {
                    StatInfo statInfo = new StatInfo();
                    ServiceCallResult callResult = mockFixedLabelServiceCallResult(200, 1000);
                    callResult.setRetStatus(RetStatus.RetSuccess);
                    statInfo.setRouterGauge(callResult);
                    handler.reportStat(statInfo);
                }, expected);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            latch.countDown();
        }).start();
        new Thread(() -> {
            try {
                batchDone(() -> {
                    StatInfo statInfo = new StatInfo();
                    ServiceCallResult callResult = mockFixedLabelServiceCallResult(200, 1000);
                    callResult.setRetStatus(RetStatus.RetFail);
                    statInfo.setRouterGauge(callResult);
                    handler.reportStat(statInfo);
                }, count - expected);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            latch.countDown();
        }).start();
        latch.await();

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.destroy();

        ServiceCallResult example = mockFixedLabelServiceCallResult(200, 1000);
        Double result = getServiceCallSuccessResult(example);
        Assert.assertEquals(new Double(expected), result);
    }

    @Test
    public void testServiceCallSumAndMaxStrategy() throws InterruptedException {
        List<Integer> delayList = Collections.synchronizedList(new ArrayList<>());
        int count = 20;
        batchDone(() -> {
            int delay = random.nextInt(1000) + 100;
            delayList.add(delay);

            StatInfo statInfo = new StatInfo();
            ServiceCallResult callResult = mockFixedLabelServiceCallResult(200, delay);
            statInfo.setRouterGauge(callResult);
            handler.reportStat(statInfo);
        }, count);

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.destroy();

        int maxExpected = 0;
        int sumExpected = 0;
        for (Integer i : delayList) {
            if (i > maxExpected) {
                maxExpected = i;
            }
            sumExpected += i;
        }

        ServiceCallResult example = mockFixedLabelServiceCallResult(200, 1000);
        Double maxResult = getServiceCallMaxResult(example);
        Double sumResult = getServiceCallSumResult(example);
        Assert.assertEquals(new Double(maxExpected), maxResult);
        Assert.assertEquals(new Double(sumExpected), sumResult);
    }

    @Test
    public void testCircuitBreakerOpen() throws InterruptedException {
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.destroy();

        DefaultCircuitBreakResult example = mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN);
        Assert.assertEquals(new Double(1), getOpenResult(example));
        Assert.assertEquals(new Double(0), getHalfOpenResult(example));
    }

    @Test
    public void testCircuitBreakerOpenToHalf() throws InterruptedException {
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.HALF_OPEN));

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.destroy();

        DefaultCircuitBreakResult example = mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN);
        Assert.assertEquals(new Double(0), getOpenResult(example));
        Assert.assertEquals(new Double(1), getHalfOpenResult(example));
    }

    @Test
    public void testCircuitBreakerOpenToClose() throws InterruptedException {
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.HALF_OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.CLOSE));

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.destroy();

        DefaultCircuitBreakResult example = mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN);
        Assert.assertEquals(new Double(0), getOpenResult(example));
        Assert.assertEquals(new Double(0), getHalfOpenResult(example));
    }

    @Test
    public void testCircuitBreakerRepeatOpenToHalfOpen() throws InterruptedException {
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.HALF_OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.HALF_OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));

        Thread.sleep(pushInterval + 1000);
        handler.destroy();
        DefaultCircuitBreakResult example = mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN);
        Assert.assertEquals(new Double(1), getOpenResult(example));
        Assert.assertEquals(new Double(0), getHalfOpenResult(example));
    }

    @Test
    public void testCircuitBreakerCloseToOpen() throws InterruptedException {
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.HALF_OPEN));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.CLOSE));
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.destroy();
        DefaultCircuitBreakResult example = mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN);
        Assert.assertEquals(new Double(1), getOpenResult(example));
        Assert.assertEquals(new Double(0), getHalfOpenResult(example));
    }

    private void batchDone(Runnable runnable, int count) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                runnable.run();
                latch.countDown();
            }).start();
        }

        latch.await();
    }

    private Double getServiceCallTotalResult(ServiceCallResult example) {
        return getServiceCallResult(example,
                new MetricValueAggregationStrategyCollections.UpstreamRequestTotalStrategy());
    }

    private Double getServiceCallSuccessResult(ServiceCallResult example) {
        return getServiceCallResult(example,
                new MetricValueAggregationStrategyCollections.UpstreamRequestSuccessStrategy());
    }

    private Double getServiceCallSumResult(ServiceCallResult example) {
        return getServiceCallResult(example,
                new MetricValueAggregationStrategyCollections.UpstreamRequestTimeoutStrategy());
    }

    private Double getServiceCallMaxResult(ServiceCallResult example) {
        return getServiceCallResult(example,
                new MetricValueAggregationStrategyCollections.UpstreamRequestMaxTimeoutStrategy());
    }

    private Double getServiceCallResult(ServiceCallResult example,
                                        MetricValueAggregationStrategy<InstanceGauge> strategy) {
        CollectorRegistry registry = handler.getPromRegistry();
        String[] labelKeys = SystemMetricLabelOrder.INSTANCE_GAUGE_LABEL_ORDER;
        String[] labelValues = CommonHandler.getOrderedMetricLabelValues(
                getServiceCallLabels(strategy, example), labelKeys);
        return registry.getSampleValue(strategy.getStrategyName(), labelKeys, labelValues);
    }

    private void changeCircuitBreakerStatus(DefaultCircuitBreakResult toStatus) {
        StatInfo statInfo = new StatInfo();
        statInfo.setCircuitBreakGauge(toStatus);
        handler.reportStat(statInfo);
    }

    private Double getOpenResult(DefaultCircuitBreakResult example) {
        return getCircuitBreakerResult(example,
                new MetricValueAggregationStrategyCollections.CircuitBreakerOpenStrategy());
    }

    private Double getHalfOpenResult(DefaultCircuitBreakResult example) {
        return getCircuitBreakerResult(example,
                new MetricValueAggregationStrategyCollections.CircuitBreakerHalfOpenStrategy());
    }

    private Double getCircuitBreakerResult(DefaultCircuitBreakResult example,
                                           MetricValueAggregationStrategy<CircuitBreakGauge> strategy) {
        CollectorRegistry registry = handler.getPromRegistry();
        String[] labelKeys = SystemMetricLabelOrder.CIRCUIT_BREAKER_LABEL_ORDER;
        String[] labelValues = CommonHandler.getOrderedMetricLabelValues(
                getCircuitBreakerLabels(strategy, example), labelKeys);
        return registry.getSampleValue(strategy.getStrategyName(), labelKeys, labelValues);
    }

    private Map<String, String> getServiceCallLabels(MetricValueAggregationStrategy<InstanceGauge> strategy,
                                                     InstanceGauge gauge) {
        Map<String, String> labels = CommonHandler.convertInsGaugeToLabels(gauge, handler.getSdkIP());
        labels.put(SystemMetricModel.SystemMetricName.METRIC_NAME_LABEL, strategy.getStrategyName());
        return labels;
    }

    private Map<String, String> getCircuitBreakerLabels(MetricValueAggregationStrategy<CircuitBreakGauge> strategy,
                                                        CircuitBreakGauge gauge) {
        Map<String, String> labels = CommonHandler.convertCircuitBreakToLabels(gauge, handler.getSdkIP());
        labels.put(SystemMetricModel.SystemMetricName.METRIC_NAME_LABEL, strategy.getStrategyName());
        return labels;
    }

    private DefaultCircuitBreakResult mockFixedCircuitResult(CircuitBreakerStatus.Status status) {
        DefaultCircuitBreakResult circuitBreakResult = new DefaultCircuitBreakResult();
        circuitBreakResult.setService("callService");
        circuitBreakResult.setNamespace("callNamespace");
        circuitBreakResult.setMethod("GET");
        circuitBreakResult.setSubset("callSubset");
        circuitBreakResult.setHost("callHost");
        circuitBreakResult.setPort(8080);
        circuitBreakResult.setInstanceId("callInstanceId");
        circuitBreakResult.setCallerService(mockCallerService());
        CircuitBreakerStatus circuitBreakerStatus = new CircuitBreakerStatus(
                "mockCB", status, System.currentTimeMillis());
        circuitBreakResult.setCircuitBreakStatus(circuitBreakerStatus);
        return circuitBreakResult;
    }

    private ServiceCallResult mockServiceCallResult() {
        return mockFixedLabelServiceCallResult(random.nextInt(300) + 200, random.nextInt(10000));
    }

    private ServiceCallResult mockFixedLabelServiceCallResult(int retCode, int Delay) {
        ServiceCallResult serviceCallResult = new ServiceCallResult();
        serviceCallResult.setService("callService");
        serviceCallResult.setNamespace("callNamespace");
        serviceCallResult.setHost("callHost");
        serviceCallResult.setPort(8080);
        serviceCallResult.setRetStatus(RetStatus.RetSuccess);
        serviceCallResult.setRetCode(retCode);
        serviceCallResult.setDelay(Delay);
        serviceCallResult.setMethod("GET");
        serviceCallResult.setCallerService(mockCallerService());
        return serviceCallResult;
    }

    private ServiceInfo mockCallerService() {
        ServiceInfo caller = new ServiceInfo();
        caller.setService("consumer");
        caller.setNamespace("Production");
        return caller;
    }

    private static class MockPushGateway extends PushGateway {

        public MockPushGateway(String address) {
            super(address);
        }

        public void pushAddByGzip(CollectorRegistry registry, String job, Map<String, String> groupingKey) {
            LOG.info("mock push-gateway push with groupKey...");
            Enumeration<MetricFamilySamples> enumeration = registry.metricFamilySamples();
            if (null == enumeration) {
                return;
            }

            if (enumeration.hasMoreElements()) {
                while (enumeration.hasMoreElements()) {
                    Collector.MetricFamilySamples samples = enumeration.nextElement();
                    if (samples.samples.isEmpty()) {
                        LOG.info("mock pgw-{} metric name {} no sample.", super.gatewayBaseURL, samples.name);
                        continue;
                    }
                    for (Collector.MetricFamilySamples.Sample sample : samples.samples) {
                        LOG.info("mock pgw-{} exposed sample: {}", super.gatewayBaseURL, sample);
                    }
                }
            }
        }

        public void pushAdd(CollectorRegistry registry, String job, Map<String, String> groupingKey) {
            LOG.info("mock push-gateway push with groupKey...");
            Enumeration<MetricFamilySamples> enumeration = registry.metricFamilySamples();
            if (null == enumeration) {
                return;
            }

            if (enumeration.hasMoreElements()) {
                while (enumeration.hasMoreElements()) {
                    Collector.MetricFamilySamples samples = enumeration.nextElement();
                    if (samples.samples.isEmpty()) {
                        LOG.info("mock pgw-{} metric name {} no sample.", super.gatewayBaseURL, samples.name);
                        continue;
                    }
                    for (Collector.MetricFamilySamples.Sample sample : samples.samples) {
                        LOG.info("mock pgw-{} exposed sample: {}", super.gatewayBaseURL, sample);
                    }
                }
            }
        }
    }
}