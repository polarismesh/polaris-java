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

package com.tencent.polaris.plugins.ratelimiter.reject;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.ratelimiter.AmountInfo;
import com.tencent.polaris.api.plugin.ratelimiter.InitCriteria;
import com.tencent.polaris.api.plugin.ratelimiter.LocalQuotaInfo;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaBucket;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import com.tencent.polaris.client.pb.RateLimitProto.Amount;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.client.pb.RateLimitProto.Rule.AmountMode;
import com.tencent.polaris.client.pb.RateLimitProto.Rule.FailoverType;
import com.tencent.polaris.client.pb.RateLimitProto.Rule.Type;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.ratelimiter.common.bucket.BucketShareInfo;
import com.tencent.polaris.plugins.ratelimiter.common.bucket.UpdateIdentifier;
import com.tencent.polaris.plugins.ratelimiter.common.slide.SlidingWindow;
import com.tencent.polaris.plugins.ratelimiter.common.slide.SlidingWindow.Result;
import com.tencent.polaris.plugins.ratelimiter.reject.TokenBucket.AllocateResult;
import com.tencent.polaris.plugins.ratelimiter.reject.TokenBucket.TokenBucketMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class RemoteAwareBucket implements QuotaBucket {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteAwareBucket.class);

    private final Map<Long, TokenBucket> tokenBucketMap = new HashMap<>();

    private final List<TokenBucket> tokenBuckets = new ArrayList<>();

    private final FlowCache flowCache;

    /**
     * 构造器
     *
     * @param initCriteria 参数
     * @param flowCache 流程缓存
     */
    public RemoteAwareBucket(InitCriteria initCriteria, FlowCache flowCache) {
        Rule rule = initCriteria.getRule();
        this.flowCache = flowCache;
        boolean shareEqual = rule.getAmountMode() == AmountMode.SHARE_EQUALLY;
        boolean local = rule.getType() == Type.LOCAL;
        boolean passOnRemoteFail = rule.getFailover() == FailoverType.FAILOVER_PASS;
        BucketShareInfo bucketShareInfo = new BucketShareInfo(shareEqual, local, passOnRemoteFail);
        long minDurationMs = 0;
        for (int i = 0; i < rule.getAmountsCount(); i++) {
            Amount amount = rule.getAmountsList().get(i);
            long validDurationMs = amount.getValidDuration().getSeconds() * 1000;
            if (minDurationMs == 0 || minDurationMs > validDurationMs) {
                minDurationMs = validDurationMs;
            }
            TokenBucket tokenBucket = new TokenBucket(initCriteria.getWindowKey(), validDurationMs,
                    amount.getMaxAmount().getValue(),
                    bucketShareInfo);
            tokenBuckets.add(tokenBucket);
            tokenBucketMap.put(validDurationMs, tokenBucket);
        }
        Collections.sort(tokenBuckets);
        bucketShareInfo.setMinDurationMs(minDurationMs);
    }

    @Override
    public QuotaResult allocateQuota(long curTimeMs, int token) throws PolarisException {
        int stopIdx = -1;
        TokenBucketMode mode = TokenBucketMode.UNKNOWN;
        UpdateIdentifier[] identifiers = new UpdateIdentifier[tokenBuckets.size()];
        AllocateResult[] results = new AllocateResult[tokenBuckets.size()];
        for (int i = 0; i < tokenBuckets.size(); i++) {
            identifiers[i] = flowCache.borrowThreadCacheObject(UpdateIdentifier.class);
            results[i] = flowCache.borrowThreadCacheObject(AllocateResult.class);
        }
        for (int i = 0; i < tokenBuckets.size(); i++) {
            TokenBucket tokenBucket = tokenBuckets.get(i);
            AllocateResult allocateResult = tokenBucket
                    .tryAllocateToken(mode, token, curTimeMs, identifiers[i], results[i]);
            mode = allocateResult.getMode();
            if (allocateResult.getLeft() < 0) {
                stopIdx = i;
                break;
            }
        }
        QuotaResult response;
        boolean usedRemoteQuota = mode == TokenBucketMode.REMOTE;
        if (stopIdx >= 0) {
            //有一个扣除不成功，则进行限流
            TokenBucket tokenBucket = tokenBuckets.get(stopIdx);
            if (usedRemoteQuota) {
                //远程才记录滑窗, 滑窗用于上报
                tokenBucket.confirmLimited(token, curTimeMs);
            }
            //归还配额
            for (int i = 0; i < stopIdx; i++) {
                TokenBucket bucket = tokenBuckets.get(i);
                bucket.giveBackToken(identifiers[i], token, mode);
            }
            response = new QuotaResult(QuotaResult.Code.QuotaResultLimited, 0, "");
        } else {
            //记录分配的配额
            for (TokenBucket tokenBucket : tokenBuckets) {
                if (usedRemoteQuota) {
                    tokenBucket.confirmPassed(token, curTimeMs);
                }
            }
            response = new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, "");
        }
        //归还对象
        for (int i = 0; i < tokenBuckets.size(); i++) {
            flowCache.giveBackThreadCacheObject(identifiers[i]);
            flowCache.giveBackThreadCacheObject(results[i]);
        }
        return response;
    }

    @Override
    public void release() {

    }

    @Override
    public void onRemoteUpdate(RemoteQuotaInfo remoteQuotaInfo) {
        long localCurTimeMs = System.currentTimeMillis();
        long remoteCurTimeMs = remoteQuotaInfo.getCurTimeMs();
        long durationMs = remoteQuotaInfo.getDurationMs();
        long localCurStartMs = SlidingWindow.calculateStartTimeMs(localCurTimeMs, durationMs);
        long remoteCurStartMs = SlidingWindow.calculateStartTimeMs(remoteCurTimeMs, durationMs);
        TokenBucket tokenBucket = tokenBucketMap.get(durationMs);
        if (null == tokenBucket) {
            return;
        }
        if (remoteCurStartMs != localCurStartMs) {
            long remoteQuotaLeft = remoteQuotaInfo.getRemoteQuotaLeft();
            if (remoteCurStartMs + durationMs == localCurStartMs) {
                //仅仅相差一个周期，可以认为是周期间切换导致，这时候可以直接更新配额为全量配额
                //当前周期没有更新，则重置当前周期配额，避免出现时间周期开始时候的误限
                remoteQuotaInfo = new RemoteQuotaInfo(tokenBucket.getRuleTotal(), remoteQuotaInfo.getClientCount(),
                        localCurStartMs, durationMs);
                LOG.warn("[RateLimit]reset remote quota, localTimeMilli {}(startMilli {}), "
                                + "remoteTimeMilli {}(startMilli {}), interval {}, remoteLeft is {}, reset to {}",
                        localCurTimeMs, localCurStartMs, remoteCurTimeMs, remoteCurStartMs, durationMs,
                        remoteQuotaLeft, remoteQuotaInfo.getRemoteQuotaLeft());
            } else {
                tokenBucket.syncUpdateRemoteClientCount(remoteQuotaInfo);
                //不在一个时间段内，丢弃
                LOG.warn("[RateLimit]Drop remote quota, localTimeMilli {}(startMilli {}), "
                                + "remoteTimeMilli {}(startMilli {}), interval {}, remoteLeft is {}", localCurTimeMs,
                        localCurStartMs, remoteCurTimeMs, remoteCurStartMs, durationMs,
                        remoteQuotaLeft);
            }
        }
        tokenBucket.syncUpdateRemoteToken(remoteQuotaInfo);
    }

    @Override
    public Map<Integer, LocalQuotaInfo> fetchLocalUsage(long curTimeMs) {
        Map<Integer, LocalQuotaInfo> localInfos = new HashMap<>();
        for (Map.Entry<Long, TokenBucket> entry : tokenBucketMap.entrySet()) {
            TokenBucket tokenBucket = entry.getValue();
            Result result = tokenBucket.getSlidingWindow().acquireCurrentValues(curTimeMs);
            LocalQuotaInfo localQuotaInfo = new LocalQuotaInfo(result.getPassed(), result.getLimited());
            localInfos.put(tokenBucket.getValidDurationSecond(), localQuotaInfo);
        }
        return localInfos;
    }

    @Override
    public Map<Integer, AmountInfo> getAmountInfo() {
        Map<Integer, AmountInfo> amountInfos = new HashMap<>();
        for (Map.Entry<Long, TokenBucket> entry : tokenBucketMap.entrySet()) {
            TokenBucket tokenBucket = entry.getValue();
            AmountInfo amountInfo = new AmountInfo();
            amountInfo.setMaxAmount(tokenBucket.getRuleTotal());
            amountInfos.put(tokenBucket.getValidDurationSecond(), amountInfo);
        }
        return amountInfos;
    }

}
