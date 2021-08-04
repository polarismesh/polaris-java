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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatStatefulMetric extends StatMetric {
    private final ConcurrentHashMap.KeySetView<String, Boolean> markedContainer;

    public StatStatefulMetric(String metricName,
                              Map<String, String> labels,
                              Long signature) {
        this(metricName, labels, ConcurrentHashMap.newKeySet(), signature);
    }

    public StatStatefulMetric(String metricName,
                              Map<String, String> labels,
                              ConcurrentHashMap.KeySetView<String, Boolean> markedContainer,
                              Long signature) {
        super(metricName, labels, signature);
        this.markedContainer = markedContainer;
    }

    public boolean addMarkedName(String markedName) {
        return this.markedContainer.add(markedName);
    }

    public boolean removeMarkedName(String markedName) {
        return this.markedContainer.remove(markedName);
    }

    public boolean contain(String markedName) {
        return this.markedContainer.contains(markedName);
    }
}
