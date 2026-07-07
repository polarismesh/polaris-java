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
 * Test for {@link ResMetricArray}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ResMetricArrayTest {

    /**
     * 测试初始值为 0
     * 测试目的：验证新构造的 ResMetricArray 各维度初始值为 0
     * 测试场景：构造 metricSize=3 的 ResMetricArray
     * 验证内容：各维度 getMetric 返回 0
     */
    @Test
    public void testInitialValueZero() {
        // Arrange
        ResMetricArray array = new ResMetricArray(3);

        // Act & Assert
        assertThat(array.getMetric(0)).isZero();
        assertThat(array.getMetric(1)).isZero();
        assertThat(array.getMetric(2)).isZero();
    }

    /**
     * 测试 addMetric 累加
     * 测试目的：验证 addMetric 能原子累加并返回新值
     * 测试场景：对维度 0 连续 addMetric
     * 验证内容：每次返回累加后的值
     */
    @Test
    public void testAddMetric() {
        // Arrange
        ResMetricArray array = new ResMetricArray(2);

        // Act
        long first = array.addMetric(0, 5L);
        long second = array.addMetric(0, 3L);

        // Assert
        assertThat(first).isEqualTo(5L);
        assertThat(second).isEqualTo(8L);
        assertThat(array.getMetric(0)).isEqualTo(8L);
    }

    /**
     * 测试 setMetric 覆盖
     * 测试目的：验证 setMetric 能直接覆盖维度值
     * 测试场景：先 add 再 set
     * 验证内容：getMetric 返回 set 的值
     */
    @Test
    public void testSetMetric() {
        // Arrange
        ResMetricArray array = new ResMetricArray(2);
        array.addMetric(1, 10L);

        // Act
        array.setMetric(1, 99L);

        // Assert
        assertThat(array.getMetric(1)).isEqualTo(99L);
    }

    /**
     * 测试各维度独立计数
     * 测试目的：验证不同维度的计数互不影响
     * 测试场景：对维度 0 和维度 1 分别 add
     * 验证内容：各维度值独立
     */
    @Test
    public void testDimensionsIndependent() {
        // Arrange
        ResMetricArray array = new ResMetricArray(2);

        // Act
        array.addMetric(0, 1L);
        array.addMetric(1, 2L);
        array.addMetric(0, 1L);

        // Assert
        assertThat(array.getMetric(0)).isEqualTo(2L);
        assertThat(array.getMetric(1)).isEqualTo(2L);
    }
}
