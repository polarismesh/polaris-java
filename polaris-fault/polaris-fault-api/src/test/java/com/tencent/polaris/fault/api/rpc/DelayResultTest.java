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

package com.tencent.polaris.fault.api.rpc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link DelayResult}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class DelayResultTest {

    /**
     * 测试构造函数与 getter
     * 测试目的：验证 DelayResult 正确保存 delay
     * 测试场景：构造 delay=200 的 DelayResult
     * 验证内容：getDelay 返回 200
     */
    @Test
    public void testConstructorAndGetDelay() {
        // Act
        DelayResult result = new DelayResult(200);

        // Assert
        assertThat(result.getDelay()).isEqualTo(200);
    }

    /**
     * 测试 setDelay
     * 测试目的：验证 setDelay 能修改 delay
     * 测试场景：构造后 setDelay(500)
     * 验证内容：getDelay 返回 500
     */
    @Test
    public void testSetDelay() {
        // Arrange
        DelayResult result = new DelayResult(100);

        // Act
        result.setDelay(500);

        // Assert
        assertThat(result.getDelay()).isEqualTo(500);
    }

    /**
     * 测试 toString 包含 delay
     * 测试目的：验证 toString 输出包含 delay 值
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含 delay 值
     */
    @Test
    public void testToString() {
        // Arrange
        DelayResult result = new DelayResult(300);

        // Act
        String str = result.toString();

        // Assert
        assertThat(str).contains("300");
    }
}
