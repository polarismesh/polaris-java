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

package com.tencent.polaris.configuration.client.util;

import ch.qos.logback.classic.Level;
import com.tencent.polaris.configuration.client.LogCapture;
import org.junit.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link YamlParser}.
 *
 * @author evelynwei
 */
public class YamlParserTest {

    private static final String SECRET_VALUE = "very-secret-value";

    private static final String YAML_CONTENT = "jdbc:\n  password: " + SECRET_VALUE + "\n";

    /**
     * 测试目的：验证 DEBUG 级别下解析日志不泄露配置正文。
     * 测试场景：解析含敏感值的 YAML，日志级别开到 DEBUG。
     * 验证内容：日志不含敏感值，但保留长度信息以便排障；解析结果本身仍为明文。
     */
    @Test
    public void testProcessDebugLogWithoutContent() {
        // Arrange
        YamlParser parser = new YamlParser();

        // Act
        try (LogCapture capture = LogCapture.attach(YamlParser.class, Level.DEBUG)) {
            Properties properties = parser.yamlToProperties(YAML_CONTENT);

            // Assert
            assertThat(properties.getProperty("jdbc.password")).isEqualTo(SECRET_VALUE);
            String logs = capture.text();
            assertThat(logs).doesNotContain(SECRET_VALUE);
            assertThat(logs).contains("length = " + YAML_CONTENT.length());
            assertThat(logs).contains("loaded 1 document(s)");
            // key 集合保留：排障需要知道解析出哪些配置项，key 本身不敏感
            assertThat(logs).contains("jdbc.password");
        }
    }

    /**
     * 测试目的：验证空内容解析不因日志改动抛 NPE。
     * 测试场景：传入 null 与空串。
     * 验证内容：返回空 Properties，日志输出 length = 0。
     */
    @Test
    public void testProcessEmptyContent() {
        // Arrange
        YamlParser parser = new YamlParser();

        // Act
        try (LogCapture capture = LogCapture.attach(YamlParser.class, Level.DEBUG)) {
            Properties properties = parser.yamlToProperties("");

            // Assert
            assertThat(properties).isEmpty();
            assertThat(capture.text()).contains("length = 0");
        }
    }
}
