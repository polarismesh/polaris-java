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

import com.google.common.util.concurrent.AtomicDouble;
import com.tencent.polaris.plugins.stat.common.util.SignatureUtil;

import java.util.Map;

public class StatMetric {
    private final String metricName;
    private final Map<String, String> labels;
    private final Long signature;
    private final AtomicDouble value;

    public StatMetric(String metricName, Map<String, String> labels) {
        this(metricName, labels, SignatureUtil.labelsToSignature(labels));
    }

    public StatMetric(String metricName, Map<String, String> labels, Long signature) {
        this.metricName = metricName;
        this.labels = labels;
        this.signature = signature;
        this.value = new AtomicDouble();
    }

    public String getMetricName() {
        return metricName;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public double getValue() {
        return value.doubleValue();
    }

    public void setValue(double value) {
        this.value.set(value);
    }

    public double addValue(double value) {
        return this.value.addAndGet(value);
    }

    public double incValue() {
        return addValue(1.0);
    }

    public boolean compareAndSet(double expect, double update) {
        return value.compareAndSet(expect, update);
    }

    public Long getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }

        if (that == null || getClass() != that.getClass()) {
            return false;
        }

        return this.signature.equals(((StatMetric) that).signature);
    }

    @Override
    public int hashCode() {
        return signature.intValue();
    }
}
