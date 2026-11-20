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
import java.util.concurrent.atomic.AtomicBoolean;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for request delay extrema aggregation strategies.
 *
 * @author fuyuwei01
 */
public class RequestDelayExtremaStrategyTest {

    private static final long INITIAL_DELAY = 1000L;

    private static final long CONCURRENT_MAX_DELAY = 1066L;

    private static final long MAX_DELAY = 1058L;

    private static final long RETRY_MAX_DELAY = 1020L;

    private static final long CONCURRENT_MIN_DELAY = 900L;

    private static final long MIN_DELAY = 950L;

    private static final long RETRY_MIN_DELAY = 980L;

    /**
     * 测试并发场景：读取后出现更优极值，当前更新必须保留它.
     */
    @Test
    public void testMaxPreservesConcurrentExtreme() {
        StatMetric metric = metricWithConcurrentRead(INITIAL_DELAY, CONCURRENT_MAX_DELAY);

        new MetricValueAggregationStrategyCollections.UpstreamRequestMaxTimeoutStrategy()
                .updateMetricValue(metric, gaugeWithDelay(MAX_DELAY));

        Assertions.assertThat(metric.getValue()).isEqualTo((double) CONCURRENT_MAX_DELAY);
    }

    /**
     * 测试重试场景：首次 CAS 失败后仍可改进，必须重新读取并更新.
     */
    @Test
    public void testMaxRetriesAfterConcurrentUpdate() {
        StatMetric metric = metricWithConcurrentCas(INITIAL_DELAY, RETRY_MAX_DELAY);

        new MetricValueAggregationStrategyCollections.UpstreamRequestMaxTimeoutStrategy()
                .updateMetricValue(metric, gaugeWithDelay(MAX_DELAY));

        Assertions.assertThat(metric.getValue()).isEqualTo((double) MAX_DELAY);
    }

    /**
     * 测试普通场景：改进时更新，相等或较差输入保持极值.
     */
    @Test
    public void testMaxUpdatesOnlyForBetterDelay() {
        StatMetric metric = new StatMetric("delay", Collections.emptyMap());
        metric.setValue(INITIAL_DELAY);
        MetricValueAggregationStrategy<InstanceGauge> strategy =
                new MetricValueAggregationStrategyCollections.UpstreamRequestMaxTimeoutStrategy();

        strategy.updateMetricValue(metric, gaugeWithDelay(MAX_DELAY));
        strategy.updateMetricValue(metric, gaugeWithDelay(MAX_DELAY));
        strategy.updateMetricValue(metric, gaugeWithDelay(INITIAL_DELAY));

        Assertions.assertThat(metric.getValue()).isEqualTo((double) MAX_DELAY);
    }

    /**
     * 测试空值场景：未提供时延时保持极值，初始值为零.
     */
    @Test
    public void testMaxIgnoresNullDelay() {
        StatMetric metric = new StatMetric("delay", Collections.emptyMap());
        metric.setValue(INITIAL_DELAY);
        InstanceGauge gauge = Mockito.mock(InstanceGauge.class);
        Mockito.when(gauge.getDelay()).thenReturn(null);
        MetricValueAggregationStrategy<InstanceGauge> strategy =
                new MetricValueAggregationStrategyCollections.UpstreamRequestMaxTimeoutStrategy();

        strategy.updateMetricValue(metric, gauge);

        Assertions.assertThat(metric.getValue()).isEqualTo((double) INITIAL_DELAY);
        Assertions.assertThat(strategy.initMetricValue(gauge)).isZero();
    }

    /**
     * 测试初始化场景：初始极值采用输入时延.
     */
    @Test
    public void testMaxInitializesFromDelay() {
        Assertions.assertThat(new MetricValueAggregationStrategyCollections.UpstreamRequestMaxTimeoutStrategy()
                .initMetricValue(gaugeWithDelay(MAX_DELAY))).isEqualTo((double) MAX_DELAY);
    }

    /**
     * 测试并发场景：读取后出现更优极值，当前更新必须保留它.
     */
    @Test
    public void testMinPreservesConcurrentExtreme() {
        StatMetric metric = metricWithConcurrentRead(INITIAL_DELAY, CONCURRENT_MIN_DELAY);

        new MetricValueAggregationStrategyCollections.UpstreamRequestMinTimeoutStrategy()
                .updateMetricValue(metric, gaugeWithDelay(MIN_DELAY));

        Assertions.assertThat(metric.getValue()).isEqualTo((double) CONCURRENT_MIN_DELAY);
    }

    /**
     * 测试重试场景：首次 CAS 失败后仍可改进，必须重新读取并更新.
     */
    @Test
    public void testMinRetriesAfterConcurrentUpdate() {
        StatMetric metric = metricWithConcurrentCas(INITIAL_DELAY, RETRY_MIN_DELAY);

        new MetricValueAggregationStrategyCollections.UpstreamRequestMinTimeoutStrategy()
                .updateMetricValue(metric, gaugeWithDelay(MIN_DELAY));

        Assertions.assertThat(metric.getValue()).isEqualTo((double) MIN_DELAY);
    }

    /**
     * 测试普通场景：改进时更新，相等或较差输入保持极值.
     */
    @Test
    public void testMinUpdatesOnlyForBetterDelay() {
        StatMetric metric = new StatMetric("delay", Collections.emptyMap());
        metric.setValue(INITIAL_DELAY);
        MetricValueAggregationStrategy<InstanceGauge> strategy =
                new MetricValueAggregationStrategyCollections.UpstreamRequestMinTimeoutStrategy();

        strategy.updateMetricValue(metric, gaugeWithDelay(MIN_DELAY));
        strategy.updateMetricValue(metric, gaugeWithDelay(MIN_DELAY));
        strategy.updateMetricValue(metric, gaugeWithDelay(INITIAL_DELAY));

        Assertions.assertThat(metric.getValue()).isEqualTo((double) MIN_DELAY);
    }

    /**
     * 测试空值场景：已有有效极值时，未提供时延的样本不能覆盖它.
     */
    @Test
    public void testMinIgnoresNullDelay() {
        StatMetric metric = new StatMetric("delay", Collections.emptyMap());
        metric.setValue(INITIAL_DELAY);
        InstanceGauge gauge = Mockito.mock(InstanceGauge.class);
        Mockito.when(gauge.getDelay()).thenReturn(null);
        MetricValueAggregationStrategy<InstanceGauge> strategy =
                new MetricValueAggregationStrategyCollections.UpstreamRequestMinTimeoutStrategy();

        strategy.updateMetricValue(metric, gauge);

        Assertions.assertThat(metric.getValue()).isEqualTo((double) INITIAL_DELAY);
    }

    /**
     * 测试初始化场景：初始极值采用输入时延.
     */
    @Test
    public void testMinInitializesFromDelay() {
        Assertions.assertThat(new MetricValueAggregationStrategyCollections.UpstreamRequestMinTimeoutStrategy()
                .initMetricValue(gaugeWithDelay(MIN_DELAY))).isEqualTo((double) MIN_DELAY);
    }

    private InstanceGauge gaugeWithDelay(long delay) {
        InstanceGauge gauge = Mockito.mock(InstanceGauge.class);
        Mockito.when(gauge.getDelay()).thenReturn(delay);
        return gauge;
    }

    private StatMetric metricWithConcurrentRead(long initialValue, long concurrentValue) {
        StatMetric metric = Mockito.spy(new StatMetric("delay", Collections.emptyMap()));
        metric.setValue(initialValue);
        AtomicBoolean firstRead = new AtomicBoolean(true);
        // 保留第一次读取值，并模拟另一个线程在读取完成后提交极值。
        Mockito.doAnswer(invocation -> {
            double observedValue = (double) invocation.callRealMethod();
            if (firstRead.getAndSet(false)) {
                metric.setValue(concurrentValue);
            }
            return observedValue;
        }).when(metric).getValue();
        return metric;
    }

    private StatMetric metricWithConcurrentCas(long initialValue, long concurrentValue) {
        StatMetric metric = Mockito.spy(new StatMetric("delay", Collections.emptyMap()));
        metric.setValue(initialValue);
        AtomicBoolean firstCas = new AtomicBoolean(true);
        // 模拟另一线程在第一次 CAS 前更新值，使本次 CAS 失败。
        Mockito.doAnswer(invocation -> {
            if (firstCas.getAndSet(false)) {
                metric.setValue(concurrentValue);
            }
            return invocation.callRealMethod();
        }).when(metric).compareAndSet(Mockito.anyLong(), Mockito.anyLong());
        return metric;
    }
}
