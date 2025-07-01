/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugins.circuitbreaker.common;

import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.StatusDimension;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public abstract class HalfOpenCounter {

    private static class CounterGroup {

        final AtomicInteger successCounter = new AtomicInteger(0);

        final AtomicInteger failCounter = new AtomicInteger(0);

        void reset() {
            successCounter.set(0);
            failCounter.set(0);
        }

        int getHalfOpenSuccessCount() {
            return successCounter.get();
        }

        int getHalfOpenFailCount() {
            return failCounter.get();
        }
    }

    private final Map<StatusDimension, CounterGroup> counterGroups = new ConcurrentHashMap<>();

    private final Function<StatusDimension, CounterGroup> create = new Function<StatusDimension, CounterGroup>() {
        @Override
        public CounterGroup apply(StatusDimension statusDimension) {
            return new CounterGroup();
        }
    };

    private CounterGroup getCounter(StatusDimension statusDimension) {
        return counterGroups.computeIfAbsent(statusDimension, create);
    }

    public abstract Set<StatusDimension> getStatusDimensions();

    public void resetHalfOpen(StatusDimension statusDimension) {
        CounterGroup counter = getCounter(statusDimension);
        counter.reset();
    }

    public int getHalfOpenSuccessCount(StatusDimension statusDimension) {
        CounterGroup counter = getCounter(statusDimension);
        return counter.getHalfOpenSuccessCount();
    }

    public int getHalfOpenFailCount(StatusDimension statusDimension) {
        CounterGroup counter = getCounter(statusDimension);
        return counter.getHalfOpenFailCount();
    }

    public boolean triggerHalfOpenConversion(StatusDimension statusDimension, RetStatus retStatus,
            HalfOpenConfig halfOpenConfig) {
        CounterGroup counter = getCounter(statusDimension);
        if (retStatus == RetStatus.RetFail || retStatus == RetStatus.RetTimeout) {
            int failCount = counter.failCounter.incrementAndGet();
            return failCount == halfOpenConfig.getHalfOpenFailCount();
        } else if (retStatus == RetStatus.RetSuccess) {
            int successCount = counter.successCounter.incrementAndGet();
            return successCount == halfOpenConfig.getHalfOpenSuccessCount();
        }
        return false;
    }

    public abstract void resetCounter(StatusDimension statusDimension);
}
