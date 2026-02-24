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

package com.tencent.polaris.auth.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.auth.api.core.AuthAPI;
import com.tencent.polaris.auth.client.api.DefaultAuthAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AuthAPIFactory}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthAPIFactoryTest {

    @Test
    public void testCreateAuthAPIByContext_WithValidContext() {
        // 准备：模拟有效的 SDK 上下文
        SDKContext mockContext = mock(SDKContext.class);
        ValueContext mockValueContext = mock(ValueContext.class);
        when(mockContext.getOrInitFlow(any())).thenReturn(null);
        when(mockContext.getValueContext()).thenReturn(mockValueContext);

        // 执行
        AuthAPI authAPI = AuthAPIFactory.createAuthAPIByContext(mockContext);

        // 验证：返回非 null 的 DefaultAuthAPI 实例
        assertThat(authAPI).isNotNull();
        assertThat(authAPI).isInstanceOf(DefaultAuthAPI.class);
    }

    @Test
    public void testCreateAuthAPIByConfig_WithValidConfig() {
        // 准备：模拟配置和 SDK 上下文
        try (MockedStatic<SDKContext> sdkContextMock = mockStatic(SDKContext.class)) {
            Configuration mockConfig = mock(Configuration.class);
            SDKContext mockContext = mock(SDKContext.class);
            ValueContext mockValueContext = mock(ValueContext.class);

            sdkContextMock.when(() -> SDKContext.initContextByConfig(mockConfig)).thenReturn(mockContext);
            when(mockContext.getOrInitFlow(any())).thenReturn(null);
            when(mockContext.getValueContext()).thenReturn(mockValueContext);

            // 执行
            AuthAPI authAPI = AuthAPIFactory.createAuthAPIByConfig(mockConfig);

            // 验证：返回非 null 的 DefaultAuthAPI 实例
            assertThat(authAPI).isNotNull();
            assertThat(authAPI).isInstanceOf(DefaultAuthAPI.class);
        }
    }

    @Test
    public void testCreateAuthAPI_WithDefaultConfig() {
        // 准备：模拟默认配置创建流程
        try (MockedStatic<ConfigAPIFactory> configApiFactoryMock = mockStatic(ConfigAPIFactory.class);
             MockedStatic<SDKContext> sdkContextMock = mockStatic(SDKContext.class)) {

            Configuration mockConfig = mock(Configuration.class);
            SDKContext mockContext = mock(SDKContext.class);
            ValueContext mockValueContext = mock(ValueContext.class);

            configApiFactoryMock.when(ConfigAPIFactory::defaultConfig).thenReturn(mockConfig);
            sdkContextMock.when(() -> SDKContext.initContextByConfig(mockConfig)).thenReturn(mockContext);
            when(mockContext.getOrInitFlow(any())).thenReturn(null);
            when(mockContext.getValueContext()).thenReturn(mockValueContext);

            // 执行
            AuthAPI authAPI = AuthAPIFactory.createAuthAPI();

            // 验证：返回非 null 对象
            assertThat(authAPI).isNotNull();
            assertThat(authAPI).isInstanceOf(DefaultAuthAPI.class);
        }
    }

    @Test
    public void testCreateAuthAPIByContext_ReturnedInstanceIsNotNull() {
        // 准备：使用已初始化的上下文
        SDKContext mockContext = mock(SDKContext.class);
        ValueContext mockValueContext = mock(ValueContext.class);
        when(mockContext.getOrInitFlow(any())).thenReturn(null);
        when(mockContext.getValueContext()).thenReturn(mockValueContext);

        // 执行
        AuthAPI authAPI = AuthAPIFactory.createAuthAPIByContext(mockContext);

        // 验证：上下文被正确设置，返回的 AuthAPI 非 null
        assertThat(authAPI).isNotNull();
    }
}