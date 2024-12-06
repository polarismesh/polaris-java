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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StatInfoRevisionCollectorTest {

    private StatInfoRevisionCollector<AtomicLong> collector;
    private Map<String, String> labels;
    private MetricValueAggregationStrategy<AtomicLong> strategy;
    private MetricValueAggregationStrategy<AtomicLong>[] strategies;

    @Before
    public void setUp() {
        collector = new StatInfoRevisionCollector<AtomicLong>();
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
                collector.collectStatInfo(new AtomicLong(1), labels, strategies);
                latch.countDown();
            }).start();
        }
        latch.await();
    }

    private static class TestIncStrategy implements MetricValueAggregationStrategy<AtomicLong> {

        @Override
        public String getStrategyDescription() {
            return "test inc strategy";
        }

        @Override
        public String getStrategyName() {
            return "test_inc_strategy";
        }

        @Override
        public void updateMetricValue(StatMetric targetValue, AtomicLong dataSource) {
            targetValue.addValue(dataSource.longValue());
        }

        @Override
        public double initMetricValue(AtomicLong dataSource) {
            return dataSource.get();
        }
    }
}
