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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class StatInfoStatefulCollector<T> extends AbstractSignatureStatInfoCollector<T, StatStatefulMetric> {
    private static final Logger LOG = LoggerFactory.getLogger(StatInfoStatefulCollector.class);

    private final Object mutex = new Object();

    public StatInfoStatefulCollector() {
        super();
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

                StatMetric metric = metricContainer.get(signature);
                if (null == metric) {
                    synchronized (mutex) {
                        metric = metricContainer.get(signature);
                        if (null == metric) {
                            StatStatefulMetric statMetric = new StatStatefulMetric(metricName, labels, signature);
                            statMetric.setValue(strategy.initMetricValue(info));
                            LOG.debug("{} with signature {} init value is {}", strategy.getStrategyName(),
                                    signature,
                                    statMetric.getValue());
                            metricContainer.put(signature, statMetric);
                            continue;
                        }
                    }
                }

                strategy.updateMetricValue(metric, info);
                LOG.debug("{} with signature {} update value is {}", strategy.getStrategyName(),
                        signature,
                        metric.getValue());
            }
        }
    }
}
