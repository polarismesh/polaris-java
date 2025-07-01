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

package com.tencent.polaris.plugins.circuitbreaker.composite.trigger;

import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.common.stat.SliceWindow;
import com.tencent.polaris.plugins.circuitbreaker.common.stat.TimeRange;
import com.tencent.polaris.plugins.circuitbreaker.composite.utils.CircuitBreakerUtils;
import org.slf4j.Logger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.tencent.polaris.plugins.circuitbreaker.composite.utils.MatchUtils.matchMethod;

public class ErrRateCounter extends TriggerCounter {

    private static final Logger LOG = LoggerFactory.getLogger(ErrRateCounter.class);

    private static final int BUCKET_COUNT = 10;

    private SliceWindow sliceWindow;

    private final ScheduledExecutorService executorService;

    private long metricWindowMs;

    private int minimumRequest;

    private int errorPercent;

    public ErrRateCounter(String ruleName, CounterOptions counterOptions) {
        super(ruleName, counterOptions);
        executorService = counterOptions.getExecutorService();
    }

    private final AtomicBoolean scheduled = new AtomicBoolean(false);

    @Override
    protected void init() {
        LOG.info("[CircuitBreaker][Counter] errRateCounter {} initialized, resource {}", ruleName, resource);
        long interval = CircuitBreakerUtils.getErrorRateIntervalSec(triggerCondition);
        metricWindowMs = interval * 1000L;
        errorPercent = triggerCondition.getErrorPercent();
        minimumRequest = triggerCondition.getMinimumRequest();
        sliceWindow = new SliceWindow(
                resource.toString(), BUCKET_COUNT, getBucketIntervalMs(interval), Dimension.maxDimension.ordinal());
    }

    @Override
    public void report(ResourceStat resourceStat, Function<String, Pattern> regexPatternFunction) {
        if (suspended.get()) {
            LOG.debug("[CircuitBreaker][Counter] errRateCounter {} suspended, skip report", ruleName);
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
            LOG.debug("[CircuitBreaker][Counter] errRateCounter {} suspended, skip report", ruleName);
            return;
        }
        LOG.debug("[CircuitBreaker][Counter] errRateCounter: add requestCount 1, success {}", success);
        sliceWindow.addGauge((bucket -> {
            if (!success) {
                bucket.addMetric(Dimension.keyFailCount.ordinal(), 1);
            }
            return bucket.addMetric(Dimension.keyRequestCount.ordinal(), 1);
        }));
        if (!success && scheduled.compareAndSet(false, true)) {
            LOG.info("[CircuitBreaker][Counter] errRateCounter: trigger error rate callback on failure, name {}",
                    ruleName);
            executorService.schedule(new StateCheckTask(), metricWindowMs, TimeUnit.MILLISECONDS);
        }
    }

    private static long getBucketIntervalMs(long interval) {
        long metricWindowMs = interval * 1000L;
        double bucketIntervalMs = (double) metricWindowMs / (double) BUCKET_COUNT;
        return (long) Math.ceil(bucketIntervalMs);
    }

    private class StateCheckTask implements Runnable {

        @Override
        public void run() {
            try {
                long currentTimeMs = System.currentTimeMillis();
                TimeRange timeRange = new TimeRange(currentTimeMs - metricWindowMs, currentTimeMs);
                long requestCount = sliceWindow.calcMetricsBothIncluded(Dimension.keyRequestCount.ordinal(), timeRange);
                LOG.info("[CircuitBreaker][Counter] errRateCounter: requestCount {}, minimumRequest {}, name {}, timeRange {}",
                        requestCount, minimumRequest, ruleName, timeRange);
                if (requestCount < minimumRequest) {
                    scheduled.set(false);
                    return;
                }
                long failCount = sliceWindow.calcMetricsBothIncluded(Dimension.keyFailCount.ordinal(), timeRange);
                double failRatio = ((double) failCount / (double) requestCount) * 100;
                LOG.info("[CircuitBreaker][Counter] errRateCounter: failCount {}, minimumRequest {}, failRatio {}," +
                        " name {}, timeRange {}", failCount, failRatio, minimumRequest, ruleName, timeRange);
                if (failRatio >= errorPercent) {
                    suspend();
                    statusChangeHandler.closeToOpen(ruleName, getReason());
                }
            } catch (Throwable throwable) {
                LOG.warn("check state failed.", throwable);
            } finally {
                scheduled.set(false);
            }
        }
    }

    @Override
    public String getReason() {
        return "error_rate:" + errorPercent + "%";
    }
}
