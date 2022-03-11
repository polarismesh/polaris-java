package com.tencent.polaris.plugins.stat.prometheus.handler;

import static com.tencent.polaris.plugins.stat.prometheus.handler.PrometheusPushHandler.REVISION_MAX_SCOPE;

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
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategy;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategyCollections;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel;
import com.tencent.polaris.plugins.stat.common.model.SystemMetricModel.SystemMetricLabelOrder;
import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.PushGateway;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for {@link PrometheusPushHandler}
 *
 * @author Haotian Zhang
 */
public class PrometheusPushHandlerTest {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusPushHandlerTest.class);

    // Avoid pushing to local push gateway.
    private static final String PUSH_DEFAULT_ADDRESS = "127.0.0.1:19091";

    private final Random random = new Random();
    private PrometheusPushHandler handler;
    private long pushInterval;

    @Before
    public void setUp() {
        String callerIp = "127.0.0.1";
        pushInterval = 2 * 1000;
        PrometheusPushHandlerConfig pushHandlerConfig = new PrometheusPushHandlerConfig();
        pushHandlerConfig.setPushInterval(pushInterval);
        pushHandlerConfig.setPushgatewayAddress(PUSH_DEFAULT_ADDRESS);
        MockPushGateway pgw = new MockPushGateway(PUSH_DEFAULT_ADDRESS);
        handler = new PrometheusPushHandler(callerIp, pushHandlerConfig,
                new ServiceDiscoveryProvider(null, pushHandlerConfig), "default", callerIp);
        handler.setPushGateway(pgw);
    }

    @Test
    public void testPushNullStatInfo() throws InterruptedException {
        StatInfo statInfo = new StatInfo();
        handler.handle(statInfo);
        handler.handle(null);

        Thread.sleep(pushInterval + 1000);
        handler.stopHandle();
    }

    @Test
    public void testPushGatewayDelayInit() throws InterruptedException {
        handler.setPushGateway(null);
        handler.handle(null);

        Thread.sleep(pushInterval + 1000);
        handler.stopHandle();

        // null cause IOException
        Thread.sleep(pushInterval + 1000);
        Assert.assertNull(handler.getPushGateway());
    }

    @Test
    public void testPushInstanceGaugeConcurrency() throws InterruptedException {
        batchDone(() -> {
            StatInfo statInfo = new StatInfo();
            statInfo.setRouterGauge(mockServiceCallResult());
            handler.handle(statInfo);
        }, 10);

        Thread.sleep(pushInterval + 1000);
        handler.stopHandle();
    }

    @Test
    public void testExpiredDataClean() throws InterruptedException {
        int count = 5;
        StatInfo statInfo = new StatInfo();
        ServiceCallResult callResult = mockServiceCallResult();
        statInfo.setRouterGauge(callResult);
        batchDone(() -> handler.handle(statInfo), count);

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

        LOG.info("mock sleep {} times end...", REVISION_MAX_SCOPE);
        Thread.sleep(pushInterval * REVISION_MAX_SCOPE + 1000);
        result = getServiceCallTotalResult(callResult);
        Assert.assertNull(result);

        // stop handle
        handler.stopHandle();
    }

    @Test
    public void testServiceCallTotalStrategy() throws InterruptedException {
        StatInfo statInfo = new StatInfo();
        ServiceCallResult callResult = mockServiceCallResult();
        statInfo.setRouterGauge(callResult);
        int count = 10;
        batchDone(() -> handler.handle(statInfo), count);

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.stopHandle();

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
                    handler.handle(statInfo);
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
                    handler.handle(statInfo);
                }, count - expected);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            latch.countDown();
        }).start();
        latch.await();

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.stopHandle();

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
            handler.handle(statInfo);
        }, count);

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.stopHandle();

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
    public void testRateLimitStrategy() throws InterruptedException {
        int count = 10;
        int passedNum = 3;
        CountDownLatch latch = new CountDownLatch(2);
        new Thread(() -> {
            try {
                batchDone(() -> {
                    StatInfo statInfo = new StatInfo();
                    DefaultRateLimitResult rateLimitResult = mockFixedRateLimitResult(RateLimitGauge.Result.PASSED);
                    statInfo.setRateLimitGauge(rateLimitResult);
                    handler.handle(statInfo);
                }, passedNum);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            latch.countDown();
        }).start();
        new Thread(() -> {
            try {
                batchDone(() -> {
                    StatInfo statInfo = new StatInfo();
                    DefaultRateLimitResult rateLimitResult = mockFixedRateLimitResult(RateLimitGauge.Result.LIMITED);
                    statInfo.setRateLimitGauge(rateLimitResult);
                    handler.handle(statInfo);
                }, count - passedNum);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            latch.countDown();
        }).start();
        latch.await();

        Thread.sleep(pushInterval + 1000);
        handler.stopHandle();

        DefaultRateLimitResult example = mockFixedRateLimitResult(RateLimitGauge.Result.LIMITED);
        // assert result
        Double totalResult = getRateLimitTotalResult(example);
        Double passResult = getRateLimitPassedResult(example);
        Double limitResult = getRateLimitLimitedResult(example);
        Assert.assertEquals(new Double(count), totalResult);
        Assert.assertEquals(new Double(passedNum), passResult);
        Assert.assertEquals(new Double(count - passedNum), limitResult);
    }

    @Test
    public void testCircuitBreakerOpen() throws InterruptedException {
        changeCircuitBreakerStatus(mockFixedCircuitResult(CircuitBreakerStatus.Status.OPEN));

        // mock pushing
        Thread.sleep(pushInterval + 1000);
        handler.stopHandle();

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
        handler.stopHandle();

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
        handler.stopHandle();

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
        handler.stopHandle();
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
        handler.stopHandle();
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
        String[] labelValues = PrometheusPushHandler.getOrderedMetricLabelValues(
                getServiceCallLabels(strategy, example, handler), labelKeys);
        return registry.getSampleValue(strategy.getStrategyName(), labelKeys, labelValues);
    }

    private Double getRateLimitTotalResult(DefaultRateLimitResult example) {
        return getRateLimitResult(example,
                new MetricValueAggregationStrategyCollections.RateLimitRequestTotalStrategy());
    }

    private Double getRateLimitPassedResult(DefaultRateLimitResult example) {
        return getRateLimitResult(example,
                new MetricValueAggregationStrategyCollections.RateLimitRequestPassStrategy());
    }

    private Double getRateLimitLimitedResult(DefaultRateLimitResult example) {
        return getRateLimitResult(example,
                new MetricValueAggregationStrategyCollections.RateLimitRequestLimitStrategy());
    }

    private Double getRateLimitResult(DefaultRateLimitResult example,
            MetricValueAggregationStrategy<RateLimitGauge> strategy) {
        CollectorRegistry registry = handler.getPromRegistry();
        String[] labelKeys = SystemMetricLabelOrder.RATELIMIT_GAUGE_LABEL_ORDER;
        String[] labelValues = PrometheusPushHandler.getOrderedMetricLabelValues(
                getRateLimitLabels(strategy, example, handler), labelKeys);
        return registry.getSampleValue(strategy.getStrategyName(), labelKeys, labelValues);
    }

    private void changeCircuitBreakerStatus(DefaultCircuitBreakResult toStatus) {
        StatInfo statInfo = new StatInfo();
        statInfo.setCircuitBreakGauge(toStatus);
        handler.handle(statInfo);
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
        String[] labelValues = PrometheusPushHandler.getOrderedMetricLabelValues(
                getCircuitBreakerLabels(strategy, example, handler), labelKeys);
        return registry.getSampleValue(strategy.getStrategyName(), labelKeys, labelValues);
    }

    private Map<String, String> getServiceCallLabels(MetricValueAggregationStrategy<InstanceGauge> strategy,
            InstanceGauge gauge,
            PrometheusPushHandler handler) {
        Map<String, String> labels = handler.convertInsGaugeToLabels(gauge);
        labels.put(SystemMetricModel.SystemMetricName.METRIC_NAME_LABEL, strategy.getStrategyName());
        return labels;
    }

    private Map<String, String> getRateLimitLabels(MetricValueAggregationStrategy<RateLimitGauge> strategy,
            RateLimitGauge gauge,
            PrometheusPushHandler handler) {
        Map<String, String> labels = handler.convertRateLimitGaugeToLabels(gauge);
        labels.put(SystemMetricModel.SystemMetricName.METRIC_NAME_LABEL, strategy.getStrategyName());
        return labels;
    }

    private Map<String, String> getCircuitBreakerLabels(MetricValueAggregationStrategy<CircuitBreakGauge> strategy,
            CircuitBreakGauge gauge,
            PrometheusPushHandler handler) {
        Map<String, String> labels = handler.convertCircuitBreakToLabels(gauge);
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
                "mockCB", status, System.currentTimeMillis(), 0);
        circuitBreakResult.setCircuitBreakStatus(circuitBreakerStatus);
        return circuitBreakResult;
    }

    private DefaultRateLimitResult mockFixedRateLimitResult(RateLimitGauge.Result result) {
        DefaultRateLimitResult rateLimitResult = new DefaultRateLimitResult();
        rateLimitResult.setMethod("GET");
        rateLimitResult.setLabels("a:b|c:d");
        rateLimitResult.setService("callService");
        rateLimitResult.setNamespace("callNamespace");
        rateLimitResult.setResult(result);
        return rateLimitResult;
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

        public void pushAdd(CollectorRegistry registry, String job, Map<String, String> groupingKey) {
            LOG.info("mock push-gateway push with groupKey...");
            Enumeration<Collector.MetricFamilySamples> enumeration = registry.metricFamilySamples();
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