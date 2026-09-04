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

package com.tencent.polaris.api.plugin.configuration;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link ConfigFile}.
 *
 * @author evelynwei
 */
public class ConfigFileTest {

    private static final String NAMESPACE = "default";

    private static final String FILE_GROUP = "biz-group";

    private static final String FILE_NAME = "biz-config";

    private static final String PLAIN_CONTENT = "jdbc.password=very-secret-value";

    private static final String CIPHER_CONTENT = "5Lit5paH5a+G5paH";

    /**
     * 测试目的：验证缓存格式标记的默认值与读写。
     * 测试场景：新建对象后读取默认值，再设置为 true。
     * 验证内容：默认为 false，设置后为 true。
     */
    @Test
    public void testCacheEncryptedDefaultFalse() {
        // Arrange & Act
        ConfigFile configFile = assembleConfigFile();

        // Assert
        assertThat(configFile.isCacheEncrypted()).isFalse();
        configFile.setCacheEncrypted(true);
        assertThat(configFile.isCacheEncrypted()).isTrue();
    }

    /**
     * 测试目的：验证 toString 不再泄露配置正文。
     * 测试场景：对象同时持有明文 content 与密文 sourceContent。
     * 验证内容：输出不含两者的值。
     */
    @Test
    public void testToStringWithoutContent() {
        // Arrange
        ConfigFile configFile = assembleConfigFile();

        // Act
        String str = configFile.toString();

        // Assert
        assertThat(str).doesNotContain(PLAIN_CONTENT);
        assertThat(str).doesNotContain(CIPHER_CONTENT);
    }

    /**
     * 测试目的：验证 toString 的问题定位能力不退化。
     * 测试场景：对象已设置完整元数据。
     * 验证内容：输出包含坐标、版本、md5 与加密标记。
     */
    @Test
    public void testToStringKeepsMetadata() {
        // Arrange
        ConfigFile configFile = assembleConfigFile();

        // Act
        String str = configFile.toString();

        // Assert
        assertThat(str).contains(NAMESPACE);
        assertThat(str).contains(FILE_GROUP);
        assertThat(str).contains(FILE_NAME);
        assertThat(str).contains("version=100");
        assertThat(str).contains("md5-of-source-content");
        assertThat(str).contains("encrypted=true");
    }

    /**
     * 测试目的：验证 cacheEncrypted 不参与业务身份判定。
     * 测试场景：两个内容一致的对象，仅 cacheEncrypted 不同。
     * 验证内容：equals 与 hashCode 均不受影响。
     */
    @Test
    public void testEqualsIgnoresCacheEncrypted() {
        // Arrange
        ConfigFile one = assembleConfigFile();
        ConfigFile another = assembleConfigFile();
        another.setCacheEncrypted(true);

        // Act & Assert
        assertThat(one).isEqualTo(another);
        assertThat(one.hashCode()).isEqualTo(another.hashCode());
    }

    private ConfigFile assembleConfigFile() {
        ConfigFile configFile = new ConfigFile(NAMESPACE, FILE_GROUP, FILE_NAME);
        configFile.setContent(PLAIN_CONTENT);
        configFile.setSourceContent(CIPHER_CONTENT);
        configFile.setVersion(100L);
        configFile.setName("v1.0.0");
        configFile.setMd5("md5-of-source-content");
        configFile.setEncrypted(true);
        configFile.setEncryptAlgo("AES");
        return configFile;
    }
}
