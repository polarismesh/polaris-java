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

import java.util.Collection;
import java.util.Map;

/**
 * 采用某种策略收集指标T，并统计为V
 *
 * @param <T> 收集的指标类型
 * @param <V> 收集指标后统计所采用的类型
 */
public interface StatInfoCollector<T, V> {
    /**
     * 使用Strategies策略集收集info的信息
     *
     * @param info 收集的指标类型的值
     * @param metricLabels 收集的指标的标签集
     * @param strategies 收集指标采用的策略集
     */
    void collectStatInfo(T info, Map<String, String> metricLabels, MetricValueAggregationStrategy<T>[] strategies);

    /**
     * 返回统计的结果
     *
     * @return 统计的结果值
     */
    Collection<V> getCollectedValues();
}
