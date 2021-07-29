package com.tencent.polaris.plugins.stat.common.model;

import com.google.common.util.concurrent.AtomicDouble;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.tencent.polaris.plugins.stat.common.TestUtil.getRandomLabels;

public class StatInfoRevisionCollectorTest {
    private StatInfoRevisionCollector<AtomicDouble> collector;
    private Map<String, String> labels;
    private MetricValueAggregationStrategy<AtomicDouble> strategy;
    private MetricValueAggregationStrategy<AtomicDouble>[] strategies;

    @Before
    public void setUp() {
        collector = new StatInfoRevisionCollector<AtomicDouble>();
        labels = getRandomLabels();
        strategy = new TestIncStrategy();
        strategies = new MetricValueAggregationStrategy[]{strategy};
    }

    @Test
    public void testCollectStatInfo() throws InterruptedException {
        // 并发收集数据
        int count = 10;
        addTestValues(count);
        StatRevisionMetric metric = collector.getMetricContainer().get(
                AbstractSignatureStatInfoCollector.getSignature(
                        strategy.getStrategyName(),
                        labels));
        Assert.assertEquals(metric.getValue(), count, 0);

        // 更新周期，并再次收集数据
        collector.incRevision();
        count = 20;
        addTestValues(count);
        Assert.assertEquals(metric.getValue(), count, 0);
        Assert.assertEquals(metric.getRevision(), collector.getCurrentRevision());
    }

    private void addTestValues(int count) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                collector.collectStatInfo(new AtomicDouble(1.0), labels, strategies);
                latch.countDown();
            }).start();
        }
        latch.await();
    }

    private static class TestIncStrategy implements MetricValueAggregationStrategy<AtomicDouble> {

        @Override
        public String getStrategyDescription() {
            return "test inc strategy";
        }

        @Override
        public String getStrategyName() {
            return "test_inc_strategy";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, AtomicDouble dataSource) {
            targetValue.addValue(dataSource.doubleValue());
        }

        @Override
        public double initMetricValue(AtomicDouble dataSource) {
            return dataSource.get();
        }
    }
}
