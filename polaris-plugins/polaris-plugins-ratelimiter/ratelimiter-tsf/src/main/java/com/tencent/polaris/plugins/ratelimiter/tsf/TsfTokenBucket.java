package com.tencent.polaris.plugins.ratelimiter.tsf;

import com.google.common.base.Ticker;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.ratelimiter.*;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;


public class TsfTokenBucket implements QuotaBucket {

    private static final Logger LOG = LoggerFactory.getLogger(TsfTokenBucket.class);

    private RateLimitProto.Rule rule;

    private long duration;
    private long durationInNanos; // 周期长度，单位是纳秒

    // 当前周期的开始时间（Nanosecond)，与 UNIX timestamp 无关
    private long currentPeriodStartAt;

    private long capacity; // 令牌桶的大小
    private long size; // 令牌桶的令牌存量
    private long qps;
    private long capacityDebuffForTokenRefill; // 用来影响令牌发送速度，并不影响令牌桶大小

    private Ticker ticker = Ticker.systemTicker();
    private long lastRefillTime; // 上一次充令牌的时间

    // [上报用] 上报所用的标识
    private String reportId;

    private AtomicLong usedCount = new AtomicLong(0);
    private AtomicLong limitedCount = new AtomicLong(0);

    /**
     * 初始化一个令牌桶。
     */
    public TsfTokenBucket(InitCriteria initCriteria) {
        this.rule = initCriteria.getRule();

        long absoluteNow = System.currentTimeMillis();
        long relativeNow = ticker.read();
        duration = initCriteria.getRule().getAmounts(0).getValidDuration().getSeconds();
        long durationInMillis = TimeUnit.SECONDS.toMillis(duration);
        long offset = absoluteNow % durationInMillis;
        this.currentPeriodStartAt = relativeNow - TimeUnit.MILLISECONDS.toNanos(offset);
        this.lastRefillTime = relativeNow;
        this.durationInNanos = TimeUnit.SECONDS.toNanos(duration);
        this.capacity = initCriteria.getRule().getAmounts(0).getMaxAmount().getValue();
        this.size = capacity / 2;
        this.qps = capacity / duration;
        this.capacityDebuffForTokenRefill = 0;
        this.reportId = initCriteria.getRule().getId().getValue();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Bucket {} is created at absolute[{}] and relative[{}]. " +
                            "durationInMillis: {}, " +
                            "offset:{}, " +
                            "currentPeriodStartAt: {}, " +
                            "lastRefillTime: {}, " +
                            "durationInNanos: {}, " +
                            "capacity: {}, " +
                            "size: {}, " +
                            "capacityDebuffForTokenRefill: {}.",
                    reportId, absoluteNow, relativeNow, durationInMillis, offset, currentPeriodStartAt, lastRefillTime,
                    durationInNanos, this.capacity, size, capacityDebuffForTokenRefill);
        }
    }

    public TsfTokenBucket() {
    }

    // by andrew: 单测使用，仅包内可见
    TsfTokenBucket(long capacity, long duration, TimeUnit durationUnit, String reportId,
                   Ticker ticker, long currentPeriodStartAt, long lastRefillTime) {
        this.ticker = ticker;

        this.currentPeriodStartAt = currentPeriodStartAt;
        this.lastRefillTime = lastRefillTime;
        this.durationInNanos = durationUnit.toNanos(duration);
        this.capacity = capacity;
        this.size = capacity / 2;
        this.capacityDebuffForTokenRefill = 0;
        this.reportId = reportId;

        LOG.info("For test: Bucket {} is created]. " +
                        "currentPeriodStartAt: {}, " +
                        "lastRefillTime: {}, " +
                        "durationInNanos: {}, " +
                        "capacity: {}, " +
                        "size: {}, " +
                        "capacityDebuffForTokenRefill: {}.",
                reportId, currentPeriodStartAt, lastRefillTime,
                durationInNanos, this.capacity, size, capacityDebuffForTokenRefill);
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public long getCapacityDebuffForTokenRefill() {
        return capacityDebuffForTokenRefill;
    }

    public void setCapacityDebuffForTokenRefill(long capacityDebuffForTokenRefill) {
        this.capacityDebuffForTokenRefill = capacityDebuffForTokenRefill;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "TsfTokenBucket{" + "duration=" + TimeUnit.NANOSECONDS.toSeconds(durationInNanos) + "s" + ", capacity="
                + capacity + ", size=" + size + '}';
    }

    @Override
    public synchronized QuotaResult allocateQuota(long curTimeMs, int numTokens) throws PolarisException {
        checkArgument(numTokens > 0, "Number of tokens to consume must be positive");
        checkArgument(numTokens <= capacity,
                "Number of tokens to consume must be less than the capacity of the bucket.");

        QuotaResult response;
        refillAndSyncPeriod();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Bucket {} will consume {} token(s) from {}.", reportId, numTokens, size);
        }
        if (numTokens <= size) {
            size -= numTokens;
            response = new QuotaResult(QuotaResult.Code.QuotaResultOk, 0, "");
            usedCount.incrementAndGet();
        } else {
            String info = RateLimitProto.Rule.Resource.QPS + ":" + qps;
            response = new QuotaResult(QuotaResult.Code.QuotaResultLimited, 0, info);
            limitedCount.incrementAndGet();
        }
        return response;
    }

    @Override
    public void returnQuota(long allocateTimeMs, int count) throws PolarisException {
        checkArgument(count > 0, "Number of tokens to return must be positive");

        refillAndSyncPeriod();

        if (LOG.isDebugEnabled()) {
            LOG.info("Bucket {} will return {} token(s) to {}.", reportId, count, size);
        }
        size = Math.min(capacity, size + count);
        usedCount.decrementAndGet();
    }

    public synchronized void refillAndSyncPeriod() {
        // 补充 token，以及维护周期变化时的逻辑
        //
        // 这次场景下应该先 refill，以保证上一次 refill 到这次，不会有升降额带来的速率变化：
        // - 升降额前
        // - 消费 token 时
        long now = ticker.read();

        // 因为降额操作只对本周期实施，需要区分本周期的时间流逝和下周期开始的时间流逝
        long timeElapsedInCurrentPeriodAfterLastRefill, timeElapsedInNextPeriods;

        if (now > currentPeriodStartAt + durationInNanos) {
            // 跨周期了
            // [1]: timeElapsedInCurrentPeriodAfterLastRefill
            // [2]: timeElapsedInNextPeriods
            //
            //                     [2]
            //      |<-- [1]  -->|<-->|
            // |-----------------|-----------------|
            //      ^                 ^
            //      |                now
            //  lastRefillTime
            timeElapsedInCurrentPeriodAfterLastRefill = currentPeriodStartAt + durationInNanos - lastRefillTime;
            timeElapsedInNextPeriods = now - (currentPeriodStartAt + durationInNanos);
        } else {
            // 没有跨周期
            // [1]: timeElapsedInCurrentPeriodAfterLastRefill
            //
            //      |<- [1] ->|
            // |-----------------|-----------------|
            //      ^         ^
            //      |        now
            //  lastRefillTime
            timeElapsedInCurrentPeriodAfterLastRefill = now - lastRefillTime;
            timeElapsedInNextPeriods = 0;
        }

        double ratioTimeElapsedInCurrentPeriodAfterLastRefill = (double) timeElapsedInCurrentPeriodAfterLastRefill
                / durationInNanos;
        double ratioTimeElapsedInNextPeriods = (double) timeElapsedInNextPeriods / durationInNanos;

        long virtualCapacity = capacity - capacityDebuffForTokenRefill;
        long numTokensToFillInCurrentPeriod = (long) (ratioTimeElapsedInCurrentPeriodAfterLastRefill * virtualCapacity);
        long numTokensToFillInNextPeriods = (long) ratioTimeElapsedInNextPeriods * capacity;
        long numTokensToFill = numTokensToFillInCurrentPeriod + numTokensToFillInNextPeriods;

        long oldSize = size;
        size = Math.min(capacity, size + numTokensToFill);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Bucket {} is refilled from {} to {} at {}. " +
                            "currentPeriodStartAt: {}, " +
                            "durationInNanos:{}, " +
                            "lastRefillTime: {}, " +
                            "capacity: {}, " +
                            "capacityDebuffForTokenRefill: {}, " +
                            "virtualCapacity: {}, " +
                            "ratioTimeElapsedInCurrentPeriodAfterLastRefill: {}, " +
                            "ratioTimeElapsedInNextPeriods: {}, " +
                            "numTokensToFillInCurrentPeriod: {}, " +
                            "numTokensToFillInNextPeriods: {}",
                    reportId, oldSize, size, now, currentPeriodStartAt, durationInNanos, lastRefillTime, capacity,
                    capacityDebuffForTokenRefill, virtualCapacity, ratioTimeElapsedInCurrentPeriodAfterLastRefill,
                    ratioTimeElapsedInNextPeriods, numTokensToFillInCurrentPeriod, numTokensToFillInNextPeriods);
        }
        if (virtualCapacity != 0) {
            // [带 debuff 的充值过程，耗费的时间] = token 折算成时间比例[1] x 整段时间 durationInNanos
            // [不带 debuff 的充值过程，耗费的时间] = token 折算成时间比例[2] x 整段时间 durationInNanos
            // [1]: numTokensToFillInCurrentPeriod / virtualCapacity
            // [2]: numTokensToFillInNextPeriods / capacity
            lastRefillTime += numTokensToFillInCurrentPeriod * durationInNanos / virtualCapacity
                    + numTokensToFillInNextPeriods * durationInNanos / capacity;
        } else {
            // debuff 最大时，当前周期都产生不了新 token，于是白白浪费掉
            lastRefillTime += timeElapsedInCurrentPeriodAfterLastRefill
                    + numTokensToFillInNextPeriods * durationInNanos / capacity;
        }

        long numPeriodsElapsed = (now - currentPeriodStartAt) / durationInNanos;
        if (numPeriodsElapsed > 0) {
            // 跨周期了，把升降额因子调整下
            capacityDebuffForTokenRefill = 0;
            currentPeriodStartAt += numPeriodsElapsed * durationInNanos;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Bucket {} new lastRefillTime is {}. numPeriodsElapsed: {}. capacityDebuffForTokenRefill: {}. New currentPeriodStartAt: {}.",
                    reportId, lastRefillTime, numPeriodsElapsed, capacityDebuffForTokenRefill, currentPeriodStartAt);
        }
    }

    @Override
    public void release() {

    }

    @Override
    public synchronized void onRemoteUpdate(RemoteQuotaInfo remoteQuotaInfo) {
        long newCapacity = remoteQuotaInfo.getRemoteQuotaLeft();

        // 周期相同，做升额降额操作。
        // 升降额操作的目标是把多出来的或者减掉的令牌数，合理地分配到 已有的令牌桶 和 待发放的令牌桶中。
        //
        // 升额时：
        // - 可使用 += (周期内已流逝时间 / 周期时间) * 增量
        // - 待发放 += (周期内剩余时间 / 周期时间) * 增量
        // - 发放速率上升到新配额
        //
        // 降额时：
        // - if (可使用+待发放) < 减量，则 可使用 => 0，待发送 => 0，这个周期不再对用户服务
        // - 可使用 -= (可使用 / (可使用+待发放)) * 减量
        // - 发放速度下降到新配额
        refillAndSyncPeriod();

        if (newCapacity == this.capacity) {
            return;
        }

        long now = ticker.read();
        // 因为前面 refillAndSyncPeriod() 了，只有非常少见的情况下会大于 1.0
        double ratioCurrentPeriodPassed = Math.min((double) (now - currentPeriodStartAt) / durationInNanos, 1.0);

        long capacityDelta = newCapacity - this.capacity;
        if (capacityDelta > 0) {
            long tokensFilled = (long) (ratioCurrentPeriodPassed * capacityDelta);
            this.size += tokensFilled;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Bucket {} newCapacity: {}. new size: {}. ratioCurrentPeriodPassed: {}. capacityDelta: {}. tokensFilled: {}. now: {}.",
                        reportId, newCapacity, size, ratioCurrentPeriodPassed, capacityDelta, tokensFilled, now);
            }
        } else {
            long numTokenToReturn = -capacityDelta;

            double tokensToFillInCurrentPeriod = (1.0 - ratioCurrentPeriodPassed) * capacity;
            if (this.size + tokensToFillInCurrentPeriod < numTokenToReturn) {
                long oldSize = this.size;
                this.size = 0;
                this.capacityDebuffForTokenRefill = -newCapacity;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Bucket {} newCapacity: {}. old size: {}. ratioCurrentPeriodPassed: {}. capacityDelta: {}. " +
                                    "tokensToFillInCurrentPeriod: {}. capacityDebuffForTokenRefill: {}. now: {}.",
                            reportId, newCapacity, oldSize, ratioCurrentPeriodPassed, capacityDelta,
                            tokensToFillInCurrentPeriod, capacityDebuffForTokenRefill, now);
                }
            } else {
                // 当前库存应该出的比例
                double ratioBucketOffered = (double) size / (size + tokensToFillInCurrentPeriod);
                // by andrew: tokensToReturnInBucket需要使用浮点数，保持与tokensToReturnInTheFuture一致。
                // 否则会出现因丢失精度导致后续的capacityDebuffForTokenRefill的计算会出现值被放大的情况
                double tokensToReturnInBucket = Math.max(
                        // 按比例扣
                        numTokenToReturn * ratioBucketOffered,
                        // 如果 size 比较满，应该扣 size 多一点，按比例扣之后 size 可能会大于 capacity。此时应该扣 size 多一点
                        (double) numTokenToReturn - (capacity - size));
                double tokensToReturnInTheFuture = numTokenToReturn - tokensToReturnInBucket;

                this.size -= (long) tokensToReturnInBucket;
                // by andrew: 由于后续的capacityDebuffForTokenRefill需要被newCapacity给扣减，所以这里计算Debuff需要用newCapacity作为分子来计算
                // 才可以保证defbuff不会超过newCapacity，这样后续的size计算才不会出现负值导致全限流。
                this.capacityDebuffForTokenRefill = (long) (tokensToReturnInTheFuture
                        / tokensToFillInCurrentPeriod * newCapacity);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Bucket {} NewCapacity: {}. size: {}. ratioCurrentPeriodPassed: {}. capacityDelta: {}. " +
                                    "tokensToFillInCurrentPeriod: {}. ratioBucketOffered: {}. " +
                                    "tokensToReturnInBucket: {}. capacityDebuffForTokenRefill: {}. now: {}.",
                            reportId, newCapacity, size, ratioCurrentPeriodPassed, capacityDelta, tokensToFillInCurrentPeriod,
                            ratioBucketOffered, tokensToReturnInBucket, capacityDebuffForTokenRefill, now);
                }
            }
        }
        this.capacity = newCapacity;
    }

    @Override
    public Map<Integer, LocalQuotaInfo> fetchLocalUsage(long curTimeMs) {
        long used = usedCount.getAndSet(0);
        long limited = limitedCount.getAndSet(0);
        Map<Integer, LocalQuotaInfo> result = new HashMap<>();
        result.put((int) duration, new LocalQuotaInfo(used, limited));
        return result;
    }

    @Override
    public Map<Integer, AmountInfo> getAmountInfo() {
        return null;
    }
}
