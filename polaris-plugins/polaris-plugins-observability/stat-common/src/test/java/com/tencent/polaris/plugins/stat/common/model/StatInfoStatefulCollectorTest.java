package com.tencent.polaris.plugins.stat.common.model;

import com.google.common.util.concurrent.AtomicDouble;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.polaris.plugins.stat.common.TestUtil.getRandomLabels;

public class StatInfoStatefulCollectorTest {

    @Test
    public void testCollectStatInfo() throws InterruptedException {
        Random random = new Random();
        StatInfoStatefulCollector<AtomicDouble> collector = new StatInfoStatefulCollector<AtomicDouble>();
        Map<String, String> labels = getRandomLabels();
        double threshold = 0.5;
        MetricValueAggregationStrategy<AtomicDouble> maxIncStrategy = new MaxThresholdIncStrategy(threshold);
        MetricValueAggregationStrategy<AtomicDouble> minIncStrategy = new MinThresholdIncStrategy(threshold);
        MetricValueAggregationStrategy<AtomicDouble>[] strategies= new MetricValueAggregationStrategy[]{
                maxIncStrategy, minIncStrategy
        };

        // 并发收集数据
        int count = random.nextInt(10) + 10;
        AtomicInteger maxIncExpected = new AtomicInteger();
        AtomicInteger minIncExpected = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                double n = random.nextDouble();
                if (n > threshold) {
                    maxIncExpected.incrementAndGet();
                } else {
                    minIncExpected.incrementAndGet();
                }
                collector.collectStatInfo(new AtomicDouble(n), labels, strategies);
                latch.countDown();
            }).start();
        }
        latch.await();

        // 验证
        StatMetric maxIncMetric = collector.getMetricContainer().get(
                AbstractSignatureStatInfoCollector.getSignature(maxIncStrategy.getStrategyName(), labels));
        StatMetric minIncMetric = collector.getMetricContainer().get(
                AbstractSignatureStatInfoCollector.getSignature(minIncStrategy.getStrategyName(), labels));
        Assert.assertEquals(maxIncExpected.get(), maxIncMetric.getValue(), 0);
        Assert.assertEquals(minIncExpected.get(), minIncMetric.getValue(), 0);
        Assert.assertEquals(minIncMetric.getValue() + maxIncMetric.getValue(), count, 0);
    }

    private static class MaxThresholdIncStrategy implements MetricValueAggregationStrategy<AtomicDouble> {

        private final double threshold;

        private MaxThresholdIncStrategy(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public String getStrategyDescription() {
            return "max threshold inc strategy";
        }

        @Override
        public String getStrategyName() {
            return "max_strategy";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, AtomicDouble dataSource) {
            if (dataSource.get() > threshold) {
                targetValue.incValue();
            }
        }

        @Override
        public double initMetricValue(AtomicDouble dataSource) {
            return dataSource.get() > threshold ? 1.0 : 0.0;
        }
    }

    private static class MinThresholdIncStrategy implements MetricValueAggregationStrategy<AtomicDouble> {

        private final double threshold;

        private MinThresholdIncStrategy(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public String getStrategyDescription() {
            return "min threshold inc strategy";
        }

        @Override
        public String getStrategyName() {
            return "min_strategy";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, AtomicDouble dataSource) {
            if (dataSource.get() <= threshold) {
                targetValue.incValue();
            }
        }

        @Override
        public double initMetricValue(AtomicDouble dataSource) {
            return dataSource.get() <= threshold ? 1.0 : 0.0;
        }
    }
}
