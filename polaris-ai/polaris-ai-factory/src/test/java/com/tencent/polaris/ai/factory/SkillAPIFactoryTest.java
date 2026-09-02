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

package com.tencent.polaris.ai.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.ai.api.core.SkillAPI;
import com.tencent.polaris.ai.client.api.DefaultSkillAPI;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Test for {@link SkillAPIFactory}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class SkillAPIFactoryTest {

    /**
     * 测试目的：通过 context 创建 SkillAPI
     * 测试场景：mock SDKContext
     * 验证内容：返回 DefaultSkillAPI
     */
    @Test
    public void testCreateSkillAPIByContext() {
        // Arrange
        SDKContext mockContext = mock(SDKContext.class);
        ValueContext mockValueContext = mock(ValueContext.class);
        when(mockContext.getOrInitFlow(any())).thenReturn(null);
        when(mockContext.getValueContext()).thenReturn(mockValueContext);

        // Act
        SkillAPI skillAPI = SkillAPIFactory.createSkillAPIByContext(mockContext);

        // Assert
        assertThat(skillAPI).isInstanceOf(DefaultSkillAPI.class);
    }

    /**
     * 测试目的：通过 config 创建 SkillAPI
     * 测试场景：mock SDKContext.initContextByConfig
     * 验证内容：返回非空
     */
    @Test
    public void testCreateSkillAPIByConfig() {
        try (MockedStatic<SDKContext> sdkContextMock = mockStatic(SDKContext.class)) {
            // Arrange
            Configuration mockConfig = mock(Configuration.class);
            SDKContext mockContext = mock(SDKContext.class);
            ValueContext mockValueContext = mock(ValueContext.class);
            sdkContextMock.when(() -> SDKContext.initContextByConfig(mockConfig)).thenReturn(mockContext);
            when(mockContext.getOrInitFlow(any())).thenReturn(null);
            when(mockContext.getValueContext()).thenReturn(mockValueContext);

            // Act
            SkillAPI skillAPI = SkillAPIFactory.createSkillAPIByConfig(mockConfig);

            // Assert
            assertThat(skillAPI).isNotNull();
        }
    }

    /**
     * 测试目的：默认配置创建
     * 测试场景：mock ConfigAPIFactory 与 SDKContext
     * 验证内容：返回 DefaultSkillAPI
     */
    @Test
    public void testCreateSkillAPI() {
        try (MockedStatic<ConfigAPIFactory> configApiFactoryMock = mockStatic(ConfigAPIFactory.class);
                MockedStatic<SDKContext> sdkContextMock = mockStatic(SDKContext.class)) {
            // Arrange
            Configuration mockConfig = mock(Configuration.class);
            SDKContext mockContext = mock(SDKContext.class);
            ValueContext mockValueContext = mock(ValueContext.class);
            configApiFactoryMock.when(ConfigAPIFactory::defaultConfig).thenReturn(mockConfig);
            sdkContextMock.when(() -> SDKContext.initContextByConfig(mockConfig)).thenReturn(mockContext);
            when(mockContext.getOrInitFlow(any())).thenReturn(null);
            when(mockContext.getValueContext()).thenReturn(mockValueContext);

            // Act
            SkillAPI skillAPI = SkillAPIFactory.createSkillAPI();

            // Assert
            assertThat(skillAPI).isInstanceOf(DefaultSkillAPI.class);
        }
    }
}
