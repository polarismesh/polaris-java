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

package com.tencent.polaris.fault.client.api;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.fault.api.flow.FaultFlow;
import com.tencent.polaris.fault.api.rpc.AbortResult;
import com.tencent.polaris.fault.api.rpc.DelayResult;
import com.tencent.polaris.fault.api.rpc.FaultRequest;
import com.tencent.polaris.fault.api.rpc.FaultResponse;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultFaultAPI}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultFaultAPITest {

    @Mock
    private SDKContext sdkContext;

    @Mock
    private FaultFlow faultFlow;

    private DefaultFaultAPI defaultFaultAPI;

    @Before
    public void setUp() {
        when(sdkContext.getOrInitFlow(FaultFlow.class)).thenReturn(faultFlow);
        defaultFaultAPI = new DefaultFaultAPI(sdkContext);
        defaultFaultAPI.subInit();
    }

    /**
     * 测试目的：验证正常调用fault方法返回成功结果
     * 测试场景：传入合法的FaultRequest，FaultFlow返回未注入故障的响应
     * 验证内容：返回的FaultResponse与mock的响应一致
     */
    @Test
    public void testFault_Success() {
        // Arrange：准备测试数据和环境
        MetadataContext metadataContext = new MetadataContext();
        FaultRequest faultRequest = new FaultRequest("source-namespace", "source-service",
                "target-namespace", "target-service", metadataContext);
        FaultResponse expectedResponse = new FaultResponse(false);
        when(faultFlow.fault(faultRequest)).thenReturn(expectedResponse);

        // Act：执行被测试的方法
        FaultResponse actualResponse = defaultFaultAPI.fault(faultRequest);

        // Assert：验证测试结果
        assertThat(actualResponse).isEqualTo(expectedResponse);
        assertThat(actualResponse.isFaultInjected()).isFalse();
    }

    /**
     * 测试目的：验证fault方法返回包含中断故障注入的响应
     * 测试场景：FaultFlow返回包含AbortResult的响应
     * 验证内容：返回的FaultResponse包含正确的AbortResult
     */
    @Test
    public void testFault_WithAbortResult() {
        // Arrange：准备测试数据和环境
        MetadataContext metadataContext = new MetadataContext();
        FaultRequest faultRequest = new FaultRequest("source-namespace", "source-service",
                "target-namespace", "target-service", metadataContext);
        AbortResult abortResult = new AbortResult(500);
        FaultResponse expectedResponse = new FaultResponse(true, abortResult);
        when(faultFlow.fault(faultRequest)).thenReturn(expectedResponse);

        // Act：执行被测试的方法
        FaultResponse actualResponse = defaultFaultAPI.fault(faultRequest);

        // Assert：验证测试结果
        assertThat(actualResponse).isEqualTo(expectedResponse);
        assertThat(actualResponse.isFaultInjected()).isTrue();
        assertThat(actualResponse.getAbortResult()).isNotNull();
        assertThat(actualResponse.getAbortResult().getAbortCode()).isEqualTo(500);
    }

    /**
     * 测试目的：验证fault方法返回包含延迟故障注入的响应
     * 测试场景：FaultFlow返回包含DelayResult的响应
     * 验证内容：返回的FaultResponse包含正确的DelayResult
     */
    @Test
    public void testFault_WithDelayResult() {
        // Arrange：准备测试数据和环境
        MetadataContext metadataContext = new MetadataContext();
        FaultRequest faultRequest = new FaultRequest("source-namespace", "source-service",
                "target-namespace", "target-service", metadataContext);
        DelayResult delayResult = new DelayResult(3000);
        FaultResponse expectedResponse = new FaultResponse(true, delayResult);
        when(faultFlow.fault(faultRequest)).thenReturn(expectedResponse);

        // Act：执行被测试的方法
        FaultResponse actualResponse = defaultFaultAPI.fault(faultRequest);

        // Assert：验证测试结果
        assertThat(actualResponse).isEqualTo(expectedResponse);
        assertThat(actualResponse.isFaultInjected()).isTrue();
        assertThat(actualResponse.getDelayResult()).isNotNull();
        assertThat(actualResponse.getDelayResult().getDelay()).isEqualTo(3000);
    }

    /**
     * 测试目的：验证fault方法传入null请求时抛出异常
     * 测试场景：传入null的FaultRequest
     * 验证内容：抛出PolarisException
     */
    @Test
    public void testFault_WithNullRequest() {
        // Act & Assert：执行被测试的方法并验证抛出异常
        assertThatThrownBy(() -> defaultFaultAPI.fault(null))
                .isInstanceOf(PolarisException.class)
                .hasMessageContaining("FaultRequest can not be null");
    }
}
