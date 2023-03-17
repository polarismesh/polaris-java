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

package com.tencent.polaris.plugins.circuitbreaker.composite.trigger;

import com.tencent.polaris.logging.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;

public class ConsecutiveCounter extends TriggerCounter {

    private static final Logger LOG = LoggerFactory.getLogger(ConsecutiveCounter.class);

    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);

    private int maxCount;

    public ConsecutiveCounter(String ruleName, CounterOptions counterOptions) {
        super(ruleName, counterOptions);
    }

    @Override
    protected void init() {
        LOG.info("[CircuitBreaker][Counter] consecutiveCounter {} initialized, resource {}", ruleName, resource);
        maxCount = triggerCondition.getErrorCount();
    }

    @Override
    public void report(boolean success) {
        if (suspended.get()) {
            return;
        }
        if (!success) {
            int currentSum = consecutiveErrors.incrementAndGet();
            if (currentSum == maxCount) {
                suspend();
                consecutiveErrors.set(0);
                statusChangeHandler.closeToOpen(ruleName);
                return;
            }
        } else {
            consecutiveErrors.set(0);
        }
    }
}
