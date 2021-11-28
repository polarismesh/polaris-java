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

import com.tencent.polaris.api.plugin.ratelimiter.RemoteQuotaInfo;
import com.tencent.polaris.api.utils.ClosableReadWriteLock;
import com.tencent.polaris.api.utils.ClosableReadWriteLock.LockWrapper;
import com.tencent.polaris.plugins.ratelimiter.common.bucket.BucketShareInfo;
import com.tencent.polaris.plugins.ratelimiter.common.bucket.UpdateIdentifier;
import com.tencent.polaris.plugins.ratelimiter.common.slide.SlidingWindow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenBucket implements Comparable<TokenBucket> {

    private static final Logger LOG = LoggerFactory.getLogger(TokenBucket.class);

    @Override
    public int compareTo(TokenBucket o) {
        return (int) (this.validDurationMs - o.validDurationMs);
    }

    //工作模式
    public enum TokenBucketMode {
        UNKNOWN,
        REMOTE,
        REMOTE_TO_LOCAL,
        LOCAL
    }

    private final UpdateIdentifier tokenBucketTimeSet = new UpdateIdentifier();

    //共享的规则数据
    private final BucketShareInfo shareInfo;

    //唯一的滑窗标识
    private final String windowKey;

    //限流区间
    private final long validDurationMs;

    //限流区间，单位s
    private final int validDurationSecond;

    //规则中定义的限流配额
    private final AtomicLong ruleToken = new AtomicLong(0);

    //每周期分配的配额总量
    private final AtomicLong tokenLeft = new AtomicLong(0);

    //远程降级到本地的剩余配额数
    private final AtomicLong remoteToLocalTokenLeft = new AtomicLong(0);

    //实例数，通过远程更新
    private final AtomicInteger instanceCount = new AtomicInteger(0);

    private final ClosableReadWriteLock lock = new ClosableReadWriteLock();

    private final SlidingWindow slidingWindow;

    public TokenBucket(String windowKey, long validDurationMs, int tokenAmount, BucketShareInfo shareInfo) {
        this.windowKey = windowKey;
        this.validDurationMs = validDurationMs;
        this.validDurationSecond = (int) (validDurationMs / 1e3);
        this.ruleToken.set(tokenAmount);
        this.tokenLeft.set(tokenAmount);
        this.slidingWindow = new SlidingWindow(1, validDurationMs);
        this.shareInfo = shareInfo;
        this.instanceCount.set(1);
    }

    /**
     * 获取限流总量
     *
     * @return 总量
     */
    public long getRuleTotal() {
        if (!shareInfo.isShareEqual() || shareInfo.isLocal()) {
            return ruleToken.get();
        }
        return ruleToken.get() * instanceCount.get();
    }

    public SlidingWindow getSlidingWindow() {
        return slidingWindow;
    }

    /**
     * 归还配额
     *
     * @param identifier 时间标识
     * @param token 配额
     * @param mode 限流模式
     */
    public void giveBackToken(UpdateIdentifier identifier, long token, TokenBucketMode mode) {
        try (LockWrapper readLock = this.lock.readLock()) {
            readLock.lock();
            //相同则归还，否则忽略
            switch (mode) {
                case REMOTE:
                    if (tokenBucketTimeSet.getLastRemoteUpdateMs().get() == identifier.getLastRemoteUpdateMs().get()) {
                        tokenLeft.addAndGet(token);
                    }
                    break;
                case LOCAL:
                    if (tokenBucketTimeSet.getStageStartMs().get() == identifier.getStageStartMs().get()) {
                        tokenLeft.addAndGet(token);
                    }
                    break;
                case REMOTE_TO_LOCAL:
                    if (tokenBucketTimeSet.getStageStartMs().get() == identifier.getStageStartMs().get()) {
                        remoteToLocalTokenLeft.addAndGet(token);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void updateRemoteClientCount(RemoteQuotaInfo remoteQuotaInfo) {
        long lastRemoteClientUpdateMs = tokenBucketTimeSet.getLastRemoteClientUpdateMs().get();
        if (lastRemoteClientUpdateMs < remoteQuotaInfo.getCurTimeMs()) {
            int lastClientCount;
            int curClientCount;
            if (remoteQuotaInfo.getClientCount() == 0) {
                curClientCount = 1;
            } else {
                curClientCount = remoteQuotaInfo.getClientCount();
            }
            lastClientCount = instanceCount.getAndSet(curClientCount);
            if (lastClientCount != curClientCount) {
                LOG.info("[RateLimit]clientCount change from {} to {}, windowKey {}", lastClientCount,
                        curClientCount,
                        windowKey);
            }
            tokenBucketTimeSet.getLastRemoteClientUpdateMs().set(remoteQuotaInfo.getCurTimeMs());

        }
    }

    /**
     * 同步更新bucket连接的客户端数量
     *
     * @param remoteQuotaInfo 远程结果
     */
    public void syncUpdateRemoteClientCount(RemoteQuotaInfo remoteQuotaInfo) {
        try (LockWrapper writeLock = this.lock.writeLock()) {
            writeLock.lock();
            updateRemoteClientCount(remoteQuotaInfo);
        }
    }

    /**
     * 同步更新远程配额
     *
     * @param remoteQuotaInfo 远程结果
     */
    public void syncUpdateRemoteToken(RemoteQuotaInfo remoteQuotaInfo) {
        try (LockWrapper writeLock = this.lock.writeLock()) {
            writeLock.lock();
            updateRemoteClientCount(remoteQuotaInfo);
            long used = slidingWindow.touchCurrentPassed(remoteQuotaInfo.getCurTimeMs());
            //需要减去上报周期使用的配额数
            tokenLeft.set(remoteQuotaInfo.getRemoteQuotaLeft() - used);
            tokenBucketTimeSet.getLastRemoteUpdateMs().set(remoteQuotaInfo.getCurTimeMs());
        }
    }

    private boolean isRemoteNotExpired(long nowMs) {
        return nowMs - tokenBucketTimeSet.getLastRemoteUpdateMs().get() <= shareInfo.getMinDurationMs();
    }

    private long calculateStageStart(long curTimeMs) {
        return curTimeMs - curTimeMs % validDurationMs;
    }

    private void initLocalStageOnLocalConfig(long nowMs) {
        long nowStageMs = calculateStageStart(nowMs);
        if (tokenBucketTimeSet.getStageStartMs().get() == nowStageMs) {
            return;
        }
        try (LockWrapper writeLock = this.lock.writeLock()) {
            writeLock.lock();
            if (isRemoteNotExpired(nowMs)) {
                return;
            }
            if (tokenBucketTimeSet.getStageStartMs().get() == nowStageMs) {
                return;
            }
            //开始降级到本地限流
            tokenLeft.set(ruleToken.get());
            tokenBucketTimeSet.getStageStartMs().set(nowStageMs);
        }
    }

    private AllocateResult tryAllocateLocal(int token, long nowMs, UpdateIdentifier identifier, AllocateResult result) {
        initLocalStageOnLocalConfig(nowMs);
        try (LockWrapper readLock = this.lock.readLock()) {
            readLock.lock();
            identifier.getStageStartMs().set(tokenBucketTimeSet.getStageStartMs().get());
            identifier.getLastRemoteUpdateMs().set(tokenBucketTimeSet.getLastRemoteUpdateMs().get());
            result.setSuccess(true);
            result.setMode(TokenBucketMode.LOCAL);
            result.setLeft(tokenLeft.addAndGet(-token));
            return result;
        }
    }

    private long directAllocateRemoteToken(int token) {
        return tokenLeft.addAndGet(-token);
    }

    //尝试只读方式分配远程配额
    private AllocateResult allocateRemoteReadOnly(int token, long nowMs, UpdateIdentifier identifier,
            AllocateResult result) {
        try (LockWrapper readLock = this.lock.readLock()) {
            readLock.lock();
            if (isRemoteNotExpired(nowMs)) {
                result.setSuccess(true);
                result.setMode(TokenBucketMode.REMOTE);
                result.setLeft(directAllocateRemoteToken(token));
                return result;
            }
            //远程配置过期，配置了直接放通的场景
            if (shareInfo.isPassOnRemoteFail()) {
                result.setSuccess(true);
                result.setMode(TokenBucketMode.REMOTE_TO_LOCAL);
                result.setLeft(0);
                return result;
            }
            long stageStartMs = tokenBucketTimeSet.getStageStartMs().get();
            if (stageStartMs == calculateStageStart(nowMs)) {
                identifier.getStageStartMs().set(stageStartMs);
                identifier.getLastRemoteUpdateMs().set(tokenBucketTimeSet.getLastRemoteUpdateMs().get());
                result.setSuccess(true);
                result.setMode(TokenBucketMode.REMOTE_TO_LOCAL);
                result.setLeft(remoteToLocalTokenLeft.addAndGet(-token));
                return result;
            }
            result.setSuccess(false);
            result.setMode(TokenBucketMode.REMOTE_TO_LOCAL);
            result.setLeft(0);
            return result;
        }
    }

    //创建远程降级的token池
    private long createRemoteToLocalTokens(long nowMs, int token, UpdateIdentifier identifier, long stageStartMs) {
        long nowStageMs = calculateStageStart(nowMs);
        if (stageStartMs == nowStageMs) {
            identifier.getStageStartMs().set(stageStartMs);
            return remoteToLocalTokenLeft.addAndGet(-token);
        }
        long tokenPerInst = (long) Math.ceil(getRuleTotal() / (double) instanceCount.get());
        if (tokenPerInst == 0) {
            tokenPerInst = 1;
        }
        remoteToLocalTokenLeft.set(tokenPerInst);
        tokenBucketTimeSet.getStageStartMs().set(nowStageMs);
        identifier.getStageStartMs().set(nowMs);
        return remoteToLocalTokenLeft.addAndGet(-token);
    }

    //以本地退化远程模式来进行分配
    private long allocateRemoteToLocal(int token, long nowMs, UpdateIdentifier identifier) {
        //远程配额过期，配置了直接放通
        if (shareInfo.isPassOnRemoteFail()) {
            return 0;
        }
        long stageStartMs = tokenBucketTimeSet.getStageStartMs().get();
        try (LockWrapper readLock = this.lock.readLock()) {
            readLock.lock();
            if (stageStartMs == calculateStageStart(nowMs)) {
                identifier.getStageStartMs().set(stageStartMs);
                identifier.getLastRemoteUpdateMs().set(tokenBucketTimeSet.getLastRemoteUpdateMs().get());
                return remoteToLocalTokenLeft.addAndGet(-token);
            }
        }
        try (LockWrapper writeLock = this.lock.writeLock()) {
            writeLock.lock();
            return createRemoteToLocalTokens(nowMs, token, identifier, stageStartMs);
        }
    }

    //远端分配完整流程
    private AllocateResult tryAllocateRemote(int token, long nowMs, UpdateIdentifier identifier,
            AllocateResult result) {
        AllocateResult allocateResult = allocateRemoteReadOnly(token, nowMs, identifier, result);
        if (allocateResult.isSuccess()) {
            return allocateResult;
        }
        try (LockWrapper writeLock = this.lock.writeLock()) {
            writeLock.lock();
            long stageStartMs = tokenBucketTimeSet.getStageStartMs().get();
            identifier.getLastRemoteUpdateMs().set(tokenBucketTimeSet.getLastRemoteUpdateMs().get());
            if (isRemoteNotExpired(nowMs)) {
                identifier.getStageStartMs().set(stageStartMs);
                result.setSuccess(true);
                result.setMode(TokenBucketMode.REMOTE);
                result.setLeft(tokenLeft.addAndGet(-token));
                return result;
            }
            result.setSuccess(true);
            result.setMode(TokenBucketMode.REMOTE_TO_LOCAL);
            result.setLeft(createRemoteToLocalTokens(nowMs, token, identifier, stageStartMs));
            return result;
        }
    }

    /**
     * 尝试分配配额
     *
     * @param token 所需配额
     * @param nowMs 当前时间点
     * @param identifier 标识
     * @param mode 配额模式
     * @param result 出参，分配结果
     * @return 分配结果
     */
    public AllocateResult tryAllocateToken(TokenBucketMode mode, int token, long nowMs, UpdateIdentifier identifier,
            AllocateResult result) {
        switch (mode) {
            case LOCAL:
                return tryAllocateLocal(token, nowMs, identifier, result);
            case REMOTE:
                result.setSuccess(true);
                result.setMode(TokenBucketMode.REMOTE);
                result.setLeft(directAllocateRemoteToken(token));
                return result;
            case REMOTE_TO_LOCAL:
                result.setSuccess(true);
                result.setMode(TokenBucketMode.REMOTE_TO_LOCAL);
                result.setLeft(allocateRemoteToLocal(token, nowMs, identifier));
                return result;
            default:
                break;
        }
        //自适应计算
        if (shareInfo.isLocal()) {
            return tryAllocateLocal(token, nowMs, identifier, result);
        }
        return tryAllocateRemote(token, nowMs, identifier, result);
    }

    public String getWindowKey() {
        return windowKey;
    }

    /**
     * 记录真实分配的配额
     *
     * @param passed 已分配配额
     * @param nowMs 时间点
     */
    public void confirmPassed(long passed, long nowMs) {
        slidingWindow.addAndGetCurrentPassed(nowMs, passed);
    }

    /**
     * 记录真实限流记录
     *
     * @param limited 已限流记录
     * @param nowMs 时间点
     */
    public void confirmLimited(long limited, long nowMs) {
        slidingWindow.addAndGetCurrentLimited(nowMs, limited);
    }

    public static class AllocateResult {

        private boolean success;

        private TokenBucketMode mode;

        private long left;

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public void setMode(TokenBucketMode mode) {
            this.mode = mode;
        }

        public void setLeft(long left) {
            this.left = left;
        }

        public boolean isSuccess() {
            return success;
        }

        public TokenBucketMode getMode() {
            return mode;
        }

        public long getLeft() {
            return left;
        }
    }

    public int getValidDurationSecond() {
        return validDurationSecond;
    }
}
