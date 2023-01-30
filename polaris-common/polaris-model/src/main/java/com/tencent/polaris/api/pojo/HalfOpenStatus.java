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

package com.tencent.polaris.api.pojo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class HalfOpenStatus extends CircuitBreakerStatus {

    private final AtomicInteger allocated = new AtomicInteger(0);

    private final int maxRequest;

    private final Consumer<Void> callback;

    public HalfOpenStatus(String circuitBreaker, long startTimeMs, int maxRequest,
            Consumer<Void> callback) {
        super(circuitBreaker, Status.HALF_OPEN, startTimeMs);
        this.maxRequest = maxRequest;
        this.callback = callback;
    }

    public int getMaxRequest() {
        return maxRequest;
    }

    public boolean allocate() {
        int result = allocated.incrementAndGet();
        if (result == maxRequest) {
            callback.accept(null);
        }
        return result <= maxRequest;
    }
}
