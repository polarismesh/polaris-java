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

package com.tencent.polaris.factory.config.global;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link LocationProviderConfigImpl}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class LocationProviderConfigImplTest {

    private static final String TYPE_LOCAL = "local";

    private static final String TYPE_CLOUD = "cloud";

    private LocationProviderConfigImpl config;

    @Before
    public void setUp() {
        config = new LocationProviderConfigImpl();
    }

    /**
     * 测试 getType 返回已设置的类型值
     * 测试目的：验证 setType/getType 能正确读写 type 字段
     * 测试场景：设置 type 后调用 getType
     * 验证内容：返回值与设置值一致
     */
    @Test
    public void testGetType() {
        // Arrange
        config.setType(TYPE_LOCAL);

        // Act
        String result = config.getType();

        // Assert
        assertThat(result).isEqualTo(TYPE_LOCAL);
    }

    /**
     * 测试 getType 未设置时返回 null
     * 测试目的：验证未调用 setType 时 getType 返回 null
     * 测试场景：新建实例不设置 type
     * 验证内容：返回值为 null
     */
    @Test
    public void testGetType_DefaultNull() {
        // Act
        String result = config.getType();

        // Assert
        assertThat(result).isNull();
    }

    /**
     * 测试 getOptions 返回已设置的选项
     * 测试目的：验证 getOptions 能正确返回内部 options map
     * 测试场景：通过反射设置 options 后调用 getOptions
     * 验证内容：返回的 map 包含预期的键值对
     */
    @Test
    public void testGetOptions() throws Exception {
        // Arrange
        Map<String, Object> opts = new HashMap<>();
        opts.put("region", "ap-guangzhou");
        opts.put("zone", "ap-guangzhou-3");
        java.lang.reflect.Field field = LocationProviderConfigImpl.class.getDeclaredField("options");
        field.setAccessible(true);
        field.set(config, opts);

        // Act
        Map<String, Object> result = config.getOptions();

        // Assert
        assertThat(result).containsEntry("region", "ap-guangzhou");
        assertThat(result).containsEntry("zone", "ap-guangzhou-3");
    }

    /**
     * 测试 getOptions 在 options 为 null 时返回空 map
     * 测试目的：验证 options 字段为 null 时 getOptions 自动初始化返回空 map
     * 测试场景：通过反射将 options 置为 null
     * 验证内容：返回非 null 的空 map
     */
    @Test
    public void testGetOptions_NullInitialized() throws Exception {
        // Arrange
        java.lang.reflect.Field field = LocationProviderConfigImpl.class.getDeclaredField("options");
        field.setAccessible(true);
        field.set(config, null);

        // Act
        Map<String, Object> result = config.getOptions();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    /**
     * 测试 verify 在 type 为 null 时抛出 IllegalArgumentException
     * 测试目的：验证 type 未设置时 verify 校验失败并抛出异常
     * 测试场景：不设置 type 直接调用 verify
     * 验证内容：抛出 IllegalArgumentException，消息包含 "location.provider.type"
     */
    @Test
    public void testVerify_TypeNull() {
        // Act & Assert
        assertThatThrownBy(() -> config.verify())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("location.provider.type");
    }

    /**
     * 测试 verify 在 type 有值时不抛出异常
     * 测试目的：验证 type 已设置时 verify 校验通过
     * 测试场景：设置有效 type 后调用 verify
     * 验证内容：不抛出任何异常
     */
    @Test
    public void testVerify_TypeValid() {
        // Arrange
        config.setType(TYPE_CLOUD);

        // Act & Assert
        assertThatCode(() -> config.verify()).doesNotThrowAnyException();
    }

    /**
     * 测试 toString 包含 type 和 options 信息
     * 测试目的：验证 toString 输出中包含 type 字段值
     * 测试场景：设置 type 后调用 toString
     * 验证内容：返回字符串包含 type 值
     */
    @Test
    public void testToString() {
        // Arrange
        config.setType(TYPE_LOCAL);

        // Act
        String result = config.toString();

        // Assert
        assertThat(result).contains(TYPE_LOCAL);
        assertThat(result).contains("options");
    }

    /**
     * 测试 setDefault 不抛出异常
     * 测试目的：验证 setDefault 为空实现，调用不会产生副作用
     * 测试场景：传入任意对象调用 setDefault
     * 验证内容：不抛出任何异常
     */
    @Test
    public void testSetDefault() {
        // Act & Assert（不抛异常即通过）
        config.setDefault(new Object());
        config.setDefault(null);
    }
}
