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

package com.tencent.polaris.configuration.client.internal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link ConfigEffectiveQueryConfig}.
 *
 * @author evelynwei
 */
public class ConfigEffectiveQueryConfigTest {

    /**
     * 测试目的：缺省时 enable 默认为 true（与配置监听 config-watch 一致，默认开启）。
     * 测试场景：新建配置未设置。
     * 验证内容：isEnable 为 true。
     */
    @Test
    public void testDefaultEnabled() {
        ConfigEffectiveQueryConfig config = new ConfigEffectiveQueryConfig();

        assertThat(config.isEnable()).isTrue();
    }

    /**
     * 测试目的：NAME 常量为 config-effective，与 default-config.yml 插件项一致。
     * 测试场景：读取常量。
     * 验证内容：等于 config-effective。
     */
    @Test
    public void testName() {
        assertThat(ConfigEffectiveQueryConfig.NAME).isEqualTo("config-effective");
    }

    /**
     * 测试目的：setEnable 后 isEnable 返回设置值。
     * 测试场景：设置 true/false。
     * 验证内容：isEnable 与设置一致。
     */
    @Test
    public void testSetEnable() {
        ConfigEffectiveQueryConfig config = new ConfigEffectiveQueryConfig();
        config.setEnable(true);
        assertThat(config.isEnable()).isTrue();
        config.setEnable(false);
        assertThat(config.isEnable()).isFalse();
    }

    /**
     * 测试目的：verify 在 enable 为 null 时抛异常并含配置路径；设置后校验通过。
     * 测试场景：未设置与已设置。
     * 验证内容：null 抛 IllegalArgumentException，非 null 不抛。
     */
    @Test
    public void testVerify() {
        ConfigEffectiveQueryConfig config = new ConfigEffectiveQueryConfig();
        assertThatThrownBy(config::verify).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("config-effective.enable");

        config.setEnable(false);
        assertThatCode(config::verify).doesNotThrowAnyException();
    }

    /**
     * 测试目的：setDefault 用默认值填充未设置的 enable，已设置时不覆盖。
     * 测试场景：目标未设置 / 已设置。
     * 验证内容：未设置取默认值，已设置保留原值。
     */
    @Test
    public void testSetDefault() {
        ConfigEffectiveQueryConfig defaults = new ConfigEffectiveQueryConfig();
        defaults.setEnable(true);

        ConfigEffectiveQueryConfig target = new ConfigEffectiveQueryConfig();
        target.setDefault(defaults);
        assertThat(target.isEnable()).isTrue();

        ConfigEffectiveQueryConfig target2 = new ConfigEffectiveQueryConfig();
        target2.setEnable(false);
        target2.setDefault(defaults);
        assertThat(target2.isEnable()).isFalse();
    }

    /**
     * 测试目的：setDefault 传 null 安全无操作。
     * 测试场景：defaultObject 为 null。
     * 验证内容：不抛异常，enable 仍缺省 true。
     */
    @Test
    public void testSetDefaultNull() {
        ConfigEffectiveQueryConfig config = new ConfigEffectiveQueryConfig();
        config.setDefault(null);
        assertThat(config.isEnable()).isTrue();
    }
}
