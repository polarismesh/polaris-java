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

package com.tencent.polaris.plugins.stat.common.model;

import static com.tencent.polaris.plugins.stat.common.TestUtil.getRandomLabels;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Test;

public class StatInfoStatefulCollectorTest {

    @Test
    public void testCollectStatInfo() throws InterruptedException {
        Random random = new Random();
        StatInfoStatefulCollector<AtomicLong> collector = new StatInfoStatefulCollector<AtomicLong>();
        Map<String, String> labels = getRandomLabels();
        double threshold = 0.5;
        MetricValueAggregationStrategy<AtomicLong> maxIncStrategy = new MaxThresholdIncStrategy(threshold);
        MetricValueAggregationStrategy<AtomicLong> minIncStrategy = new MinThresholdIncStrategy(threshold);
        MetricValueAggregationStrategy<AtomicLong>[] strategies = new MetricValueAggregationStrategy[]{
                maxIncStrategy, minIncStrategy
        };

        // 并发收集数据
        int count = random.nextInt(10) + 10;
        AtomicInteger maxIncExpected = new AtomicInteger();
        AtomicInteger minIncExpected = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                long n = random.nextLong();
                if (n > threshold) {
                    maxIncExpected.incrementAndGet();
                } else {
                    minIncExpected.incrementAndGet();
                }
                collector.collectStatInfo(new AtomicLong(n), labels, strategies);
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

    private static class MaxThresholdIncStrategy implements MetricValueAggregationStrategy<AtomicLong> {

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
        public void updateMetricValue(StatMetric targetValue, AtomicLong dataSource) {
            if (dataSource.get() > threshold) {
                targetValue.incValue();
            }
        }

        @Override
        public double initMetricValue(AtomicLong dataSource) {
            return dataSource.get() > threshold ? 1.0 : 0.0;
        }
    }

    private static class MinThresholdIncStrategy implements MetricValueAggregationStrategy<AtomicLong> {

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
        public void updateMetricValue(StatMetric targetValue, AtomicLong dataSource) {
            if (dataSource.get() <= threshold) {
                targetValue.incValue();
            }
        }

        @Override
        public double initMetricValue(AtomicLong dataSource) {
            return dataSource.get() <= threshold ? 1.0 : 0.0;
        }
    }
}
