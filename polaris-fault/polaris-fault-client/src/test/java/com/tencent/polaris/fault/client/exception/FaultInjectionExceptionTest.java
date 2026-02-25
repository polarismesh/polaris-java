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

package com.tencent.polaris.fault.client.exception;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.FallbackInfo;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link FaultInjectionException}.
 *
 * @author Haotian Zhang
 */
public class FaultInjectionExceptionTest {

    /**
     * 测试目的：验证FaultInjectionException的构造函数和属性获取
     * 测试场景：使用包含完整信息的FallbackInfo构造异常
     * 验证内容：errorCode和fallbackInfo属性正确
     */
    @Test
    public void testConstructor_WithFullFallbackInfo() {
        // Arrange：准备测试数据和环境
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        FallbackInfo fallbackInfo = new FallbackInfo(500, headers, "Internal Server Error");

        // Act：执行被测试的方法
        FaultInjectionException exception = new FaultInjectionException(fallbackInfo);

        // Assert：验证测试结果
        assertThat(exception.getCode()).isEqualTo(ErrorCode.CLIENT_FAULT_INJECTED);
        assertThat(exception.getFallbackInfo()).isEqualTo(fallbackInfo);
        assertThat(exception.getFallbackInfo().getCode()).isEqualTo(500);
        assertThat(exception.getFallbackInfo().getHeaders()).containsEntry("Content-Type", "application/json");
        assertThat(exception.getFallbackInfo().getBody()).isEqualTo("Internal Server Error");
        assertThat(exception.getMessage()).contains("fallbackInfo");
    }

    /**
     * 测试目的：验证FaultInjectionException使用空headers的FallbackInfo
     * 测试场景：使用空headers的FallbackInfo构造异常
     * 验证内容：errorCode和fallbackInfo属性正确
     */
    @Test
    public void testConstructor_WithEmptyHeaders() {
        // Arrange：准备测试数据和环境
        FallbackInfo fallbackInfo = new FallbackInfo(404, Collections.emptyMap(), "Not Found");

        // Act：执行被测试的方法
        FaultInjectionException exception = new FaultInjectionException(fallbackInfo);

        // Assert：验证测试结果
        assertThat(exception.getCode()).isEqualTo(ErrorCode.CLIENT_FAULT_INJECTED);
        assertThat(exception.getFallbackInfo()).isEqualTo(fallbackInfo);
        assertThat(exception.getFallbackInfo().getCode()).isEqualTo(404);
        assertThat(exception.getFallbackInfo().getHeaders()).isEmpty();
        assertThat(exception.getFallbackInfo().getBody()).isEqualTo("Not Found");
    }

    /**
     * 测试目的：验证FaultInjectionException使用null body的FallbackInfo
     * 测试场景：body为null
     * 验证内容：errorCode正确，body为null
     */
    @Test
    public void testConstructor_WithNullBody() {
        // Arrange：准备测试数据和环境
        FallbackInfo fallbackInfo = new FallbackInfo(503, null, null);

        // Act：执行被测试的方法
        FaultInjectionException exception = new FaultInjectionException(fallbackInfo);

        // Assert：验证测试结果
        assertThat(exception.getCode()).isEqualTo(ErrorCode.CLIENT_FAULT_INJECTED);
        assertThat(exception.getFallbackInfo()).isEqualTo(fallbackInfo);
        assertThat(exception.getFallbackInfo().getCode()).isEqualTo(503);
        assertThat(exception.getFallbackInfo().getBody()).isNull();
    }

    /**
     * 测试目的：验证FaultInjectionException是PolarisException的子类
     * 测试场景：验证继承关系
     * 验证内容：异常是PolarisException的实例
     */
    @Test
    public void testInheritance() {
        // Arrange：准备测试数据和环境
        FallbackInfo fallbackInfo = new FallbackInfo(500, null, "error");

        // Act：执行被测试的方法
        FaultInjectionException exception = new FaultInjectionException(fallbackInfo);

        // Assert：验证测试结果
        assertThat(exception).isInstanceOf(PolarisException.class);
    }
}
