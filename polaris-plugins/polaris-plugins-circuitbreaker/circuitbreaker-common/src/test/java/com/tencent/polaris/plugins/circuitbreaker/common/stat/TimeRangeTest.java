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
 * Test for {@link TimeRange}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class TimeRangeTest {

    /**
     * 测试 isTimeInBucket 时间点在区间之前
     * 测试目的：验证 inputTime < start 返回 before
     * 测试场景：区间 10-20，输入 5
     * 验证内容：返回 before
     */
    @Test
    public void testIsTimeInBucketBefore() {
        // Arrange
        TimeRange range = new TimeRange(10L, 20L);

        // Act
        TimePosition pos = range.isTimeInBucket(5L);

        // Assert
        assertThat(pos).isEqualTo(TimePosition.before);
    }

    /**
     * 测试 isTimeInBucket 时间点在区间内
     * 测试目的：验证 start <= inputTime < end 返回 inside
     * 测试场景：区间 10-20，输入 10 和 19
     * 验证内容：均返回 inside
     */
    @Test
    public void testIsTimeInBucketInside() {
        // Arrange
        TimeRange range = new TimeRange(10L, 20L);

        // Act & Assert
        assertThat(range.isTimeInBucket(10L)).isEqualTo(TimePosition.inside);
        assertThat(range.isTimeInBucket(19L)).isEqualTo(TimePosition.inside);
    }

    /**
     * 测试 isTimeInBucket 时间点等于 end 返回 after
     * 测试目的：验证 inputTime >= end 返回 after（区间右开）
     * 测试场景：区间 10-20，输入 20
     * 验证内容：返回 after
     */
    @Test
    public void testIsTimeInBucketAtEnd() {
        // Arrange
        TimeRange range = new TimeRange(10L, 20L);

        // Act
        TimePosition pos = range.isTimeInBucket(20L);

        // Assert
        assertThat(pos).isEqualTo(TimePosition.after);
    }

    /**
     * 测试 isTimeInBucket 时间点在区间之后
     * 测试目的：验证 inputTime > end 返回 after
     * 测试场景：区间 10-20，输入 25
     * 验证内容：返回 after
     */
    @Test
    public void testIsTimeInBucketAfter() {
        // Arrange
        TimeRange range = new TimeRange(10L, 20L);

        // Act
        TimePosition pos = range.isTimeInBucket(25L);

        // Assert
        assertThat(pos).isEqualTo(TimePosition.after);
    }

    /**
     * 测试 getStart 和 getEnd
     * 测试目的：验证 getter 返回构造时的区间边界
     * 测试场景：构造区间 100-200
     * 验证内容：start=100，end=200
     */
    @Test
    public void testGetStartAndEnd() {
        // Act
        TimeRange range = new TimeRange(100L, 200L);

        // Assert
        assertThat(range.getStart()).isEqualTo(100L);
        assertThat(range.getEnd()).isEqualTo(200L);
    }

    /**
     * 测试 toString 包含 start 和 end
     * 测试目的：验证 toString 输出包含区间边界
     * 测试场景：构造区间 10-20 调用 toString
     * 验证内容：toString 包含 10 和 20
     */
    @Test
    public void testToString() {
        // Arrange
        TimeRange range = new TimeRange(10L, 20L);

        // Act
        String str = range.toString();

        // Assert
        assertThat(str).contains("10");
        assertThat(str).contains("20");
    }
}
