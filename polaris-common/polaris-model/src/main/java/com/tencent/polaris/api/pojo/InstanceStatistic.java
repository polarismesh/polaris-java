package com.tencent.polaris.api.pojo;

import java.util.concurrent.atomic.AtomicLong;

public class InstanceStatistic {

    private final AtomicLong totalCount;
    private final AtomicLong succeededCount;
    private final AtomicLong totalElapsed;
    private final AtomicLong succeededElapsed;
    private final AtomicLong lastSucceededElapsed;
    private final AtomicLong maxElapsed;
    private final AtomicLong failedMaxElapsed;
    private final AtomicLong succeededMaxElapsed;
    private final AtomicLong active =  new AtomicLong(0);

    public InstanceStatistic() {
        this(0, 0, 0, 0, 0, 0, 0, 0);
    }
    public InstanceStatistic(long totalCount, long succeededCount, long totalElapsed, long succeededElapsed,
                             long lastSucceededElapsed, long maxElapsed, long failedMaxElapsed, long succeededMaxElapsed) {
        this.totalCount = new AtomicLong(totalCount);
        this.succeededCount = new AtomicLong(succeededCount);
        this.totalElapsed = new AtomicLong(totalElapsed);
        this.succeededElapsed = new AtomicLong(succeededElapsed);
        this.lastSucceededElapsed = new AtomicLong(lastSucceededElapsed);
        this.maxElapsed = new AtomicLong(maxElapsed);
        this.failedMaxElapsed = new AtomicLong(failedMaxElapsed);
        this.succeededMaxElapsed = new AtomicLong(succeededMaxElapsed);
    }
    public void count(long elapsed, boolean success)  {
        totalCount.incrementAndGet();
        totalElapsed.addAndGet(elapsed);
        maxElapsed.set(Math.max(maxElapsed.get(), elapsed));
        if (success) {
            succeededCount.incrementAndGet();
            succeededElapsed.addAndGet(elapsed);
            lastSucceededElapsed.set(elapsed);
            succeededMaxElapsed.set(Math.max(succeededMaxElapsed.get(), elapsed));
        } else{
            failedMaxElapsed.addAndGet(elapsed);
        }
    }
    public long getTotalCount() {
        return totalCount.get();
    }

    public long getSucceededCount() {
        return succeededCount.get();
    }

    public long getTotalElapsed() {
        return totalElapsed.get();
    }

    public long getSucceededElapsed() {
        return succeededElapsed.get();
    }

    public long getLastSucceededElapsed() {
        return lastSucceededElapsed.get();
    }

    public long getMaxElapsed() {
        return maxElapsed.get();
    }

    public long getFailedMaxElapsed() {
        return failedMaxElapsed.get();
    }

    public long getSucceededMaxElapsed() {
        return succeededMaxElapsed.get();
    }

    public long getActive() {
         return active.get();
    }

    public long getAndIncrementActive() {
        return active.incrementAndGet();
    }

    public long getAndDecrementActive() {
        return active.decrementAndGet();
    }



    @Override
    public String toString() {
        return "InstanceStatistic{" +
                "totalCount=" + totalCount +
                ", succeededCount=" + succeededCount +
                ", totalElapsed=" + totalElapsed +
                ", succeededElapsed=" + succeededElapsed +
                ", lastSucceededElapsed=" + lastSucceededElapsed +
                ", maxElapsed=" + maxElapsed +
                ", failedMaxElapsed=" + failedMaxElapsed +
                ", succeededMaxElapsed=" + succeededMaxElapsed +
                '}';
    }
}