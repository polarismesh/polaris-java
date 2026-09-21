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

package com.tencent.polaris.factory.config.skill;

import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link SkillConnectorConfigImpl}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class SkillConnectorConfigImplTest {

    /**
     * 测试目的：非法 connectorType 被拒绝
     * 测试场景：connectorType=nacos
     * 验证内容：IllegalArgumentException
     */
    @Test
    public void testVerifyRejectsUnsupportedConnectorType() {
        // Arrange
        SkillConnectorConfigImpl config = new SkillConnectorConfigImpl();
        config.setConnectorType("nacos");

        // Act & Assert
        assertThatThrownBy(config::verify).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 测试目的：空 persistDir 使用默认目录
     * 测试场景：合法 polaris 连接器
     * 验证内容：persistDir 回填默认值
     */
    @Test
    public void testVerifyFillsDefaultPersistDir() {
        // Arrange
        SkillConnectorConfigImpl config = validConfig();
        config.setPersistDir("");

        // Act
        config.verify();

        // Assert
        assertThat(config.getPersistDir()).isEqualTo(DefaultValues.SKILL_DEFAULT_CACHE_PERSIST_DIR);
        assertThat(config.getConnectorType()).isEqualTo(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
    }

    /**
     * 测试目的：setDefault 从 Skill 源配置补齐空字段
     * 测试场景：目标对象字段置空后继承 source
     * 验证内容：persist 与 connectorType 来自 source
     */
    @Test
    public void testSetDefaultCopiesSkillFields() throws Exception {
        // Arrange
        SkillConnectorConfigImpl source = validConfig();
        source.setPersistEnable(false);
        source.setPersistDir("/tmp/skill-cache");
        source.setPersistMaxWriteRetry(3);
        source.setPersistMaxReadRetry(2);
        source.setPersistRetryInterval(200L);
        source.setFallbackToLocalCache(false);
        source.setDownloadTimeout(45000L);
        SkillConnectorConfigImpl target = new SkillConnectorConfigImpl();
        setPrivateField(target, "persistEnable", null);
        setPrivateField(target, "persistMaxWriteRetry", null);
        setPrivateField(target, "persistMaxReadRetry", null);
        setPrivateField(target, "persistRetryInterval", null);
        setPrivateField(target, "fallbackToLocalCache", null);
        setPrivateField(target, "downloadTimeout", null);

        // Act
        target.setDefault(source);

        // Assert
        assertThat(target.getConnectorType()).isEqualTo(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        assertThat(target.getPersistEnable()).isFalse();
        assertThat(target.getPersistDir()).isEqualTo("/tmp/skill-cache");
        assertThat(target.getPersistMaxWriteRetry()).isEqualTo(3);
        assertThat(target.getPersistMaxReadRetry()).isEqualTo(2);
        assertThat(target.getPersistRetryInterval()).isEqualTo(200L);
        assertThat(target.getFallbackToLocalCache()).isFalse();
        assertThat(target.getDownloadTimeout()).isEqualTo(45000L);
    }

    /**
     * 测试目的：setDefault 也能吃 ServerConnectorConfig
     * 测试场景：传入纯 ServerConnectorConfigImpl
     * 验证内容：addresses 被继承
     */
    @Test
    public void testSetDefaultFromServerConnector() {
        // Arrange
        ServerConnectorConfigImpl source = new ServerConnectorConfigImpl();
        source.setAddresses(Collections.singletonList("10.0.0.1:8091"));
        source.setProtocol("grpc");
        SkillConnectorConfigImpl target = new SkillConnectorConfigImpl();

        // Act
        target.setDefault(source);

        // Assert
        assertThat(target.getAddresses()).containsExactly("10.0.0.1:8091");
        assertThat(target.getProtocol()).isEqualTo("grpc");
    }

    /**
     * 测试目的：setter 覆盖 getter
     * 测试场景：手工设置 persist 开关
     * 验证内容：读回一致
     */
    @Test
    public void testPersistSetters() {
        // Arrange
        SkillConnectorConfigImpl config = new SkillConnectorConfigImpl();

        // Act
        config.setPersistEnable(false);
        config.setFallbackToLocalCache(false);
        config.setPersistMaxWriteRetry(4);
        config.setPersistMaxReadRetry(5);
        config.setPersistRetryInterval(300L);
        config.setPersistDir("./backup");
        config.setConnectorType("polaris");
        config.setDownloadTimeout(30000L);

        // Assert
        assertThat(config.getPersistEnable()).isFalse();
        assertThat(config.getFallbackToLocalCache()).isFalse();
        assertThat(config.getPersistMaxWriteRetry()).isEqualTo(4);
        assertThat(config.getPersistMaxReadRetry()).isEqualTo(5);
        assertThat(config.getPersistRetryInterval()).isEqualTo(300L);
        assertThat(config.getPersistDir()).isEqualTo("./backup");
        assertThat(config.getDownloadTimeout()).isEqualTo(30000L);
    }

    /**
     * 测试目的：downloadTimeout 使用 30 秒默认值
     * 测试场景：创建默认 Skill connector 配置
     * 验证内容：downloadTimeout 为 30000 毫秒
     */
    @Test
    public void testDefaultDownloadTimeout() {
        // Arrange & Act
        SkillConnectorConfigImpl config = new SkillConnectorConfigImpl();

        // Assert
        assertThat(config.getDownloadTimeout()).isEqualTo(30000L);
    }

    /**
     * 测试目的：setDefault 空源不改字段
     * 测试场景：defaultObject=null
     * 验证内容：connectorType 仍为空
     */
    @Test
    public void testSetDefaultNullDoesNothing() {
        // Arrange
        SkillConnectorConfigImpl config = new SkillConnectorConfigImpl();

        // Act
        config.setDefault(null);

        // Assert
        assertThat(config.getConnectorType()).isNull();
    }

    private SkillConnectorConfigImpl validConfig() {
        SkillConnectorConfigImpl config = new SkillConnectorConfigImpl();
        config.setConnectorType(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        config.setAddresses(Collections.singletonList("127.0.0.1:8094"));
        config.setProtocol("grpc");
        config.setConnectTimeout(500L);
        config.setMessageTimeout(5000L);
        config.setServerSwitchInterval(600000L);
        config.setConnectionIdleTimeout(60000L);
        config.setReconnectInterval(500L);
        return config;
    }

    private static void setPrivateField(Object object, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
