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

/**
 * 维度相关的上报数据的聚合策略
 *
 * @param <T> 聚合数据源的数据类型
 */
public interface MetricValueAggregationStrategy<T> {
    /**
     * 返回策略的描述信息
     *
     * @return 描述信息
     */
    String getStrategyDescription();

    /**
     * 返回策略名称，通常该名称用作metricName
     *
     * @return 策略名称
     */
    String getStrategyName();

    /**
     * 根据metric自身的value值和聚合数据源T的值来更新metric的value
     *
     * @param targetValue 待更新的value值
     * @param dataSource 聚合数据源数据
     */
    void updateMetricValue(StatMetric targetValue, T dataSource);

    /**
     * 根据数据源的内容获取第一次创建metric的时候的初始值
     *
     * @param dataSource 聚合数据源数据
     * @return metric需要的初始值
     */
    double initMetricValue(T dataSource);
}