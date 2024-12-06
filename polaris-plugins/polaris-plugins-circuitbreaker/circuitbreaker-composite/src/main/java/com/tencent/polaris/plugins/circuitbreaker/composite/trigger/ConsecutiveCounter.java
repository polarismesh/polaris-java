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

package com.tencent.polaris.plugins.circuitbreaker.composite.trigger;

import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.composite.utils.CircuitBreakerUtils;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.tencent.polaris.plugins.circuitbreaker.composite.utils.MatchUtils.matchMethod;

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
    public void report(ResourceStat resourceStat, Function<String, Pattern> regexPatternFunction) {
        if (suspended.get()) {
            return;
        }

        if (api != null && !matchMethod(resourceStat.getResource(), api, regexFunction, trieNodeFunction)) {
            return;
        }

        RetStatus retStatus = CircuitBreakerUtils.parseRetStatus(resourceStat, errorConditionList, regexPatternFunction);
        boolean success = retStatus != RetStatus.RetFail && retStatus != RetStatus.RetTimeout;
        report(success, regexPatternFunction);
    }

    @Override
    public void report(boolean success, Function<String, Pattern> regexPatternFunction) {
        if (suspended.get()) {
            return;
        }

        if (!success) {
            int currentSum = consecutiveErrors.incrementAndGet();
            if (currentSum == maxCount) {
                suspend();
                consecutiveErrors.set(0);
                statusChangeHandler.closeToOpen(ruleName, getReason());
                return;
            }
        } else {
            consecutiveErrors.set(0);
        }
    }

    @Override
    public String getReason() {
        return "consecutive_error_count:" + maxCount;
    }
}
