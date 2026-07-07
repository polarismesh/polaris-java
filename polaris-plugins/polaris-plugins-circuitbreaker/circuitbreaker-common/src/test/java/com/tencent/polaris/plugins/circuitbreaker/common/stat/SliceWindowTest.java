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

import java.util.function.Function;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link SliceWindow}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class SliceWindowTest {

    private static final long LARGE_INTERVAL_MS = 60000L;

    private Function<Bucket, Long> addValue(int dimension, long value) {
        return bucket -> {
            bucket.addMetric(dimension, value);
            return value;
        };
    }

    /**
     * 测试构造函数计算窗口总长
     * 测试目的：验证 windowLengthMs = bucketCount * bucketIntervalMs
     * 测试场景：bucketCount=5，bucketIntervalMs=1000
     * 验证内容：getWindowLengthMs 返回 5000
     */
    @Test
    public void testConstructorWindowLength() {
        // Act
        SliceWindow window = new SliceWindow("test", 5, 1000L, 2);

        // Assert
        assertThat(window.getWindowLengthMs()).isEqualTo(5000L);
        assertThat(window.getBucketCount()).isEqualTo(5);
        assertThat(window.getBucketIntervalMs()).isEqualTo(1000L);
    }

    /**
     * 测试首桶创建
     * 测试目的：验证首次 addGauge 创建首桶并写入值
     * 测试场景：使用大 interval 窗口，addGauge 写入维度 0 值 10
     * 验证内容：返回 10，getHeadBucket 非空且 metric 为 10
     */
    @Test
    public void testAddGaugeFirstBucket() {
        // Arrange
        SliceWindow window = new SliceWindow("test", 5, LARGE_INTERVAL_MS, 2);

        // Act
        long value = window.addGauge(addValue(0, 10L));

        // Assert
        assertThat(value).isEqualTo(10L);
        assertThat(window.getHeadBucket()).isNotNull();
        assertThat(window.getHeadBucket().getMetric(0)).isEqualTo(10L);
    }

    /**
     * 测试 tail 桶命中直接累加
     * 测试目的：验证同一时间桶内连续 addGauge 直接写入 tail 桶
     * 测试场景：使用大 interval 窗口，连续两次 addGauge
     * 验证内容：tail 桶累加值为两次之和
     */
    @Test
    public void testAddGaugeTailBucketHit() {
        // Arrange
        SliceWindow window = new SliceWindow("test", 5, LARGE_INTERVAL_MS, 2);

        // Act
        window.addGauge(addValue(0, 10L));
        long second = window.addGauge(addValue(0, 5L));

        // Assert
        assertThat(second).isEqualTo(5L);
        assertThat(window.getTailBucket().getMetric(0)).isEqualTo(15L);
    }

    /**
     * 测试 calcMetricsBothIncluded 空窗口返回 0
     * 测试目的：验证无桶时 calcMetricsBothIncluded 返回 0
     * 测试场景：新构造窗口不 addGauge，直接 calcMetricsBothIncluded
     * 验证内容：返回 0
     */
    @Test
    public void testCalcMetricsBothIncludedEmptyWindow() {
        // Arrange
        SliceWindow window = new SliceWindow("test", 5, 1000L, 2);

        // Act
        long total = window.calcMetricsBothIncluded(0, new TimeRange(0L, 5000L));

        // Assert
        assertThat(total).isZero();
    }

    /**
     * 测试 calcMetricsBothIncluded 汇总桶值
     * 测试目的：验证 calcMetricsBothIncluded 能汇总窗口内桶的值
     * 测试场景：使用大 interval 窗口，连续 addGauge 两次写入同一桶，再 calc
     * 验证内容：返回两次累加值
     */
    @Test
    public void testCalcMetricsBothIncludedSum() {
        // Arrange
        SliceWindow window = new SliceWindow("test", 5, LARGE_INTERVAL_MS, 2);
        window.addGauge(addValue(0, 3L));
        window.addGauge(addValue(0, 4L));
        Bucket head = window.getHeadBucket();
        long start = head.getTimeRange().getStart();
        long end = head.getTimeRange().getEnd();

        // Act
        long total = window.calcMetricsBothIncluded(0, new TimeRange(start, end));

        // Assert
        assertThat(total).isEqualTo(7L);
    }

    /**
     * 测试 getTailBucket 空窗口返回 null
     * 测试目的：验证无桶时 getTailBucket 返回 null
     * 测试场景：新构造窗口不 addGauge
     * 验证内容：getTailBucket 为 null
     */
    @Test
    public void testGetTailBucketEmpty() {
        // Arrange
        SliceWindow window = new SliceWindow("test", 5, 1000L, 2);

        // Act
        Bucket tail = window.getTailBucket();

        // Assert
        assertThat(tail).isNull();
    }

    /**
     * 测试 getHeadBucket 空窗口返回 null
     * 测试目的：验证无桶时 getHeadBucket 返回 null
     * 测试场景：新构造窗口不 addGauge
     * 验证内容：getHeadBucket 为 null
     */
    @Test
    public void testGetHeadBucketEmpty() {
        // Arrange
        SliceWindow window = new SliceWindow("test", 5, 1000L, 2);

        // Act
        Bucket head = window.getHeadBucket();

        // Assert
        assertThat(head).isNull();
    }

    /**
     * 测试 addGauge 多维度独立计数
     * 测试目的：验证同一桶内不同维度的计数互不影响
     * 测试场景：使用大 interval 窗口，对维度 0 和维度 1 分别 addGauge
     * 验证内容：tail 桶两维度值独立
     */
    @Test
    public void testAddGaugeMultipleDimensions() {
        // Arrange
        SliceWindow window = new SliceWindow("test", 5, LARGE_INTERVAL_MS, 2);

        // Act
        window.addGauge(addValue(0, 10L));
        window.addGauge(addValue(1, 20L));

        // Assert
        assertThat(window.getTailBucket().getMetric(0)).isEqualTo(10L);
        assertThat(window.getTailBucket().getMetric(1)).isEqualTo(20L);
    }
}
