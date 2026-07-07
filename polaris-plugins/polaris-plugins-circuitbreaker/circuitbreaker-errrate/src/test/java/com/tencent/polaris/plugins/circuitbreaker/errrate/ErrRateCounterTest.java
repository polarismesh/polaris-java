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

package com.tencent.polaris.plugins.circuitbreaker.errrate;

import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.plugins.circuitbreaker.common.stat.SliceWindow;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ErrRateCounter}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ErrRateCounterTest {

    private Config buildConfig(int buckets) {
        Config config = new Config();
        config.setMetricNumBuckets(buckets);
        return config;
    }

    /**
     * 测试 getSliceWindow 首次创建
     * 测试目的：验证 getSliceWindow 对新维度创建 SliceWindow
     * 测试场景：构造 ErrRateCounter，首次 getSliceWindow
     * 验证内容：返回非 null 的 SliceWindow
     */
    @Test
    public void testGetSliceWindowCreate() {
        // Arrange
        ErrRateCounter counter = new ErrRateCounter("test", buildConfig(5), 1000L);
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;

        // Act
        SliceWindow window = counter.getSliceWindow(dim);

        // Assert
        assertThat(window).isNotNull();
        assertThat(window.getBucketCount()).isEqualTo(5);
        assertThat(window.getBucketIntervalMs()).isEqualTo(1000L);
    }

    /**
     * 测试 getSliceWindow 同维度返回同一实例
     * 测试目的：验证 getSliceWindow 对同一维度返回缓存的同一 SliceWindow
     * 测试场景：对同一维度连续 getSliceWindow 两次
     * 验证内容：两次返回同一实例
     */
    @Test
    public void testGetSliceWindowSameInstance() {
        // Arrange
        ErrRateCounter counter = new ErrRateCounter("test", buildConfig(5), 1000L);
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;

        // Act
        SliceWindow first = counter.getSliceWindow(dim);
        SliceWindow second = counter.getSliceWindow(dim);

        // Assert
        assertThat(first).isSameAs(second);
    }

    /**
     * 测试 getSliceWindow 不同维度返回不同实例
     * 测试目的：验证 getSliceWindow 对不同维度创建独立 SliceWindow
     * 测试场景：对两个不同维度分别 getSliceWindow
     * 验证内容：两次返回不同实例
     */
    @Test
    public void testGetSliceWindowDifferentDimension() {
        // Arrange
        ErrRateCounter counter = new ErrRateCounter("test", buildConfig(5), 1000L);
        StatusDimension dim1 = new StatusDimension("GET", null);
        StatusDimension dim2 = new StatusDimension("POST", null);

        // Act
        SliceWindow w1 = counter.getSliceWindow(dim1);
        SliceWindow w2 = counter.getSliceWindow(dim2);

        // Assert
        assertThat(w1).isNotSameAs(w2);
    }

    /**
     * 测试 getStatusDimensions 返回所有维度
     * 测试目的：验证 getStatusDimensions 包含所有 getSliceWindow 过的维度
     * 测试场景：对两个不同维度 getSliceWindow
     * 验证内容：getStatusDimensions 大小为 2
     */
    @Test
    public void testGetStatusDimensions() {
        // Arrange
        ErrRateCounter counter = new ErrRateCounter("test", buildConfig(5), 1000L);
        counter.getSliceWindow(new StatusDimension("GET", null));
        counter.getSliceWindow(new StatusDimension("POST", null));

        // Act & Assert
        assertThat(counter.getStatusDimensions()).hasSize(2);
    }

    /**
     * 测试 resetCounter 空实现不抛异常
     * 测试目的：验证 resetCounter 当前为空实现，调用不抛异常
     * 测试场景：对任意维度调用 resetCounter
     * 验证内容：不抛出任何异常
     */
    @Test
    public void testResetCounterDoesNotThrow() {
        // Arrange
        ErrRateCounter counter = new ErrRateCounter("test", buildConfig(5), 1000L);
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        counter.getSliceWindow(dim);

        // Act & Assert
        counter.resetCounter(dim);
        assertThat(counter.getSliceWindow(dim)).isNotNull();
    }
}
