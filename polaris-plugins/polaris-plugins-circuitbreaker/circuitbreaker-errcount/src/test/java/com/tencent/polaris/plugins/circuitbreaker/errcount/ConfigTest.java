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

package com.tencent.polaris.plugins.circuitbreaker.errcount;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link Config}.
 *
 * @author Haotian Zhang
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigTest {

    /**
     * 测试 setter/getter
     * 测试目的：验证 continuousErrorThreshold 读写
     * 测试场景：设置 threshold 为 5 后读取
     * 验证内容：返回 5
     */
    @Test
    public void testSetterAndGetter() {
        // Arrange
        Config config = new Config();

        // Act
        config.setContinuousErrorThreshold(5);

        // Assert
        assertThat(config.getContinuousErrorThreshold()).isEqualTo(5);
    }

    /**
     * 测试 verify 通过
     * 测试目的：验证 continuousErrorThreshold 为正数时 verify 不抛异常
     * 测试场景：threshold 设置为 10
     * 验证内容：verify 正常返回
     */
    @Test
    public void testVerifyPass() {
        // Arrange
        Config config = new Config();
        config.setContinuousErrorThreshold(10);

        // Act & Assert
        assertThatCode(config::verify).doesNotThrowAnyException();
    }

    /**
     * 测试 verify 在 threshold 为 null 时抛异常
     * 测试目的：验证 threshold 为 null 时抛 IllegalArgumentException
     * 测试场景：不设置 threshold
     * 验证内容：抛出异常且消息包含 continuousErrorThreshold
     */
    @Test
    public void testVerifyNullThreshold() {
        // Arrange
        Config config = new Config();

        // Act & Assert
        assertThatThrownBy(config::verify)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("continuousErrorThreshold");
    }

    /**
     * 测试 verify 在 threshold 为 0 时抛异常
     * 测试目的：验证 threshold <= 0 时抛异常
     * 测试场景：threshold 设置为 0
     * 验证内容：抛出 IllegalArgumentException
     */
    @Test
    public void testVerifyZeroThreshold() {
        // Arrange
        Config config = new Config();
        config.setContinuousErrorThreshold(0);

        // Act & Assert
        assertThatThrownBy(config::verify)
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 测试 setDefault 从默认对象补全字段
     * 测试目的：验证 setDefault 在字段为 null 时从默认对象继承
     * 测试场景：默认对象 threshold=8，当前对象不设置
     * 验证内容：setDefault 后 threshold 为 8
     */
    @Test
    public void testSetDefaultInherits() {
        // Arrange
        Config config = new Config();
        Config defaultConfig = new Config();
        defaultConfig.setContinuousErrorThreshold(8);

        // Act
        config.setDefault(defaultConfig);

        // Assert
        assertThat(config.getContinuousErrorThreshold()).isEqualTo(8);
    }

    /**
     * 测试 setDefault 不覆盖已设值
     * 测试目的：验证 setDefault 不覆盖非 null 字段
     * 测试场景：当前 threshold=3，默认对象 threshold=8
     * 验证内容：setDefault 后 threshold 仍为 3
     */
    @Test
    public void testSetDefaultNotOverride() {
        // Arrange
        Config config = new Config();
        config.setContinuousErrorThreshold(3);
        Config defaultConfig = new Config();
        defaultConfig.setContinuousErrorThreshold(8);

        // Act
        config.setDefault(defaultConfig);

        // Assert
        assertThat(config.getContinuousErrorThreshold()).isEqualTo(3);
    }

    /**
     * 测试 setDefault 传入 null 不抛异常
     * 测试目的：验证 setDefault 对 null 入参的安全处理
     * 测试场景：setDefault(null)
     * 验证内容：不抛异常
     */
    @Test
    public void testSetDefaultNull() {
        // Arrange
        Config config = new Config();

        // Act & Assert
        assertThatCode(() -> config.setDefault(null)).doesNotThrowAnyException();
    }

    /**
     * 测试 toString 包含 threshold
     * 测试目的：验证 toString 输出包含 continuousErrorThreshold
     * 测试场景：设置 threshold 后调用 toString
     * 验证内容：toString 包含 threshold 值
     */
    @Test
    public void testToString() {
        // Arrange
        Config config = new Config();
        config.setContinuousErrorThreshold(7);

        // Act
        String str = config.toString();

        // Assert
        assertThat(str).contains("7");
    }
}
