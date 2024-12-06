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

package com.tencent.polaris.api.plugin.circuitbreaker;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.pojo.Subset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 熔断结果
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class CircuitBreakResult {

    public static class ResultKey {

        private final String instId;

        private final StatusDimension statusDimension;

        public ResultKey(String instId, StatusDimension statusDimension) {
            this.instId = instId;
            this.statusDimension = statusDimension;
        }

        public String getInstId() {
            return instId;
        }

        public StatusDimension getStatusDimension() {
            return statusDimension;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ResultKey resultKey = (ResultKey) o;
            return Objects.equals(instId, resultKey.instId) && Objects
                    .equals(statusDimension, resultKey.statusDimension);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instId, statusDimension);
        }
    }

    private final long createTimeMs;

    private final int maxRequestCountAfterHalfOpen;

    private final Map<CircuitBreakerStatus.Status, Map<ResultKey, Instance>> instanceResult = new HashMap<>();

    private final Map<CircuitBreakerStatus.Status, Map<ResultKey, Subset>> subsetResult = new HashMap<>();

    public CircuitBreakResult(long createTimeMs, int maxRequestCountAfterHalfOpen) {
        this.createTimeMs = createTimeMs;
        this.maxRequestCountAfterHalfOpen = maxRequestCountAfterHalfOpen;
        instanceResult.put(CircuitBreakerStatus.Status.CLOSE, new HashMap<>());
        instanceResult.put(CircuitBreakerStatus.Status.HALF_OPEN, new HashMap<>());
        instanceResult.put(CircuitBreakerStatus.Status.OPEN, new HashMap<>());
        subsetResult.put(CircuitBreakerStatus.Status.CLOSE, new HashMap<>());
        subsetResult.put(CircuitBreakerStatus.Status.HALF_OPEN, new HashMap<>());
        subsetResult.put(CircuitBreakerStatus.Status.OPEN, new HashMap<>());
    }

    public boolean isEmptyResult() {
        for (Map.Entry<CircuitBreakerStatus.Status, Map<ResultKey, Instance>> entry : instanceResult.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                return false;
            }
        }
        for (Map.Entry<CircuitBreakerStatus.Status, Map<ResultKey, Subset>> entry : subsetResult.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public Map<ResultKey, Instance> getInstancesToOpen() {
        return instanceResult.get(CircuitBreakerStatus.Status.OPEN);
    }

    public Map<ResultKey, Instance> getInstancesToHalfOpen() {
        return instanceResult.get(CircuitBreakerStatus.Status.HALF_OPEN);
    }

    public Map<ResultKey, Instance> getInstancesToClose() {
        return instanceResult.get(CircuitBreakerStatus.Status.CLOSE);
    }

    public Map<ResultKey, Subset> getSubsetsToOpen() {
        return subsetResult.get(CircuitBreakerStatus.Status.OPEN);
    }

    public Map<ResultKey, Subset> getSubsetsToHalfOpen() {
        return subsetResult.get(CircuitBreakerStatus.Status.HALF_OPEN);
    }

    public Map<ResultKey, Subset> getSubsetsToClose() {
        return subsetResult.get(CircuitBreakerStatus.Status.CLOSE);
    }

    public long getCreateTimeMs() {
        return createTimeMs;
    }

    public int getMaxRequestCountAfterHalfOpen() {
        return maxRequestCountAfterHalfOpen;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "CircuitBreakResult{"
                +
                "result=" + instanceResult
                +
                '}';
    }
}
