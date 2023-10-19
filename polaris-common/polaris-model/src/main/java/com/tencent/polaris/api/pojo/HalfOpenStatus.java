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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HalfOpenStatus extends CircuitBreakerStatus {

    private final AtomicInteger allocated = new AtomicInteger(0);

    private final int maxRequest;

    private final AtomicBoolean scheduled = new AtomicBoolean(false);

    // record invoke success or fail
    private final List<Boolean> calledResult;

    private boolean triggered = false;

    private final Object lock = new Object();

    public HalfOpenStatus(String circuitBreaker, long startTimeMs, int maxRequest) {
        super(circuitBreaker, Status.HALF_OPEN, startTimeMs);
        this.maxRequest = maxRequest;
        this.calledResult = new ArrayList<>(maxRequest * 2);
    }

    public boolean report(boolean success) {
        synchronized (lock) {
            calledResult.add(success);
            boolean needTrigger = !success || calledResult.size() >= maxRequest;
            if (needTrigger && !triggered) {
                triggered = true;
                return true;
            }
            return false;
        }
    }

    public Status calNextStatus() {
        synchronized (lock) {
            if (!triggered) {
                return Status.HALF_OPEN;
            }
            for (Boolean result : calledResult) {
                if (!result) {
                    return Status.OPEN;
                }
            }
            return Status.CLOSE;
        }
    }

    public boolean schedule() {
        return scheduled.compareAndSet(false, true);
    }
}
