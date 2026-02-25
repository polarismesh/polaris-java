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

package com.tencent.polaris.fault.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.ConsumerConfig;
import com.tencent.polaris.api.config.consumer.FaultConfig;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.fault.api.rpc.FaultRequest;
import com.tencent.polaris.fault.api.rpc.FaultResponse;
import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.specification.api.v1.traffic.manage.FaultInjectionProto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultFaultFlow}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultFaultFlowTest {

    @Mock
    private SDKContext sdkContext;

    @Mock
    private Configuration configuration;

    @Mock
    private ConsumerConfig consumerConfig;

    @Mock
    private FaultConfig faultConfig;

    private DefaultFaultFlow defaultFaultFlow;

    @Before
    public void setUp() {
        // 构建 mock 链：sdkContext -> configuration -> consumerConfig -> faultConfig
        when(sdkContext.getConfig()).thenReturn(configuration);
        when(configuration.getConsumer()).thenReturn(consumerConfig);
        when(consumerConfig.getFault()).thenReturn(faultConfig);

        defaultFaultFlow = new DefaultFaultFlow();
        defaultFaultFlow.setSDKContext(sdkContext);
    }

    /**
     * 测试目的：验证getName返回默认flow名称
     * 测试场景：调用getName方法
     * 验证内容：返回值与FlowConfig.DEFAULT_FLOW_NAME一致
     */
    @Test
    public void testGetName() {
        // Act & Assert：执行被测试的方法并验证结果
        assertThat(defaultFaultFlow.getName()).isEqualTo(FlowConfig.DEFAULT_FLOW_NAME);
    }

    /**
     * 测试目的：验证故障注入功能禁用时返回未注入故障的响应
     * 测试场景：faultConfig.isEnable()返回false
     * 验证内容：FaultResponse.isFaultInjected为false
     */
    @Test
    public void testFault_WithFaultDisabled() {
        // Arrange：准备测试数据和环境
        when(faultConfig.isEnable()).thenReturn(false);
        MetadataContext metadataContext = new MetadataContext();
        FaultRequest faultRequest = new FaultRequest("source-namespace", "source-service",
                "target-namespace", "target-service", metadataContext);

        // Act：执行被测试的方法
        FaultResponse response = defaultFaultFlow.fault(faultRequest);

        // Assert：验证测试结果
        assertThat(response).isNotNull();
        assertThat(response.isFaultInjected()).isFalse();
    }

    /**
     * 测试目的：验证faultConfig为null时返回未注入故障的响应
     * 测试场景：faultConfig为null
     * 验证内容：FaultResponse.isFaultInjected为false
     */
    @Test
    public void testFault_WithNullFaultConfig() throws NoSuchFieldException, IllegalAccessException {
        // Arrange：准备测试数据和环境 - 通过反射将faultConfig设为null
        when(consumerConfig.getFault()).thenReturn(null);
        DefaultFaultFlow flowWithNullConfig = new DefaultFaultFlow();
        flowWithNullConfig.setSDKContext(sdkContext);

        MetadataContext metadataContext = new MetadataContext();
        FaultRequest faultRequest = new FaultRequest("source-namespace", "source-service",
                "target-namespace", "target-service", metadataContext);

        // Act：执行被测试的方法
        FaultResponse response = flowWithNullConfig.fault(faultRequest);

        // Assert：验证测试结果
        assertThat(response).isNotNull();
        assertThat(response.isFaultInjected()).isFalse();
    }

    /**
     * 测试目的：验证故障注入功能启用但无规则时返回未注入故障的响应
     * 测试场景：启用故障注入，getFaultInjectionRules返回空列表
     * 验证内容：FaultResponse.isFaultInjected为false
     */
    @Test
    public void testFault_WithEnabledButEmptyRules() {
        // Arrange：准备测试数据和环境
        when(faultConfig.isEnable()).thenReturn(true);
        MetadataContext metadataContext = new MetadataContext();
        // 使用空的service信息触发空规则列表返回
        FaultRequest faultRequest = new FaultRequest("source-namespace", "source-service",
                "", "", metadataContext);

        // Act：执行被测试的方法
        FaultResponse response = defaultFaultFlow.fault(faultRequest);

        // Assert：验证测试结果
        assertThat(response).isNotNull();
        assertThat(response.isFaultInjected()).isFalse();
    }

    /**
     * 测试目的：验证getFaultInjectionRules方法在service为空时返回空列表
     * 测试场景：传入空的ServiceKey
     * 验证内容：返回空列表
     */
    @Test
    public void testGetFaultInjectionRules_WithBlankService() {
        // Arrange：准备测试数据和环境
        ServiceKey serviceKey = new ServiceKey("namespace", "");

        // Act：执行被测试的方法
        List<FaultInjectionProto.FaultInjection> rules = defaultFaultFlow.getFaultInjectionRules(serviceKey);

        // Assert：验证测试结果
        assertThat(rules).isEmpty();
    }

    /**
     * 测试目的：验证getFaultInjectionRules方法在namespace为空时返回空列表
     * 测试场景：传入空namespace的ServiceKey
     * 验证内容：返回空列表
     */
    @Test
    public void testGetFaultInjectionRules_WithBlankNamespace() {
        // Arrange：准备测试数据和环境
        ServiceKey serviceKey = new ServiceKey("", "service");

        // Act：执行被测试的方法
        List<FaultInjectionProto.FaultInjection> rules = defaultFaultFlow.getFaultInjectionRules(serviceKey);

        // Assert：验证测试结果
        assertThat(rules).isEmpty();
    }

}
