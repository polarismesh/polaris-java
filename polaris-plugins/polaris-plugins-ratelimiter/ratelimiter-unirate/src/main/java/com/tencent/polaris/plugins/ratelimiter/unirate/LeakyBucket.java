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

package com.tencent.polaris.plugins.ratelimiter.unirate;

import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;

/**
 * Implement of leaky bucket.
 *
 * @author Haotian Zhang
 */
public class LeakyBucket implements Comparable<LeakyBucket> {

    private static final Logger LOG = LoggerFactory.getLogger(LeakyBucket.class);

    /**
     * 限流规则
     */
    private Rule rule;
    /**
     * 上次分配配额的时间戳
     */
    private AtomicLong lastGrantTime;
    /**
     * 等效配额
     */
    private Integer effectiveAmount;
    /**
     * 等效时间窗
     */
    private Long effectiveDuration;
    /**
     * 为一个实例生成一个配额的平均时间
     */
    private Long effectiveRate;
    /**
     * 所有实例分配一个配额的平均时间
     */
    private Double totalRate;
    /**
     * 最大排队时间
     */
    private Long maxQueuingDuration;
    /**
     * 是不是有amount为0
     */
    private Boolean rejectAll;

    public LeakyBucket() {
        this.lastGrantTime = new AtomicLong(0);
        this.rejectAll = false;
    }

    public QuotaResult getQuota() {
        if (getRejectAll()) {
            return new QuotaResult(QuotaResult.Code.QuotaResultLimited, 0,
                    "uniRate RateLimiter: reject for zero rule amount");
        }

        long costDuration = effectiveRate;
        long waitDuration = 0;
        while (true) {
            long currentTimestamp = System.currentTimeMillis();
            long expectedTimestamp = lastGrantTime.addAndGet(costDuration);
            waitDuration = expectedTimestamp - currentTimestamp;
            if (waitDuration >= 0) {
                break;
            }
            // 首次访问，尝试更新时间间隔
            if (lastGrantTime.compareAndSet(expectedTimestamp, currentTimestamp)) {
                // 更新时间成功，此时他是第一个进来的，等待时间归0
                waitDuration = 0;
                break;
            }
        }
        if (waitDuration == 0) {
            LOG.debug("grant quota without wait.");
            return new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, "uniRate RateLimiter: grant quota");
        }
        // 如果等待时间在上限之内，那么放通
        if (waitDuration <= maxQueuingDuration) {
            LOG.debug("grant quota, waitDuration {}ms.", waitDuration);
            return new QuotaResult(QuotaResult.Code.QuotaResultOk, waitDuration, "uniRate RateLimiter: grant quota");
        }
        // 如果等待时间超过配置的上限，那么拒绝
        // 归还等待间隔
        LOG.debug("ratelimited, waitDuration {}ms.", waitDuration);
        String info = String.format("uniRate RateLimiter: queueing time %d exceed maxQueuingTime %s", waitDuration,
                maxQueuingDuration);
        lastGrantTime.addAndGet(-costDuration);
        return new QuotaResult(QuotaResult.Code.QuotaResultLimited, 0, info);
    }

    @Override
    public int compareTo(LeakyBucket o) {
        return (int) (this.effectiveRate - o.effectiveDuration);
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public Long getLastGrantTime() {
        return lastGrantTime.get();
    }

    public Integer getEffectiveAmount() {
        return effectiveAmount;
    }

    public void setEffectiveAmount(Integer effectiveAmount) {
        this.effectiveAmount = effectiveAmount;
    }

    public Long getEffectiveDuration() {
        return effectiveDuration;
    }

    public void setEffectiveDuration(Long effectiveDuration) {
        this.effectiveDuration = effectiveDuration;
    }

    public Long getEffectiveRate() {
        return effectiveRate;
    }

    public void setEffectiveRate(Long effectiveRate) {
        this.effectiveRate = effectiveRate;
    }

    public Double getTotalRate() {
        return totalRate;
    }

    public void setTotalRate(Double totalRate) {
        this.totalRate = totalRate;
    }

    public Long getMaxQueuingDuration() {
        return maxQueuingDuration;
    }

    public void setMaxQueuingDuration(Long maxQueuingDuration) {
        this.maxQueuingDuration = maxQueuingDuration;
    }

    public Boolean getRejectAll() {
        return rejectAll;
    }

    public void setRejectAll(Boolean rejectAll) {
        this.rejectAll = rejectAll;
    }
}
