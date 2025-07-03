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

package com.tencent.polaris.api.pojo;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Instance Invocation Statistic
 *
 * @author Yuwei Fu
 */

public class InstanceStatistic {

    /**
     * 总调用次数统计
     */
    private final AtomicLong totalCount;
    /**
     * 成功调用次数统计
     */
    private final AtomicLong succeededCount;
    /**
     * 总调用耗时统计
     */
    private final AtomicLong totalElapsed;
    /**
     * 成功调用耗时统计
     */
    private final AtomicLong succeededElapsed;
    /**
     * 最后一次成功调用耗时
     */
    private final AtomicLong lastSucceededElapsed;
    /**
     * 最大调用耗时
     */
    private final AtomicLong maxElapsed;
    /**
     * 失败调用最大耗时
     */
    private final AtomicLong failedMaxElapsed;
    /**
     * 成功调用最大耗时
     */
    private final AtomicLong succeededMaxElapsed;
    /**
     * 当前实例的连接数
     */
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

    public long incrementAndGetActive() {
        return active.incrementAndGet();
    }

    public long decrementAndGetActive() {
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