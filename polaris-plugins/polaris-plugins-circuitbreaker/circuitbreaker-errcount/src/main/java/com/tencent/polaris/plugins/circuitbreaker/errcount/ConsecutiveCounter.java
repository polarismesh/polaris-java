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

package com.tencent.polaris.plugins.circuitbreaker.errcount;

import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.plugins.circuitbreaker.common.HalfOpenCounter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ConsecutiveCounter extends HalfOpenCounter {

    private final Map<StatusDimension, AtomicInteger> consecutiveErrors = new HashMap<>();

    private final Function<StatusDimension, AtomicInteger> create = new Function<StatusDimension, AtomicInteger>() {
        @Override
        public AtomicInteger apply(StatusDimension statusDimension) {
            return new AtomicInteger(0);
        }
    };

    private AtomicInteger getCounter(StatusDimension statusDimension) {
        return consecutiveErrors.computeIfAbsent(statusDimension, create);
    }

    @Override
    public Set<StatusDimension> getStatusDimensions() {
        return consecutiveErrors.keySet();
    }

    public int onFail(StatusDimension statusDimension) {
        AtomicInteger counter = getCounter(statusDimension);
        return counter.incrementAndGet();
    }

    public int getConsecutiveErrorCount(StatusDimension statusDimension) {
        AtomicInteger counter = getCounter(statusDimension);
        return counter.get();
    }

    @Override
    public void resetCounter(StatusDimension statusDimension) {
        AtomicInteger counter = getCounter(statusDimension);
        counter.set(0);
    }
}
