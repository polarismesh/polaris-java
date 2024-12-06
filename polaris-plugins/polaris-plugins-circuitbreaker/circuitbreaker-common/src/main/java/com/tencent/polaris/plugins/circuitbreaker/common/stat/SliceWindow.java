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

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 时间滑窗的具体实现
 *
 * @author andrewshan
 * @date 2019/8/25
 */
public class SliceWindow {

    private static final Logger LOG = LoggerFactory.getLogger(SliceWindow.class);

    private final String name;

    /**
     * 预计滑桶总量
     */
    private final int bucketCount;

    /**
     * 滑桶的长度
     */
    private final long bucketIntervalMs;

    /**
     * 滑窗总时间
     */
    private final long windowLengthMs;

    /**
     * 维度数量
     */
    private final int metricSize;

    /**
     * 全局锁，控制滑桶的移出移入
     */
    private final Object lock = new Object();

    /**
     * 滑桶链表头
     */
    private final AtomicReference<Bucket> headBucket = new AtomicReference<>();

    public SliceWindow(String name, int bucketCount, long bucketIntervalMs, int metricSize) {
        this.name = name;
        this.bucketCount = bucketCount;
        this.bucketIntervalMs = bucketIntervalMs;
        this.metricSize = metricSize;
        windowLengthMs = bucketIntervalMs * bucketCount;
    }

    public int getBucketCount() {
        return bucketCount;
    }

    public long getBucketIntervalMs() {
        return bucketIntervalMs;
    }

    public long getWindowLengthMs() {
        return windowLengthMs;
    }

    private long doAddGauge(Function<Bucket, Long> operation, long now) {
        //需要进行滑窗的变更，不是直接添加
        Bucket head = headBucket.get();
        Bucket tail = null;
        if (null != head) {
            tail = head.getTailBucket();
        }
        if (null != tail && TimePosition.inside == tail.getTimeRange().isTimeInBucket(now)) {
            Long retValue = operation.apply(tail);
            LOG.debug("window {}: add tail bucket {}, now is {}, value is {}", name, tail.getTimeRange(), now,
                    retValue);
            return null != retValue ? retValue : 0;
        }
        if (null == head || null == tail || tail.getTimeRange().getEnd() + windowLengthMs <= now) {
            //首节点为空或者尾部节点已经过期，则新建一个窗口
            Bucket newBucket = new Bucket(now, bucketIntervalMs, metricSize);
            newBucket.getCount().set(1);
            headBucket.set(newBucket);
            LOG.debug("window {}: recreated when adding, new bucket {}, now is {}", name, newBucket.getTimeRange(), now);
            Long value = operation.apply(newBucket);
            return null != value ? value : 0L;
        }
        //计算下一个尾节点的起始和结束位置
        return addNextBucket(operation, head, tail, now);
    }

    private long addNextBucket(Function<Bucket, Long> operation, Bucket head, Bucket tail, long now) {
        //计算下一个尾节点的起始和结束位置
        long lastTailEndTime = tail.getTimeRange().getEnd();
        long nextStartTime = lastTailEndTime + (now - lastTailEndTime) / bucketIntervalMs;
        Bucket newBucket = new Bucket(nextStartTime, bucketIntervalMs, metricSize);
        //添加链表
        tail.getNextBucket().set(newBucket);
        //滑掉过期的节点
        slipExpireBucket(head, newBucket, now);
        Long value = operation.apply(newBucket);
        LOG.debug("window {}: tail add next, tail is {}, new bucket {}, now is {}, value is {}", name,
                tail.getTimeRange(), newBucket.getTimeRange(), now, value);
        return null != value ? value : 0L;
    }

    private void slipExpireBucket(Bucket head, Bucket newBucket, long now) {
        Bucket nextHead = head;
        long headStartTime = nextHead.getTimeRange().getStart();
        long headWindowEndTime = headStartTime + windowLengthMs;
        int slipCount = 0;
        while (headWindowEndTime <= newBucket.getTimeRange().getStart()) {
            nextHead = nextHead.getNextBucket().get();
            if (null == nextHead) {
                break;
            }
            slipCount++;
            headStartTime = nextHead.getTimeRange().getStart();
            headWindowEndTime = headStartTime + windowLengthMs;
        }
        if (null == nextHead) {
            newBucket.getCount().set(1);
            headBucket.set(newBucket);
            LOG.info("window {}: recreated when slip expiring, new bucket {}, now is {}", name, newBucket.getTimeRange(), now);
            return;
        }
        //滑掉之前的窗
        if (slipCount > 0) {
            nextHead.getCount().addAndGet(1 - slipCount);
            headBucket.set(nextHead);
        }
    }

    public long addGauge(Function<Bucket, Long> operation) {
        long now = System.currentTimeMillis();
        Bucket tail = getTailBucket();
        if (null != tail && TimePosition.inside == tail.getTimeRange().isTimeInBucket(now)) {
            Long retValue = operation.apply(tail);
            LOG.debug("window {}: add tail bucket {}, now is {}, value is {}", name, tail.getTimeRange(), now,
                    retValue);
            return null != retValue ? retValue : 0;
        }
        synchronized (lock) {
            return doAddGauge(operation, now);
        }
    }

    public Bucket getHeadBucket() {
        return headBucket.get();
    }

    public Bucket getTailBucket() {
        Bucket head = headBucket.get();
        if (null != head) {
            return head.getTailBucket();
        }
        return null;
    }

    /**
     * 通过首节点计算维度统计信息
     *
     * @param dimension 维度下表
     * @param timeRange 时间段
     * @return long
     */
    public long calcMetricsBothIncluded(int dimension, TimeRange timeRange) {
        Bucket head = getHeadBucket();
        if (null == head) {
            return 0;
        }
        return head.calcMetricsBothIncluded(dimension, timeRange);
    }

}
