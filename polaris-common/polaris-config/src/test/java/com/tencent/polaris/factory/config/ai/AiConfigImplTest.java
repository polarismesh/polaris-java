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

package com.tencent.polaris.factory.config.ai;

import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.consumer.ConsumerConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFileConfigImpl;
import com.tencent.polaris.factory.config.global.GlobalConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.config.provider.ProviderConfigImpl;
import com.tencent.polaris.factory.config.skill.SkillConfigImpl;
import com.tencent.polaris.factory.config.skill.SkillConnectorConfigImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test for {@link AiConfigImpl} and skill inherit on {@link ConfigurationImpl}.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class AiConfigImplTest {

    /**
     * 测试目的：skill connector 为空时 verify 失败
     * 测试场景：AiConfig.skill.serverConnector 未设置
     * 验证内容：抛异常
     */
    @Test
    public void testSkillVerifyRequiresConnector() {
        // Arrange
        SkillConfigImpl skillConfig = new SkillConfigImpl();
        AiConfigImpl aiConfig = new AiConfigImpl();
        aiConfig.setSkill(skillConfig);

        // Act & Assert
        assertThatThrownBy(aiConfig::verify).isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试目的：skill 为空时 verify 直接通过
     * 测试场景：未 setSkill
     * 验证内容：不抛异常
     */
    @Test
    public void testAiVerifyWhenSkillNull() {
        // Arrange
        AiConfigImpl aiConfig = new AiConfigImpl();

        // Act & Assert
        assertThatCode(aiConfig::verify).doesNotThrowAnyException();
    }

    /**
     * 测试目的：合法 SkillConfig verify 通过
     * 测试场景：connectorType=polaris 且地址合法
     * 验证内容：不抛异常
     */
    @Test
    public void testSkillConfigVerifySuccess() {
        // Arrange
        SkillConnectorConfigImpl connector = new SkillConnectorConfigImpl();
        connector.setConnectorType(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        connector.setAddresses(Collections.singletonList("127.0.0.1:8094"));
        connector.setProtocol("grpc");
        connector.setConnectTimeout(500L);
        connector.setMessageTimeout(5000L);
        connector.setServerSwitchInterval(600000L);
        connector.setConnectionIdleTimeout(60000L);
        connector.setReconnectInterval(500L);
        SkillConfigImpl skillConfig = new SkillConfigImpl();
        skillConfig.setServerConnector(connector);

        // Act
        skillConfig.verify();

        // Assert
        assertThat(skillConfig.getServerConnector()).isSameAs(connector);
        AiConfigImpl aiConfig = new AiConfigImpl();
        aiConfig.setSkill(skillConfig);
        assertThatCode(aiConfig::verify).doesNotThrowAnyException();
    }

    /**
     * 测试目的：SkillConfig setDefault 空源不改字段
     * 测试场景：defaultObject=null
     * 验证内容：serverConnector 仍为空
     */
    @Test
    public void testSkillSetDefaultNullKeepsConnectorUnset() {
        // Arrange
        SkillConfigImpl skillConfig = new SkillConfigImpl();

        // Act
        skillConfig.setDefault(null);

        // Assert
        assertThat(skillConfig.getServerConnector()).isNull();
    }

    /**
     * 测试目的：已有 connector 时 setDefault 复用原对象
     * 测试场景：目标已 setServerConnector
     * 验证内容：仍是同一实例
     */
    @Test
    public void testSkillSetDefaultReusesExistingConnector() {
        // Arrange
        SkillConnectorConfigImpl existing = new SkillConnectorConfigImpl();
        existing.setConnectorType(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        SkillConfigImpl skillConfig = new SkillConfigImpl();
        skillConfig.setServerConnector(existing);
        SkillConnectorConfigImpl sourceConnector = new SkillConnectorConfigImpl();
        sourceConnector.setConnectorType(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        SkillConfigImpl source = new SkillConfigImpl();
        source.setServerConnector(sourceConnector);

        // Act
        skillConfig.setDefault(source);

        // Assert
        assertThat(skillConfig.getServerConnector()).isSameAs(existing);
    }

    /**
     * 测试目的：AiConfig setDefault 源为空仍会创建 skill
     * 测试场景：defaultObject=null
     * 验证内容：skill 非空
     */
    @Test
    public void testAiSetDefaultNullSourceCreatesSkill() {
        // Arrange
        AiConfigImpl target = new AiConfigImpl();

        // Act
        target.setDefault(null);

        // Assert
        assertThat(target.getSkill()).isNotNull();
    }

    /**
     * 测试目的：AiConfig 已有 skill 时 setDefault 复用
     * 测试场景：先 setSkill 再继承
     * 验证内容：skill 实例不变
     */
    @Test
    public void testAiSetDefaultReusesExistingSkill() {
        // Arrange
        SkillConfigImpl existing = new SkillConfigImpl();
        AiConfigImpl target = new AiConfigImpl();
        target.setSkill(existing);
        SkillConnectorConfigImpl sourceConnector = new SkillConnectorConfigImpl();
        sourceConnector.setConnectorType(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        SkillConfigImpl sourceSkill = new SkillConfigImpl();
        sourceSkill.setServerConnector(sourceConnector);
        AiConfigImpl source = new AiConfigImpl();
        source.setSkill(sourceSkill);

        // Act
        target.setDefault(source);

        // Assert
        assertThat(target.getSkill()).isSameAs(existing);
        assertThat(target.getSkill().getServerConnector().getConnectorType())
                .isEqualTo(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
    }

    /**
     * 测试目的：setDefault 在 skill 为空时新建并继承
     * 测试场景：source 带 polaris connector
     * 验证内容：目标 skill.connectorType 被填上
     */
    @Test
    public void testAiSetDefaultCreatesSkill() {
        // Arrange
        SkillConnectorConfigImpl sourceConnector = new SkillConnectorConfigImpl();
        sourceConnector.setConnectorType(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        SkillConfigImpl sourceSkill = new SkillConfigImpl();
        sourceSkill.setServerConnector(sourceConnector);
        AiConfigImpl source = new AiConfigImpl();
        source.setSkill(sourceSkill);
        AiConfigImpl target = new AiConfigImpl();

        // Act
        target.setDefault(source);

        // Assert
        assertThat(target.getSkill()).isNotNull();
        assertThat(target.getSkill().getServerConnector().getConnectorType())
                .isEqualTo(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
    }

    /**
     * 测试目的：skill 地址/token 为空时继承 global.serverConnector
     * 测试场景：global 有 8091 与 token
     * 验证内容：skill connector 地址与 token 被拷贝
     */
    @Test
    public void testConfigurationInheritsSkillConnectorFromGlobal() {
        // Arrange
        ServerConnectorConfigImpl globalConnector = new ServerConnectorConfigImpl();
        globalConnector.setAddresses(Collections.singletonList("127.0.0.1:8091"));
        globalConnector.setToken("skill-token");
        GlobalConfigImpl global = new GlobalConfigImpl();
        global.setServerConnector(globalConnector);
        SkillConnectorConfigImpl skillConnector = new SkillConnectorConfigImpl();
        SkillConfigImpl skillConfig = new SkillConfigImpl();
        skillConfig.setServerConnector(skillConnector);
        AiConfigImpl aiConfig = new AiConfigImpl();
        aiConfig.setSkill(skillConfig);
        ConfigurationImpl configuration = new ConfigurationImpl();
        configuration.setGlobal(global);
        configuration.setConsumer(new ConsumerConfigImpl());
        configuration.setProvider(new ProviderConfigImpl());
        configuration.setConfigFile(new ConfigFileConfigImpl());
        configuration.setAi(aiConfig);

        // Act
        configuration.setDefault(new ConfigurationImpl());

        // Assert
        assertThat(configuration.getAi().getSkill().getServerConnector().getAddresses())
                .containsExactly("127.0.0.1:8091");
        assertThat(configuration.getAi().getSkill().getServerConnector().getToken()).isEqualTo("skill-token");
        configuration.setAi(aiConfig);
        assertThat(configuration.getAi()).isSameAs(aiConfig);
    }
}
