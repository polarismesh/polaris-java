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

package com.tencent.polaris.plugins.circuitbreaker.errcount;

import com.tencent.polaris.api.pojo.StatusDimension;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConsecutiveCounter}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsecutiveCounterTest {

    /**
     * 测试 onFail 累加连续错误计数
     * 测试目的：验证 onFail 能累加并返回当前连续错误数
     * 测试场景：连续调用 onFail 三次
     * 验证内容：返回值依次为 1、2、3
     */
    @Test
    public void testOnFailIncrement() {
        // Arrange
        ConsecutiveCounter counter = new ConsecutiveCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;

        // Act & Assert
        assertThat(counter.onFail(dim)).isEqualTo(1);
        assertThat(counter.onFail(dim)).isEqualTo(2);
        assertThat(counter.onFail(dim)).isEqualTo(3);
    }

    /**
     * 测试 getConsecutiveErrorCount 读取计数
     * 测试目的：验证 getConsecutiveErrorCount 返回当前连续错误数
     * 测试场景：onFail 两次后读取
     * 验证内容：返回 2
     */
    @Test
    public void testGetConsecutiveErrorCount() {
        // Arrange
        ConsecutiveCounter counter = new ConsecutiveCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        counter.onFail(dim);
        counter.onFail(dim);

        // Act & Assert
        assertThat(counter.getConsecutiveErrorCount(dim)).isEqualTo(2);
    }

    /**
     * 测试 resetCounter 清零
     * 测试目的：验证 resetCounter 能将连续错误计数归零
     * 测试场景：onFail 两次后 resetCounter
     * 验证内容：getConsecutiveErrorCount 返回 0
     */
    @Test
    public void testResetCounter() {
        // Arrange
        ConsecutiveCounter counter = new ConsecutiveCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        counter.onFail(dim);
        counter.onFail(dim);

        // Act
        counter.resetCounter(dim);

        // Assert
        assertThat(counter.getConsecutiveErrorCount(dim)).isZero();
    }

    /**
     * 测试不同维度的计数独立
     * 测试目的：验证不同 StatusDimension 的连续错误计数互不影响
     * 测试场景：dim1 onFail 两次，dim2 onFail 一次
     * 验证内容：dim1 计数 2，dim2 计数 1
     */
    @Test
    public void testDimensionsIndependent() {
        // Arrange
        ConsecutiveCounter counter = new ConsecutiveCounter();
        StatusDimension dim1 = new StatusDimension("GET", null);
        StatusDimension dim2 = new StatusDimension("POST", null);

        // Act
        counter.onFail(dim1);
        counter.onFail(dim1);
        counter.onFail(dim2);

        // Assert
        assertThat(counter.getConsecutiveErrorCount(dim1)).isEqualTo(2);
        assertThat(counter.getConsecutiveErrorCount(dim2)).isEqualTo(1);
    }

    /**
     * 测试 getStatusDimensions 返回所有维度
     * 测试目的：验证 getStatusDimensions 包含所有 onFail 过的维度
     * 测试场景：对两个不同维度 onFail
     * 验证内容：getStatusDimensions 大小为 2
     */
    @Test
    public void testGetStatusDimensions() {
        // Arrange
        ConsecutiveCounter counter = new ConsecutiveCounter();
        StatusDimension dim1 = new StatusDimension("GET", null);
        StatusDimension dim2 = new StatusDimension("POST", null);

        // Act
        counter.onFail(dim1);
        counter.onFail(dim2);

        // Assert
        assertThat(counter.getStatusDimensions()).hasSize(2);
    }
}
