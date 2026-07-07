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

package com.tencent.polaris.circuitbreak.api.pojo;

import java.util.HashMap;
import java.util.Map;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus.FallbackInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link CheckResult}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckResultTest {

    /**
     * 测试通过的 CheckResult
     * 测试目的：验证 pass=true 时 isPass 返回 true，fallbackInfo 为 null
     * 测试场景：构造 pass=true、ruleName 非空、fallbackInfo 为 null 的 CheckResult
     * 验证内容：isPass 为 true，ruleName 与入参一致，fallbackInfo 为 null
     */
    @Test
    public void testPassedCheckResult() {
        // Act
        CheckResult result = new CheckResult(true, "rule-1", null);

        // Assert
        assertThat(result.isPass()).isTrue();
        assertThat(result.getRuleName()).isEqualTo("rule-1");
        assertThat(result.getFallbackInfo()).isNull();
    }

    /**
     * 测试被熔断的 CheckResult
     * 测试目的：验证 pass=false 时携带 FallbackInfo
     * 测试场景：构造 pass=false、带 FallbackInfo 的 CheckResult
     * 验证内容：isPass 为 false，fallbackInfo 与入参一致
     */
    @Test
    public void testBlockedCheckResultWithFallback() {
        // Arrange
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Reason", "circuit-open");
        FallbackInfo fallbackInfo = new FallbackInfo(503, headers, "unavailable");

        // Act
        CheckResult result = new CheckResult(false, "rule-2", fallbackInfo);

        // Assert
        assertThat(result.isPass()).isFalse();
        assertThat(result.getRuleName()).isEqualTo("rule-2");
        assertThat(result.getFallbackInfo()).isNotNull();
        assertThat(result.getFallbackInfo().getCode()).isEqualTo(503);
        assertThat(result.getFallbackInfo().getBody()).isEqualTo("unavailable");
    }

    /**
     * 测试 toString 包含关键字段
     * 测试目的：验证 toString 输出包含 pass 和 ruleName
     * 测试场景：构造 CheckResult 调用 toString
     * 验证内容：toString 包含 ruleName
     */
    @Test
    public void testToString() {
        // Arrange
        CheckResult result = new CheckResult(true, "rule-3", null);

        // Act
        String str = result.toString();

        // Assert
        assertThat(str).contains("rule-3");
        assertThat(str).contains("true");
    }
}
