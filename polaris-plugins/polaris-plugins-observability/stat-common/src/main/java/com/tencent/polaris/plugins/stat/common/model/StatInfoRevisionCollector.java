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

package com.tencent.polaris.plugins.stat.common.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 带有版本信息的指标收集器，当更新版本后，再次收集指标的时候，指标重新计数。
 *
 * @param <T> 收集的指标类型
 */
public class StatInfoRevisionCollector<T> extends AbstractSignatureStatInfoCollector<T, StatRevisionMetric> {
    private final Object mutex = new Object();
    private final AtomicLong currentRevision;

    public StatInfoRevisionCollector() {
        super();
        this.currentRevision = new AtomicLong();
    }

    public long incRevision() {
        return currentRevision.incrementAndGet();
    }

    public long getCurrentRevision() {
        return currentRevision.get();
    }

    @Override
    public void collectStatInfo(T info,
                                Map<String, String> metricLabels,
                                MetricValueAggregationStrategy<T>[] strategies) {
        if (null != strategies) {
            String metricName;
            for (MetricValueAggregationStrategy<T> strategy : strategies) {
                metricName = strategy.getStrategyName();
                Map<String, String> labels = new HashMap<>(metricLabels);
                Long signature = getSignature(metricName, labels);

                StatRevisionMetric metrics = metricContainer.get(signature);
                if (null == metrics) {
                    synchronized (mutex) {
                        metrics = metricContainer.get(signature);
                        if (null == metrics) {
                            // 如果该指标不存在，则初始化该指标。
                            StatRevisionMetric stateMetric = new StatRevisionMetric(metricName,
                                    labels,
                                    signature,
                                    currentRevision.get());
                            stateMetric.setValue(strategy.initMetricValue(info));
                            metricContainer.put(signature, stateMetric);
                            continue;
                        }
                    }
                }

                if (currentRevision.get() != metrics.getRevision()) {
                    synchronized (mutex) {
                        if (currentRevision.get() != metrics.getRevision()) {
                            // 如果revision不一致，说明该指标记录过期，
                            // 则reversion设置为当前的reversion，且value重置为初始值。
                            metrics.setValue(strategy.initMetricValue(info));
                            metrics.setRevision(currentRevision.get());
                            continue;
                        }
                    }
                }

                strategy.updateMetricValue(metrics, info);
            }
        }
    }
}
