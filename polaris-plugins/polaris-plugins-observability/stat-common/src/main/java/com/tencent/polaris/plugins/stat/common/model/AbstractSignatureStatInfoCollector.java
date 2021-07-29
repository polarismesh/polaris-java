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

import com.tencent.polaris.plugins.stat.common.util.SignatureUtil;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 签名类型的指标收集器，该收集器提供将指标转为签名的方法以及对应的容器。
 *
 * @param <T> 收集的指标类型
 * @param <V> 收集指标后统计所采用的类型
 */
public abstract class AbstractSignatureStatInfoCollector<T, V> implements StatInfoCollector<T, V> {
    protected ConcurrentMap<Long, V> metricContainer;

    protected AbstractSignatureStatInfoCollector() {
        this.metricContainer = new ConcurrentHashMap<>();
    }

    public static Long getSignature(String metricName, Map<String, String> labels) {
        labels.put(SystemMetricModel.SystemMetricName.METRIC_NAME_LABEL, metricName);
        return SignatureUtil.labelsToSignature(labels);
    }

    public ConcurrentMap<Long, V> getMetricContainer() {
        return metricContainer;
    }

    @Override
    public Collection<V> getCollectedValues() {
        return metricContainer.values();
    }
}
