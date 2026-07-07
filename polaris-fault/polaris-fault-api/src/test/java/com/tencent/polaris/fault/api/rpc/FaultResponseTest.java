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
 * Test for {@link FaultResponse}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class FaultResponseTest {

    /**
     * 测试单参数构造函数
     * 测试目的：验证仅传 isFaultInjected 时 abortResult/delayResult 为 null
     * 测试场景：构造 isFaultInjected=true 的 FaultResponse
     * 验证内容：isFaultInjected 为 true，abortResult/delayResult 为 null
     */
    @Test
    public void testConstructorSingleArg() {
        // Act
        FaultResponse response = new FaultResponse(true);

        // Assert
        assertThat(response.isFaultInjected()).isTrue();
        assertThat(response.getAbortResult()).isNull();
        assertThat(response.getDelayResult()).isNull();
    }

    /**
     * 测试 isFaultInjected=false 的单参数构造
     * 测试目的：验证非故障注入响应
     * 测试场景：构造 isFaultInjected=false 的 FaultResponse
     * 验证内容：isFaultInjected 为 false
     */
    @Test
    public void testConstructorSingleArgNotInjected() {
        // Act
        FaultResponse response = new FaultResponse(false);

        // Assert
        assertThat(response.isFaultInjected()).isFalse();
    }

    /**
     * 测试带 AbortResult 的构造函数
     * 测试目的：验证传入 AbortResult 时 delayResult 为 null
     * 测试场景：构造带 AbortResult 的 FaultResponse
     * 验证内容：abortResult 与入参一致，delayResult 为 null
     */
    @Test
    public void testConstructorWithAbortResult() {
        // Arrange
        AbortResult abortResult = new AbortResult(503);

        // Act
        FaultResponse response = new FaultResponse(true, abortResult);

        // Assert
        assertThat(response.isFaultInjected()).isTrue();
        assertThat(response.getAbortResult()).isEqualTo(abortResult);
        assertThat(response.getDelayResult()).isNull();
    }

    /**
     * 测试带 DelayResult 的构造函数
     * 测试目的：验证传入 DelayResult 时 abortResult 为 null
     * 测试场景：构造带 DelayResult 的 FaultResponse
     * 验证内容：delayResult 与入参一致，abortResult 为 null
     */
    @Test
    public void testConstructorWithDelayResult() {
        // Arrange
        DelayResult delayResult = new DelayResult(200);

        // Act
        FaultResponse response = new FaultResponse(true, delayResult);

        // Assert
        assertThat(response.isFaultInjected()).isTrue();
        assertThat(response.getDelayResult()).isEqualTo(delayResult);
        assertThat(response.getAbortResult()).isNull();
    }

    /**
     * 测试全参数构造函数
     * 测试目的：验证同时传入 AbortResult 和 DelayResult
     * 测试场景：构造带两种结果的全参数 FaultResponse
     * 验证内容：abortResult 和 delayResult 均与入参一致
     */
    @Test
    public void testConstructorWithAllArgs() {
        // Arrange
        AbortResult abortResult = new AbortResult(500);
        DelayResult delayResult = new DelayResult(100);

        // Act
        FaultResponse response = new FaultResponse(true, abortResult, delayResult);

        // Assert
        assertThat(response.getAbortResult()).isEqualTo(abortResult);
        assertThat(response.getDelayResult()).isEqualTo(delayResult);
    }

    /**
     * 测试 setFaultInjected 修改注入标记
     * 测试目的：验证 setFaultInjected 能覆盖原值
     * 测试场景：构造 true 后 setFaultInjected(false)
     * 验证内容：isFaultInjected 变为 false
     */
    @Test
    public void testSetFaultInjected() {
        // Arrange
        FaultResponse response = new FaultResponse(true);

        // Act
        response.setFaultInjected(false);

        // Assert
        assertThat(response.isFaultInjected()).isFalse();
    }

    /**
     * 测试 setAbortResult 和 setDelayResult
     * 测试目的：验证 setter 能替换原结果
     * 测试场景：构造后分别 setAbortResult 和 setDelayResult
     * 验证内容：getter 返回新设置的值
     */
    @Test
    public void testSetAbortAndDelayResult() {
        // Arrange
        FaultResponse response = new FaultResponse(true);
        AbortResult abortResult = new AbortResult(404);
        DelayResult delayResult = new DelayResult(50);

        // Act
        response.setAbortResult(abortResult);
        response.setDelayResult(delayResult);

        // Assert
        assertThat(response.getAbortResult()).isEqualTo(abortResult);
        assertThat(response.getDelayResult()).isEqualTo(delayResult);
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含注入标记和结果
     * 测试场景：构造全参数 FaultResponse 调用 toString
     * 验证内容：toString 包含 abortCode 和 delay 值
     */
    @Test
    public void testToString() {
        // Arrange
        FaultResponse response = new FaultResponse(true, new AbortResult(502), new DelayResult(30));

        // Act
        String str = response.toString();

        // Assert
        assertThat(str).contains("502");
        assertThat(str).contains("30");
    }
}
