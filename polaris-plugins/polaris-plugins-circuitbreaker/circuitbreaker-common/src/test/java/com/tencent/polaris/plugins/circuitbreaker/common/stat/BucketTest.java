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

package com.tencent.polaris.plugins.circuitbreaker.common.stat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Bucket}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class BucketTest {

    /**
     * 测试 getTimeRange 构造的区间
     * 测试目的：验证 Bucket 构造函数正确计算 timeRange
     * 测试场景：start=100，intervalMs=10 构造桶
     * 验证内容：timeRange 的 start=100，end=110
     */
    @Test
    public void testGetTimeRange() {
        // Act
        Bucket bucket = new Bucket(100L, 10L, 2);

        // Assert
        assertThat(bucket.getTimeRange().getStart()).isEqualTo(100L);
        assertThat(bucket.getTimeRange().getEnd()).isEqualTo(110L);
    }

    /**
     * 测试 addMetric 和 getMetric
     * 测试目的：验证 Bucket 委托 ResMetricArray 进行计数
     * 测试场景：对维度 0 addMetric 两次
     * 验证内容：getMetric 返回累加值
     */
    @Test
    public void testAddAndGetMetric() {
        // Arrange
        Bucket bucket = new Bucket(0L, 10L, 2);

        // Act
        bucket.addMetric(0, 5L);
        bucket.addMetric(0, 3L);

        // Assert
        assertThat(bucket.getMetric(0)).isEqualTo(8L);
    }

    /**
     * 测试 setMetric 覆盖
     * 测试目的：验证 setMetric 直接覆盖维度值
     * 测试场景：先 add 再 set
     * 验证内容：getMetric 返回 set 的值
     */
    @Test
    public void testSetMetric() {
        // Arrange
        Bucket bucket = new Bucket(0L, 10L, 2);
        bucket.addMetric(1, 10L);

        // Act
        bucket.setMetric(1, 42L);

        // Assert
        assertThat(bucket.getMetric(1)).isEqualTo(42L);
    }

    /**
     * 测试 getTailBucket 单桶返回自身
     * 测试目的：验证无后继桶时 getTailBucket 返回自身
     * 测试场景：构造单桶
     * 验证内容：getTailBucket 返回该桶
     */
    @Test
    public void testGetTailBucketSingle() {
        // Arrange
        Bucket bucket = new Bucket(0L, 10L, 1);

        // Act
        Bucket tail = bucket.getTailBucket();

        // Assert
        assertThat(tail).isSameAs(bucket);
    }

    /**
     * 测试 getTailBucket 多桶链表
     * 测试目的：验证 getTailBucket 能遍历到链表末尾
     * 测试场景：构造 head->b1->b2 三桶链表
     * 验证内容：getTailBucket 返回 b2
     */
    @Test
    public void testGetTailBucketChain() {
        // Arrange
        Bucket head = new Bucket(0L, 10L, 1);
        Bucket mid = new Bucket(10L, 10L, 1);
        Bucket tail = new Bucket(20L, 10L, 1);
        head.getNextBucket().set(mid);
        mid.getNextBucket().set(tail);

        // Act
        Bucket result = head.getTailBucket();

        // Assert
        assertThat(result).isSameAs(tail);
    }

    /**
     * 测试 calcMetricsBothIncluded 累加区间内桶的值
     * 测试目的：验证 calcMetricsBothIncluded 能汇总落在查询区间内的桶
     * 测试场景：构造三桶链表（区间 0-10、10-20、20-30），查询区间 5-25
     * 验证内容：累加三桶的维度 0 值
     */
    @Test
    public void testCalcMetricsBothIncluded() {
        // Arrange
        Bucket head = new Bucket(0L, 10L, 1);
        Bucket mid = new Bucket(10L, 10L, 1);
        Bucket tail = new Bucket(20L, 10L, 1);
        head.getNextBucket().set(mid);
        mid.getNextBucket().set(tail);
        head.addMetric(0, 1L);
        mid.addMetric(0, 2L);
        tail.addMetric(0, 4L);

        // Act
        long total = head.calcMetricsBothIncluded(0, new TimeRange(5L, 25L));

        // Assert
        assertThat(total).isEqualTo(7L);
    }

    /**
     * 测试 calcMetricsBothIncluded 查询区间完全包含某桶
     * 测试目的：验证查询区间完全包住一个桶时该桶被计入
     * 测试场景：单桶区间 10-20，查询区间 0-30
     * 验证内容：返回该桶的值
     */
    @Test
    public void testCalcMetricsBothIncludedBucketInsideRange() {
        // Arrange
        Bucket head = new Bucket(10L, 10L, 1);
        head.addMetric(0, 8L);

        // Act
        long total = head.calcMetricsBothIncluded(0, new TimeRange(0L, 30L));

        // Assert
        assertThat(total).isEqualTo(8L);
    }

    /**
     * 测试 calcMetricsBothIncluded 查询区间不重叠
     * 测试目的：验证查询区间与所有桶都不重叠时返回 0
     * 测试场景：单桶区间 10-20，查询区间 30-40
     * 验证内容：返回 0
     */
    @Test
    public void testCalcMetricsBothIncludedNoOverlap() {
        // Arrange
        Bucket head = new Bucket(10L, 10L, 1);
        head.addMetric(0, 8L);

        // Act
        long total = head.calcMetricsBothIncluded(0, new TimeRange(30L, 40L));

        // Assert
        assertThat(total).isZero();
    }
}
