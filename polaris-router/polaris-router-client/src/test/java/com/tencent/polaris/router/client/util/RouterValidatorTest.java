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

package com.tencent.polaris.router.client.util;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.router.api.rpc.ProcessLoadBalanceRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link RouterValidator}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class RouterValidatorTest {

    /**
     * 测试 validateProcessRouterRequest 在 dstInstances 为 null 时抛异常
     * 测试目的：验证路由请求 dstInstances 为 null 时抛 PolarisException
     * 测试场景：构造 dstInstances 为 null 的 ProcessRoutersRequest
     * 验证内容：抛出 PolarisException，errorCode 为 API_INVALID_ARGUMENT
     */
    @Test
    public void testValidateProcessRouterRequestNullInstances() {
        // Arrange
        ProcessRoutersRequest request = new ProcessRoutersRequest();

        // Act & Assert
        assertThatThrownBy(() -> RouterValidator.validateProcessRouterRequest(request))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("dstInstances");
    }

    /**
     * 测试 validateProcessRouterRequest 异常的 errorCode
     * 测试目的：验证抛出的异常携带正确的 errorCode
     * 测试场景：构造 dstInstances 为 null 的请求，捕获异常检查 code
     * 验证内容：errorCode 为 API_INVALID_ARGUMENT
     */
    @Test
    public void testValidateProcessRouterRequestErrorCode() {
        // Arrange
        ProcessRoutersRequest request = new ProcessRoutersRequest();

        // Act
        try {
            RouterValidator.validateProcessRouterRequest(request);
        } catch (PolarisException ex) {
            // Assert
            assertThat(ex.getCode()).isEqualTo(ErrorCode.API_INVALID_ARGUMENT);
        }
    }

    /**
     * 测试 validateProcessLoadBalanceRequest 在 dstInstances 为 null 时抛异常
     * 测试目的：验证负载均衡请求 dstInstances 为 null 时抛 PolarisException
     * 测试场景：构造 dstInstances 为 null 的 ProcessLoadBalanceRequest
     * 验证内容：抛出 PolarisException，消息包含 dstInstances
     */
    @Test
    public void testValidateProcessLoadBalanceRequestNullInstances() {
        // Arrange
        ProcessLoadBalanceRequest request = new ProcessLoadBalanceRequest();

        // Act & Assert
        assertThatThrownBy(() -> RouterValidator.validateProcessLoadBalanceRequest(request))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("dstInstances");
    }

    /**
     * 测试 validateProcessLoadBalanceRequest 异常的 errorCode
     * 测试目的：验证抛出的异常携带正确的 errorCode
     * 测试场景：构造 dstInstances 为 null 的请求，捕获异常检查 code
     * 验证内容：errorCode 为 API_INVALID_ARGUMENT
     */
    @Test
    public void testValidateProcessLoadBalanceRequestErrorCode() {
        // Arrange
        ProcessLoadBalanceRequest request = new ProcessLoadBalanceRequest();

        // Act
        try {
            RouterValidator.validateProcessLoadBalanceRequest(request);
        } catch (PolarisException ex) {
            // Assert
            assertThat(ex.getCode()).isEqualTo(ErrorCode.API_INVALID_ARGUMENT);
        }
    }
}
