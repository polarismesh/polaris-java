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

package com.tencent.polaris.plugins.stat.prometheus.handler;

import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategy;
import com.tencent.polaris.plugins.stat.common.model.MetricValueAggregationStrategyCollections;
import com.tencent.polaris.plugins.stat.common.model.StatInfoRevisionCollector;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import java.util.Collections;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for exporting minimum delays with missing samples.
 *
 * @author fuyuwei01
 */
public class MinimumDelayExportTest {

    private static final String METRIC_NAME = "upstream_rq_min_timeout";

    private static final long VALID_DELAY = 950L;

    private static final long LOWER_DELAY = 900L;

    private final CollectorRegistry registry = new CollectorRegistry();

    private final Gauge gauge = Gauge.build().name(METRIC_NAME).help("Minimum request delay")
            .labelNames("service").register(registry);

    private final StatInfoRevisionCollector<InstanceGauge> collector = new StatInfoRevisionCollector<>();

    /**
     * 验证全 null 样本不导出虚假的零值或哨兵，首个有效时延正常导出.
     */
    @Test
    public void testNullThenValidExport() {
        collect(null);
        export();
        Assertions.assertThat(exportedValue()).isNull();

        collect(VALID_DELAY);
        export();

        Assertions.assertThat(exportedValue()).isEqualTo((double) VALID_DELAY);
    }

    /**
     * 验证有效零时延可导出，后续 null 不改变它.
     */
    @Test
    public void testZeroThenNullExport() {
        collect(0L);
        collect(null);
        export();

        Assertions.assertThat(exportedValue()).isZero();
    }

    /**
     * 验证缺失样本周期按既有规则归零，下一有效样本重新初始化.
     */
    @Test
    public void testMissingPeriodThenValidExport() {
        collect(LOWER_DELAY);
        export();
        Assertions.assertThat(exportedValue()).isEqualTo((double) LOWER_DELAY);
        collector.incRevision();

        collect(null);
        export();
        Assertions.assertThat(exportedValue()).isZero();
        collect(VALID_DELAY);
        export();

        Assertions.assertThat(exportedValue()).isEqualTo((double) VALID_DELAY);
    }

    private Double exportedValue() {
        return registry.getSampleValue(METRIC_NAME, new String[]{"service"}, new String[]{"checkout"});
    }

    private void export() {
        CommonHandler.putDataFromContainerInOrder(Collections.singletonMap(METRIC_NAME, gauge), collector,
                collector.getCurrentRevision(), new String[]{"service"});
    }

    @SuppressWarnings("unchecked")
    private void collect(Long delay) {
        InstanceGauge sample = Mockito.mock(InstanceGauge.class);
        Mockito.when(sample.getDelay()).thenReturn(delay);
        MetricValueAggregationStrategy<InstanceGauge>[] strategies = new MetricValueAggregationStrategy[]{
                new MetricValueAggregationStrategyCollections.UpstreamRequestMinTimeoutStrategy(),
        };
        collector.collectStatInfo(sample, Collections.singletonMap("service", "checkout"), strategies);
    }
}
