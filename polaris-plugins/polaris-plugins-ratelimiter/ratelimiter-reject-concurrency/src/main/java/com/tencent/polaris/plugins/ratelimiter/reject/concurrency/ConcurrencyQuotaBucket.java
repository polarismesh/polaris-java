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

package com.tencent.polaris.plugins.ratelimiter.reject.concurrency;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.ratelimiter.*;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发数限流
 *
 * @author Haotian Zhang
 */
public class ConcurrencyQuotaBucket implements QuotaBucket {

    private static final Logger LOG = LoggerFactory.getLogger(ConcurrencyQuotaBucket.class);

    private final RateLimitProto.Rule rule;

    private final Integer initThreadCount;

    private final AtomicInteger currentThreadCount;

    public ConcurrencyQuotaBucket(InitCriteria initCriteria) {
        this.rule = initCriteria.getRule();
        this.initThreadCount = initCriteria.getRule().getConcurrencyAmount().getMaxAmount();
        this.currentThreadCount = new AtomicInteger(0);
    }

    @Override
    public QuotaResult allocateQuota(long curTimeMs, int count) throws PolarisException {
        QuotaResult response;
        if (initThreadCount == null || currentThreadCount == null) {
            LOG.error("ratelimit rule {} has not thread permit", rule.getId());
            response = new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, "");
        } else {
            /*
              先尝试将线程数+1
              1. 如果+1后大于初始线程数，表示此时配额已经满了，当前请求需要被限流
              2. 如果+1后小于等于初始线程数，表示当前请求在配额限制范围内，可以继续执行请求
             */
            if (currentThreadCount.incrementAndGet() > initThreadCount) {
                // 由于先尝试+1了，所以需要立即减去
                currentThreadCount.decrementAndGet();

                LOG.debug("block by ratelimit rule {}, concurrencyAmount: {}.", rule.getId(), initThreadCount);
                String info = RateLimitProto.Rule.Resource.CONCURRENCY + ":" + initThreadCount;
                response = new QuotaResult(QuotaResult.Code.QuotaResultLimited, 0, info);
            } else {
                // 当前请求在配额限制范围内，在执行完请求后释放消费的配额。在上层应用中的 finally 中释放
                LOG.debug("ratelimit rule {} passing, concurrencyAmount: {}.", rule.getId(), initThreadCount);
                response = new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, "");
                response.setRelease(this::release);
            }
        }
        return response;
    }

    @Override
    public void returnQuota(long allocateTimeMs, int count) throws PolarisException {

    }

    @Override
    public void release() {
        currentThreadCount.decrementAndGet();
    }

    @Override
    public void onRemoteUpdate(RemoteQuotaInfo remoteQuotaInfo) {

    }

    @Override
    public Map<Integer, LocalQuotaInfo> fetchLocalUsage(long curTimeMs) {
        return null;
    }

    @Override
    public Map<Integer, AmountInfo> getAmountInfo() {
        return null;
    }
}
