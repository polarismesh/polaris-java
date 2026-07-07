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

import java.util.concurrent.TimeUnit;

import com.tencent.polaris.api.pojo.ServiceKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link InvokeContext}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class InvokeContextTest {

    /**
     * 测试 RequestContext 构造与基本 getter
     * 测试目的：验证 RequestContext 正确保存 service/protocol/method/path
     * 测试场景：构造 RequestContext 并读取各字段
     * 验证内容：各字段与入参一致，sourceService 默认为 null
     */
    @Test
    public void testRequestContextConstructorAndGetters() {
        // Arrange
        ServiceKey service = new ServiceKey("Test", "svc");

        // Act
        InvokeContext.RequestContext context = new InvokeContext.RequestContext(service, "http", "GET", "/api/v1");

        // Assert
        assertThat(context.getService()).isEqualTo(service);
        assertThat(context.getProtocol()).isEqualTo("http");
        assertThat(context.getMethod()).isEqualTo("GET");
        assertThat(context.getPath()).isEqualTo("/api/v1");
        assertThat(context.getSourceService()).isNull();
    }

    /**
     * 测试 RequestContext 的 sourceService 和 resultToErrorCode setter
     * 测试目的：验证 sourceService 和 resultToErrorCode 的读写
     * 测试场景：构造后设置 sourceService 和 resultToErrorCode
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testRequestContextSetters() {
        // Arrange
        InvokeContext.RequestContext context = new InvokeContext.RequestContext(
                new ServiceKey("Test", "svc"), "http", "GET", "/api");
        ServiceKey sourceService = new ServiceKey("Prod", "caller");
        ResultToErrorCode errorCode = new ResultToErrorCode() {
            @Override
            public int onSuccess(Object value) {
                return 200;
            }

            @Override
            public int onError(Throwable throwable) {
                return 500;
            }
        };

        // Act
        context.setSourceService(sourceService);
        context.setResultToErrorCode(errorCode);

        // Assert
        assertThat(context.getSourceService()).isEqualTo(sourceService);
        assertThat(context.getResultToErrorCode()).isEqualTo(errorCode);
    }

    /**
     * 测试 ResponseContext 的 duration 及单位
     * 测试目的：验证 ResponseContext 的 duration/durationUnit 读写
     * 测试场景：构造 ResponseContext 设置 duration 和单位
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testResponseContextDuration() {
        // Arrange
        InvokeContext.ResponseContext context = new InvokeContext.ResponseContext();

        // Act
        context.setDuration(500L);
        context.setDurationUnit(TimeUnit.MILLISECONDS);

        // Assert
        assertThat(context.getDuration()).isEqualTo(500L);
        assertThat(context.getDurationUnit()).isEqualTo(TimeUnit.MILLISECONDS);
    }

    /**
     * 测试 ResponseContext 的 result 和 error
     * 测试目的：验证 ResponseContext 的 result/error 读写
     * 测试场景：设置 result 和 error 后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testResponseContextResultAndError() {
        // Arrange
        InvokeContext.ResponseContext context = new InvokeContext.ResponseContext();
        Object result = "ok";
        Throwable error = new RuntimeException("boom");

        // Act
        context.setResult(result);
        context.setError(error);

        // Assert
        assertThat(context.getResult()).isEqualTo(result);
        assertThat(context.getError()).isEqualTo(error);
    }

    /**
     * 测试 ResultToErrorCode 接口的 lambda 实现
     * 测试目的：验证 ResultToErrorCode 接口可被 lambda 实现并正确调用
     * 测试场景：用 lambda 构造成功返回 200、异常返回 500 的实现
     * 验证内容：onSuccess 返回 200，onError 返回 500
     */
    @Test
    public void testResultToErrorCodeLambda() {
        // Arrange
        ResultToErrorCode errorCode = new ResultToErrorCode() {
            @Override
            public int onSuccess(Object value) {
                return 200;
            }

            @Override
            public int onError(Throwable throwable) {
                return 500;
            }
        };

        // Act & Assert
        assertThat(errorCode.onSuccess("any")).isEqualTo(200);
        assertThat(errorCode.onError(new RuntimeException("fail"))).isEqualTo(500);
    }
}
