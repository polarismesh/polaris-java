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

package com.tencent.polaris.plugins.circuitbreaker.common;

import java.util.Set;

import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.StatusDimension;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link HalfOpenCounter}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class HalfOpenCounterTest {

    private static class TestCounter extends HalfOpenCounter {
        @Override
        public Set<StatusDimension> getStatusDimensions() {
            return null;
        }

        @Override
        public void resetCounter(StatusDimension statusDimension) {
        }
    }

    /**
     * 测试 RetFail 达到失败阈值触发转换
     * 测试目的：验证连续失败达到 halfOpenFailCount 时 triggerHalfOpenConversion 返回 true
     * 测试场景：halfOpenFailCount=2，连续两次 RetFail
     * 验证内容：第二次返回 true，第一次返回 false
     */
    @Test
    public void testTriggerHalfOpenConversionFail() {
        // Arrange
        TestCounter counter = new TestCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        HalfOpenConfig config = mock(HalfOpenConfig.class);
        when(config.getHalfOpenFailCount()).thenReturn(2);

        // Act
        boolean first = counter.triggerHalfOpenConversion(dim, RetStatus.RetFail, config);
        boolean second = counter.triggerHalfOpenConversion(dim, RetStatus.RetFail, config);

        // Assert
        assertThat(first).isFalse();
        assertThat(second).isTrue();
    }

    /**
     * 测试 RetTimeout 也计入失败
     * 测试目的：验证 RetTimeout 与 RetFail 一样计入失败计数
     * 测试场景：halfOpenFailCount=2，一次 RetFail + 一次 RetTimeout
     * 验证内容：第二次返回 true
     */
    @Test
    public void testTriggerHalfOpenConversionTimeout() {
        // Arrange
        TestCounter counter = new TestCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        HalfOpenConfig config = mock(HalfOpenConfig.class);
        when(config.getHalfOpenFailCount()).thenReturn(2);

        // Act
        boolean first = counter.triggerHalfOpenConversion(dim, RetStatus.RetTimeout, config);
        boolean second = counter.triggerHalfOpenConversion(dim, RetStatus.RetTimeout, config);

        // Assert
        assertThat(first).isFalse();
        assertThat(second).isTrue();
    }

    /**
     * 测试 RetSuccess 达到成功阈值触发转换
     * 测试目的：验证连续成功达到 halfOpenSuccessCount 时返回 true
     * 测试场景：halfOpenSuccessCount=3，连续三次 RetSuccess
     * 验证内容：第三次返回 true，前两次返回 false
     */
    @Test
    public void testTriggerHalfOpenConversionSuccess() {
        // Arrange
        TestCounter counter = new TestCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        HalfOpenConfig config = mock(HalfOpenConfig.class);
        when(config.getHalfOpenSuccessCount()).thenReturn(3);

        // Act
        boolean first = counter.triggerHalfOpenConversion(dim, RetStatus.RetSuccess, config);
        boolean second = counter.triggerHalfOpenConversion(dim, RetStatus.RetSuccess, config);
        boolean third = counter.triggerHalfOpenConversion(dim, RetStatus.RetSuccess, config);

        // Assert
        assertThat(first).isFalse();
        assertThat(second).isFalse();
        assertThat(third).isTrue();
    }

    /**
     * 测试 RetUnknown 不触发转换
     * 测试目的：验证 RetUnknown 既不计入成功也不计入失败，返回 false
     * 测试场景：halfOpenSuccessCount=1，传入 RetUnknown
     * 验证内容：返回 false
     */
    @Test
    public void testTriggerHalfOpenConversionUnknown() {
        // Arrange
        TestCounter counter = new TestCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        HalfOpenConfig config = mock(HalfOpenConfig.class);

        // Act
        boolean result = counter.triggerHalfOpenConversion(dim, RetStatus.RetUnknown, config);

        // Assert
        assertThat(result).isFalse();
    }

    /**
     * 测试 resetHalfOpen 重置计数
     * 测试目的：验证 resetHalfOpen 后成功/失败计数归零
     * 测试场景：累计一次失败后 resetHalfOpen，再查 getHalfOpenFailCount
     * 验证内容：getHalfOpenFailCount 返回 0
     */
    @Test
    public void testResetHalfOpen() {
        // Arrange
        TestCounter counter = new TestCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        HalfOpenConfig config = mock(HalfOpenConfig.class);
        when(config.getHalfOpenFailCount()).thenReturn(5);
        counter.triggerHalfOpenConversion(dim, RetStatus.RetFail, config);

        // Act
        counter.resetHalfOpen(dim);

        // Assert
        assertThat(counter.getHalfOpenFailCount(dim)).isZero();
    }

    /**
     * 测试成功与失败计数独立
     * 测试目的：验证成功计数和失败计数互不影响
     * 测试场景：一次 RetFail + 一次 RetSuccess
     * 验证内容：失败计数 1，成功计数 1
     */
    @Test
    public void testSuccessAndFailCountIndependent() {
        // Arrange
        TestCounter counter = new TestCounter();
        StatusDimension dim = StatusDimension.EMPTY_DIMENSION;
        HalfOpenConfig config = mock(HalfOpenConfig.class);
        when(config.getHalfOpenFailCount()).thenReturn(5);
        when(config.getHalfOpenSuccessCount()).thenReturn(5);

        // Act
        counter.triggerHalfOpenConversion(dim, RetStatus.RetFail, config);
        counter.triggerHalfOpenConversion(dim, RetStatus.RetSuccess, config);

        // Assert
        assertThat(counter.getHalfOpenFailCount(dim)).isEqualTo(1);
        assertThat(counter.getHalfOpenSuccessCount(dim)).isEqualTo(1);
    }
}
