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

import com.google.protobuf.util.Durations;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.ratelimiter.AmountInfo;
import com.tencent.polaris.api.plugin.ratelimiter.InitCriteria;
import com.tencent.polaris.api.plugin.ratelimiter.LocalQuotaInfo;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaBucket;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import com.tencent.polaris.client.pb.RateLimitProto.Amount;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

/**
 * Quota bucket implement of leaky bucket.
 *
 * @author Haotian Zhang
 */
public class RemoteAwareLeakyBucket implements QuotaBucket {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAwareLeakyBucket.class);

    private final LeakyBucket leakyBucket;

    /**
     * 构造器
     *
     * @param initCriteria 参数
     */
    public RemoteAwareLeakyBucket(InitCriteria initCriteria, Configuration configuration) {
        leakyBucket = new LeakyBucket();
        Rule rule = initCriteria.getRule();
        leakyBucket.setRule(rule);
        int instCount = 1;
        boolean effective = false;
        double effectiveRate = 0.0F;
        leakyBucket.setMaxQueuingDuration(configuration.getProvider().getRateLimit().getMaxQueuingTime());
        if (rule.getMaxQueueDelay().getValue() > 0) {
            leakyBucket.setMaxQueuingDuration(TimeUnit.SECONDS.toMillis(rule.getMaxQueueDelay().getValue()));
        }
        long maxDuration = 0;
        for (Amount amount : rule.getAmountsList()) {
            int maxAmount = amount.getMaxAmount().getValue();
            if (maxAmount == 0) {
                leakyBucket.setRejectAll(true);
                return;
            }

            long duration = Durations.toMillis(amount.getValidDuration());
            // 选出允许qps最低的amount和duration组合，作为effectiveAmount和effectiveDuration
            // 在匀速排队限流器里面，就是每个请求都要间隔同样的时间，
            // 如限制1s 10个请求，那么每个请求只有在上个请求允许过去100ms后才能通过下一个请求
            // 这种机制下面，那么在多个amount组合里面，只要允许qps最低的组合生效，那么所有限制都满足了
            if (!effective) {
                leakyBucket.setEffectiveAmount(maxAmount);
                leakyBucket.setEffectiveDuration(duration);
                maxDuration = duration;
                effective = true;
                effectiveRate =
                        ((double) leakyBucket.getEffectiveDuration()) / ((double) leakyBucket.getEffectiveAmount());
            } else {
                double newRate = ((double) duration) / ((double) maxAmount);
                if (newRate > effectiveRate) {
                    leakyBucket.setEffectiveAmount(maxAmount);
                    leakyBucket.setEffectiveDuration(duration);
                    effectiveRate = newRate;
                }
                if (duration > maxDuration) {
                    maxDuration = duration;
                }
            }
        }
        effectiveRate = ((double) leakyBucket.getEffectiveDuration()) / ((double) leakyBucket.getEffectiveAmount());
        leakyBucket.setTotalRate(effectiveRate);
        leakyBucket.setEffectiveRate(Math.round(effectiveRate));
        LOG.debug("Create a leaky bucket with effective rate with {}", leakyBucket.getEffectiveRate());
    }

    @Override
    public QuotaResult allocateQuota(long curTimeMs, int count) throws PolarisException {
        return leakyBucket.getQuota();
    }

    @Override
    public void release() {

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
