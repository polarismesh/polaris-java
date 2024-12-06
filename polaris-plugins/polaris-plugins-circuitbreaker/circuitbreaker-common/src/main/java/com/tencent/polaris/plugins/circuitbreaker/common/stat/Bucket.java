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

package com.tencent.polaris.plugins.circuitbreaker.common.stat;

import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单个滑桶具体实现
 *
 * @author andrewshan
 * @date 2019/8/25
 */
public class Bucket {

    private static final Logger LOG = LoggerFactory.getLogger(Bucket.class);

    /**
     * 统计序列集合，key为资源ID
     */
    private final ResMetricArray metric;

    /**
     * 滑桶的时间范围
     */
    private final TimeRange timeRange;

    /**
     * 滑桶链总长，只有首节点需要
     */
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * 下一个滑桶
     */
    private final AtomicReference<Bucket> nextBucket = new AtomicReference<>();

    public Bucket(long start, long intervalMs, int metricSize) {
        timeRange = new TimeRange(start, start + intervalMs);
        metric = new ResMetricArray(metricSize);
    }

    public TimeRange getTimeRange() {
        return timeRange;
    }

    public long getMetric(int dimension) {
        return metric.getMetric(dimension);
    }

    public long addMetric(int dimension, long value) {
        return metric.addMetric(dimension, value);
    }

    public void setMetric(int dimension, long value) {
        metric.setMetric(dimension, value);
    }

    public AtomicReference<Bucket> getNextBucket() {
        return nextBucket;
    }

    public AtomicInteger getCount() {
        return count;
    }

    /**
     * 获取最末端的桶
     *
     * @return Bucket
     */
    public Bucket getTailBucket() {
        Bucket head = this;
        Bucket next = head.getNextBucket().get();
        while (null != next) {
            head = next;
            next = head.getNextBucket().get();
        }
        return head;
    }

    /**
     * 计算维度下所有的时间段的值之和
     * 只要startTime或者endTime所存在的区间，或者包含在startTime-endTime之间的区间
     * 比如: 3-----6-----9-----12---15的区间，
     * 对于4-10的时间范围，则返回3-----6-----9-----12
     * 对于2-7的时间范围，则返回3-----6-----9
     *
     * @param dimension 维度下表
     * @param timeRange 时间段
     * @return long
     */
    long calcMetricsBothIncluded(int dimension, TimeRange timeRange) {
        long total = 0L;
        List<Bucket> buckets = new ArrayList<>();
        Bucket next = this;
        LOG.debug("begin calc with dimension {} and time range {}.", dimension, timeRange);
        while (null != next) {
            boolean added = false;
            TimeRange bucketTimeRange = next.getTimeRange();
            TimePosition posStart = bucketTimeRange.isTimeInBucket(timeRange.getStart());
            TimePosition posEnd = bucketTimeRange.isTimeInBucket(timeRange.getEnd());
            TimePosition posBucketRangeStart = timeRange.isTimeInBucket(bucketTimeRange.getStart());
            TimePosition posBucketRangeEnd = timeRange.isTimeInBucket(bucketTimeRange.getEnd());
            if (posStart == TimePosition.inside || posEnd == TimePosition.inside) {
                buckets.add(next);
                added = true;
            } else if (posBucketRangeStart == TimePosition.inside && posBucketRangeEnd == TimePosition.inside) {
                buckets.add(next);
                added = true;
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug("Bucket with dimension {} is {} added metrics {} with time range {}.",
                        dimension, added, next.getMetric(dimension), bucketTimeRange);
            }
            next = next.getNextBucket().get();
        }
        for (Bucket bucket : buckets) {
            total += bucket.getMetric(dimension);
        }
        LOG.debug("end calc with dimension {} and time range {}.", dimension, timeRange);
        return total;
    }
}
