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

package com.tencent.polaris.plugins.circuitbreaker.errrate;

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
     * 测试 setErrorRateThreshold 计算 errRate
     * 测试目的：验证 setErrorRateThreshold 将百分比转为 0-1 的小数
     * 测试场景：设置 errorRateThreshold=50
     * 验证内容：getErrRate 返回 0.5
     */
    @Test
    public void testSetErrorRateThresholdCalcErrRate() {
        // Arrange
        Config config = new Config();

        // Act
        config.setErrorRateThreshold(50);

        // Assert
        assertThat(config.getErrorRateThreshold()).isEqualTo(50);
        assertThat(config.getErrRate()).isEqualTo(0.5);
    }

    /**
     * 测试 setErrorRateThreshold 超过 100 截断为 1.0
     * 测试目的：验证 threshold>100 时 errRate 截断为 1.0
     * 测试场景：设置 errorRateThreshold=150
     * 验证内容：getErrRate 返回 1.0
     */
    @Test
    public void testSetErrorRateThresholdOver100() {
        // Arrange
        Config config = new Config();

        // Act
        config.setErrorRateThreshold(150);

        // Assert
        assertThat(config.getErrRate()).isEqualTo(1.0);
    }

    /**
     * 测试 setErrorRateThreshold 为 100 时 errRate 为 1.0
     * 测试目的：验证 threshold=100 时 errRate 精确为 1.0
     * 测试场景：设置 errorRateThreshold=100
     * 验证内容：getErrRate 返回 1.0
     */
    @Test
    public void testSetErrorRateThreshold100() {
        // Arrange
        Config config = new Config();

        // Act
        config.setErrorRateThreshold(100);

        // Assert
        assertThat(config.getErrRate()).isEqualTo(1.0);
    }

    /**
     * 测试 setErrorRateThreshold 为 null 不抛异常
     * 测试目的：验证 threshold 为 null 时 setErrorRateThreshold 安全返回
     * 测试场景：setErrorRateThreshold(null)
     * 验证内容：不抛异常，errRate 为 0
     */
    @Test
    public void testSetErrorRateThresholdNull() {
        // Arrange
        Config config = new Config();

        // Act & Assert
        assertThatCode(() -> config.setErrorRateThreshold(null)).doesNotThrowAnyException();
        assertThat(config.getErrRate()).isZero();
    }

    /**
     * 测试基本字段 setter/getter
     * 测试目的：验证 requestVolumeThreshold/metricNumBuckets 读写
     * 测试场景：设置两个字段后读取
     * 验证内容：读取值与设置值一致
     */
    @Test
    public void testBasicSettersAndGetters() {
        // Arrange
        Config config = new Config();

        // Act
        config.setRequestVolumeThreshold(10);
        config.setMetricNumBuckets(5);

        // Assert
        assertThat(config.getRequestVolumeThreshold()).isEqualTo(10);
        assertThat(config.getMetricNumBuckets()).isEqualTo(5);
    }

    /**
     * 测试 verify 通过
     * 测试目的：验证所有字段为正且 errorRateThreshold<=100 时 verify 不抛异常
     * 测试场景：requestVolumeThreshold=10，errorRateThreshold=50，metricNumBuckets=5
     * 验证内容：verify 正常返回
     */
    @Test
    public void testVerifyPass() {
        // Arrange
        Config config = new Config();
        config.setRequestVolumeThreshold(10);
        config.setErrorRateThreshold(50);
        config.setMetricNumBuckets(5);

        // Act & Assert
        assertThatCode(config::verify).doesNotThrowAnyException();
    }

    /**
     * 测试 verify 在 requestVolumeThreshold 为 null 时抛异常
     * 测试目的：验证 requestVolumeThreshold 为 null 时抛异常
     * 测试场景：仅设置 errorRateThreshold 和 metricNumBuckets
     * 验证内容：抛出 IllegalArgumentException
     */
    @Test
    public void testVerifyNullRequestVolume() {
        // Arrange
        Config config = new Config();
        config.setErrorRateThreshold(50);
        config.setMetricNumBuckets(5);

        // Act & Assert
        assertThatThrownBy(config::verify).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 测试 verify 在 errorRateThreshold 超过 100 时抛异常
     * 测试目的：验证 errorRateThreshold>100 时抛异常
     * 测试场景：errorRateThreshold=120，其余字段正常
     * 验证内容：抛出 IllegalArgumentException 且消息包含 100
     */
    @Test
    public void testVerifyErrorRateOver100() {
        // Arrange
        Config config = new Config();
        config.setRequestVolumeThreshold(10);
        config.setErrorRateThreshold(120);
        config.setMetricNumBuckets(5);

        // Act & Assert
        assertThatThrownBy(config::verify)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");
    }

    /**
     * 测试 setDefault 从默认对象补全字段
     * 测试目的：验证 setDefault 在字段为 null 时从默认对象继承
     * 测试场景：默认对象三个字段均设值，当前对象不设置
     * 验证内容：setDefault 后三个字段与默认对象一致
     */
    @Test
    public void testSetDefaultInherits() {
        // Arrange
        Config config = new Config();
        Config defaultConfig = new Config();
        defaultConfig.setRequestVolumeThreshold(10);
        defaultConfig.setErrorRateThreshold(50);
        defaultConfig.setMetricNumBuckets(5);

        // Act
        config.setDefault(defaultConfig);

        // Assert
        assertThat(config.getRequestVolumeThreshold()).isEqualTo(10);
        assertThat(config.getErrorRateThreshold()).isEqualTo(50);
        assertThat(config.getMetricNumBuckets()).isEqualTo(5);
    }

    /**
     * 测试 setDefault 不覆盖已设值
     * 测试目的：验证 setDefault 不覆盖非 null 字段
     * 测试场景：当前 requestVolumeThreshold=20，默认对象为 10
     * 验证内容：setDefault 后 requestVolumeThreshold 仍为 20
     */
    @Test
    public void testSetDefaultNotOverride() {
        // Arrange
        Config config = new Config();
        config.setRequestVolumeThreshold(20);
        Config defaultConfig = new Config();
        defaultConfig.setRequestVolumeThreshold(10);

        // Act
        config.setDefault(defaultConfig);

        // Assert
        assertThat(config.getRequestVolumeThreshold()).isEqualTo(20);
    }

    /**
     * 测试 toString 包含字段值
     * 测试目的：验证 toString 输出包含各字段
     * 测试场景：设置字段后调用 toString
     * 验证内容：toString 包含 requestVolumeThreshold 值
     */
    @Test
    public void testToString() {
        // Arrange
        Config config = new Config();
        config.setRequestVolumeThreshold(10);

        // Act
        String str = config.toString();

        // Assert
        assertThat(str).contains("10");
    }
}
