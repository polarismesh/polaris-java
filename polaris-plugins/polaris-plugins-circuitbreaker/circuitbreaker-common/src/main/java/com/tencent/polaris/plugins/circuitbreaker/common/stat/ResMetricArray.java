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

package com.tencent.polaris.plugins.circuitbreaker.common.stat;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 资源统计序列，记录单个资源的统计项
 * 数组中每一个元素为一个统计项
 *
 * @author andrewshan
 * @date 2019/8/25
 */
public class ResMetricArray {

    /**
     * 统计序列，每一项都可以进行原子更新
     */
    private final AtomicLong[] metrics;

    public ResMetricArray(int metricSize) {
        metrics = new AtomicLong[metricSize];
        for (int i = 0; i < metricSize; ++i) {
            metrics[i] = new AtomicLong(0L);
        }
    }

    public long getMetric(int dimension) {
        return metrics[dimension].get();
    }

    public long addMetric(int dimension, long value) {
        return metrics[dimension].addAndGet(value);
    }

    public void setMetric(int dimension, long value) {
        metrics[dimension].set(value);
    }
}
