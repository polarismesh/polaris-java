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
 * Test for {@link AbortResult}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class AbortResultTest {

    /**
     * 测试构造函数与 getter
     * 测试目的：验证 AbortResult 正确保存 abortCode
     * 测试场景：构造 abortCode=503 的 AbortResult
     * 验证内容：getAbortCode 返回 503
     */
    @Test
    public void testConstructorAndGetAbortCode() {
        // Act
        AbortResult result = new AbortResult(503);

        // Assert
        assertThat(result.getAbortCode()).isEqualTo(503);
    }

    /**
     * 测试 setAbortCode
     * 测试目的：验证 setAbortCode 能修改 abortCode
     * 测试场景：构造后 setAbortCode(404)
     * 验证内容：getAbortCode 返回 404
     */
    @Test
    public void testSetAbortCode() {
        // Arrange
        AbortResult result = new AbortResult(500);

        // Act
        result.setAbortCode(404);

        // Assert
        assertThat(result.getAbortCode()).isEqualTo(404);
    }

    /**
     * 测试 toString 包含 abortCode
     * 测试目的：验证 toString 输出包含 abortCode 值
     * 测试场景：构造后调用 toString
     * 验证内容：toString 包含 abortCode
     */
    @Test
    public void testToString() {
        // Arrange
        AbortResult result = new AbortResult(429);

        // Act
        String str = result.toString();

        // Assert
        assertThat(str).contains("429");
    }
}
