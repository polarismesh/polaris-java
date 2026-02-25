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

package com.tencent.polaris.fault.client.utils;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.fault.api.rpc.FaultRequest;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link FaultValidator}.
 *
 * @author Haotian Zhang
 */
public class FaultValidatorTest {

    /**
     * 测试目的：验证传入合法的FaultRequest不抛出异常
     * 测试场景：sourceService和targetService均有效
     * 验证内容：方法正常执行，不抛出异常
     */
    @Test
    public void testValidateFaultRequest_WithValidRequest() {
        // Arrange：准备测试数据和环境
        MetadataContext metadataContext = new MetadataContext();
        FaultRequest faultRequest = new FaultRequest("source-namespace", "source-service",
                "target-namespace", "target-service", metadataContext);

        // Act & Assert：执行被测试的方法并验证不抛出异常
        assertThatCode(() -> FaultValidator.validateFaultRequest(faultRequest))
                .doesNotThrowAnyException();
    }

    /**
     * 测试目的：验证传入null的FaultRequest会抛出异常
     * 测试场景：faultRequest为null
     * 验证内容：抛出PolarisException，包含"FaultRequest can not be null"
     */
    @Test
    public void testValidateFaultRequest_WithNullRequest() {
        // Act & Assert：执行被测试的方法并验证抛出异常
        assertThatThrownBy(() -> FaultValidator.validateFaultRequest(null))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("FaultRequest can not be null");
    }

    /**
     * 测试目的：验证传入metadataContext为null的请求也能正常校验
     * 测试场景：metadataContext为null
     * 验证内容：方法正常执行，不抛出异常（校验不涉及metadataContext）
     */
    @Test
    public void testValidateFaultRequest_WithNullMetadataContext() {
        // Arrange：准备测试数据和环境
        FaultRequest faultRequest = new FaultRequest("source-namespace", "source-service",
                "target-namespace", "target-service", null);

        // Act & Assert：执行被测试的方法并验证不抛出异常
        assertThatCode(() -> FaultValidator.validateFaultRequest(faultRequest))
                .doesNotThrowAnyException();
    }
}
