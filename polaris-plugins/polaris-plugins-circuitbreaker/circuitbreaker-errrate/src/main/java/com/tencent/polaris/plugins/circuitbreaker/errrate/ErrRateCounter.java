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

package com.tencent.polaris.plugins.circuitbreaker.errrate;

import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.plugins.circuitbreaker.common.HalfOpenCounter;
import com.tencent.polaris.plugins.circuitbreaker.common.stat.SliceWindow;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ErrRateCounter extends HalfOpenCounter {

    private final Map<StatusDimension, SliceWindow> sliceWindows = new ConcurrentHashMap<>();

    private final long bucketIntervalMs;

    private final int metricNumBuckets;

    private final String name;

    private final Function<StatusDimension, SliceWindow> create = new Function<StatusDimension, SliceWindow>() {
        @Override
        public SliceWindow apply(StatusDimension statusDimension) {
            return new SliceWindow(name, metricNumBuckets, bucketIntervalMs, Dimension.maxDimension.ordinal());
        }
    };

    public ErrRateCounter(String name, Config config, long bucketIntervalMs) {
        this.name = name;
        this.bucketIntervalMs = bucketIntervalMs;
        this.metricNumBuckets = config.getMetricNumBuckets();
    }

    public SliceWindow getSliceWindow(StatusDimension statusDimension) {
        return sliceWindows.computeIfAbsent(statusDimension, create);
    }

    @Override
    public Set<StatusDimension> getStatusDimensions() {
        return sliceWindows.keySet();
    }

    @Override
    public void resetCounter(StatusDimension statusDimension) {

    }
}
