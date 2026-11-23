/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

import com.tencent.polaris.api.pojo.InstanceGauge;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for minimum delay collection with missing samples.
 *
 * @author fuyuwei01
 */
public class MinimumDelayCollectorTest {

    private static final long VALID_DELAY = 950L;

    private static final long LOWER_DELAY = 900L;

    private static final int THREAD_COUNT = 3;

    private static final long TIMEOUT_SECONDS = 5L;

    /**
     * 验证首次及连续 null 不创建指标，后续有效时延正确初始化.
     */
    @Test
    public void testStatefulNullThenValidDelay() {
        assertNullThenValid(new StatInfoStatefulCollector<InstanceGauge>());
    }

    /**
     * 验证有周期的指标在 null 后使用首个有效时延初始化.
     */
    @Test
    public void testRevisionNullThenValidDelay() {
        assertNullThenValid(new StatInfoRevisionCollector<InstanceGauge>());
    }

    /**
     * 验证无周期指标的有效零时延不被当作缺失样本.
     */
    @Test
    public void testStatefulZeroThenNullDelay() {
        assertZeroThenNull(new StatInfoStatefulCollector<InstanceGauge>());
    }

    /**
     * 验证有周期指标的有效零时延不被覆盖.
     */
    @Test
    public void testRevisionZeroThenNullDelay() {
        assertZeroThenNull(new StatInfoRevisionCollector<InstanceGauge>());
    }

    /**
     * 验证周期切换后的 null 不重置指标，首个有效时延初始化新周期.
     */
    @Test
    public void testNullDoesNotResetRevision() {
        StatInfoRevisionCollector<InstanceGauge> collector = new StatInfoRevisionCollector<>();
        collect(collector, LOWER_DELAY);
        StatRevisionMetric metric = collector.getCollectedValues().iterator().next();
        long initialRevision = metric.getRevision();
        collector.incRevision();

        collect(collector, null);

        Assertions.assertThat(metric.getRevision()).isEqualTo(initialRevision);
        Assertions.assertThat(metric.getValue()).isEqualTo((double) LOWER_DELAY);
        collect(collector, VALID_DELAY);
        Assertions.assertThat(metric.getRevision()).isEqualTo(collector.getCurrentRevision());
        Assertions.assertThat(metric.getValue()).isEqualTo((double) VALID_DELAY);
    }

    /**
     * 验证全 null 周期不会创建虚假的零值指标.
     */
    @Test
    public void testAllNullPeriods() {
        StatInfoRevisionCollector<InstanceGauge> collector = new StatInfoRevisionCollector<>();
        collect(collector, null);
        collector.incRevision();
        collect(collector, null);

        Assertions.assertThat(collector.getCollectedValues()).isEmpty();
    }

    /**
     * 验证无周期指标并发收到 null 和有效样本时正确取最小值.
     */
    @Test
    public void testStatefulConcurrentNullAndValid() throws Exception {
        assertConcurrentCollection(new StatInfoStatefulCollector<InstanceGauge>());
    }

    /**
     * 验证有周期指标并发收到 null 和有效样本时正确取最小值.
     */
    @Test
    public void testRevisionConcurrentNullAndValid() throws Exception {
        assertConcurrentCollection(new StatInfoRevisionCollector<InstanceGauge>());
    }

    /**
     * 验证默认策略仍接受缺失时延的调用样本并计数.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testTotalCountsNullDelay() {
        StatInfoRevisionCollector<InstanceGauge> collector = new StatInfoRevisionCollector<>();
        InstanceGauge gauge = Mockito.mock(InstanceGauge.class);
        Mockito.when(gauge.getDelay()).thenReturn(null);
        MetricValueAggregationStrategy<InstanceGauge>[] strategies = new MetricValueAggregationStrategy[]{
                new MetricValueAggregationStrategyCollections.UpstreamRequestTotalStrategy(),
        };

        collector.collectStatInfo(gauge, Collections.emptyMap(), strategies);

        Assertions.assertThat(collector.getCollectedValues().iterator().next().getValue()).isEqualTo(1.0);
    }

    private void assertNullThenValid(AbstractSignatureStatInfoCollector<InstanceGauge, ?> collector) {
        collect(collector, null);
        collect(collector, null);
        Assertions.assertThat(collector.getCollectedValues()).isEmpty();

        collect(collector, VALID_DELAY);
        collect(collector, null);

        Assertions.assertThat(collector.getCollectedValues()).hasSize(1);
        StatMetric metric = (StatMetric) collector.getCollectedValues().iterator().next();
        Assertions.assertThat(metric.getValue()).isEqualTo((double) VALID_DELAY);
    }

    private void assertZeroThenNull(AbstractSignatureStatInfoCollector<InstanceGauge, ?> collector) {
        collect(collector, null);
        collect(collector, 0L);
        collect(collector, null);
        collect(collector, VALID_DELAY);

        Assertions.assertThat(collector.getCollectedValues()).hasSize(1);
        StatMetric metric = (StatMetric) collector.getCollectedValues().iterator().next();
        Assertions.assertThat(metric.getValue()).isZero();
    }

    private void assertConcurrentCollection(AbstractSignatureStatInfoCollector<InstanceGauge, ?> collector)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<?> missing = executor.submit(() -> {
                awaitStart(start);
                collect(collector, null);
            });
            Future<?> valid = executor.submit(() -> {
                awaitStart(start);
                collect(collector, VALID_DELAY);
            });
            final Future<?> lower = executor.submit(() -> {
                awaitStart(start);
                collect(collector, LOWER_DELAY);
            });
            start.countDown();
            missing.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            valid.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            lower.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            StatMetric metric = (StatMetric) collector.getCollectedValues().iterator().next();
            Assertions.assertThat(metric.getValue()).isEqualTo((double) LOWER_DELAY);
        } finally {
            executor.shutdownNow();
        }
    }

    private void awaitStart(CountDownLatch start) {
        try {
            Assertions.assertThat(start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void collect(AbstractSignatureStatInfoCollector<InstanceGauge, ?> collector, Long delay) {
        InstanceGauge gauge = Mockito.mock(InstanceGauge.class);
        Mockito.when(gauge.getDelay()).thenReturn(delay);
        MetricValueAggregationStrategy<InstanceGauge>[] strategies = new MetricValueAggregationStrategy[]{
                new MetricValueAggregationStrategyCollections.UpstreamRequestMinTimeoutStrategy(),
        };
        collector.collectStatInfo(gauge, Collections.emptyMap(), strategies);
    }
}
