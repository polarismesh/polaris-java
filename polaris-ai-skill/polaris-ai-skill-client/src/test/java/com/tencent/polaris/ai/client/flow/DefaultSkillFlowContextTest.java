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

package com.tencent.polaris.ai.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.ai.AiConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.config.skill.SkillConfig;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.skill.SkillConnector;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.config.skill.SkillConnectorConfigImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultSkillFlow} SDKContext wiring.
 *
 * @author Fishtail Fu
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultSkillFlowContextTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SDKContext sdkContext;

    @Mock
    private Configuration configuration;

    @Mock
    private AiConfig aiConfig;

    @Mock
    private SkillConfig skillConfig;

    @Mock
    private Extensions extensions;

    @Mock
    private Supplier supplier;

    @Mock
    private SkillConnector skillConnector;

    /**
     * 测试目的：从 SDKContext 装配 connector 与 persist
     * 测试场景：合法 persistDir，connector 返回成功
     * 验证内容：getSkill 走注入的 connector
     */
    @Test
    public void testSetSDKContextWiresConnector() throws Exception {
        // Arrange
        SkillConnectorConfigImpl connectorConfig = new SkillConnectorConfigImpl();
        connectorConfig.setConnectorType(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        connectorConfig.setFallbackToLocalCache(true);
        connectorConfig.setPersistEnable(true);
        connectorConfig.setPersistDir(temporaryFolder.newFolder().getAbsolutePath());
        connectorConfig.setPersistMaxWriteRetry(1);
        connectorConfig.setPersistMaxReadRetry(0);
        connectorConfig.setPersistRetryInterval(10L);
        when(sdkContext.getConfig()).thenReturn(configuration);
        when(configuration.getAi()).thenReturn(aiConfig);
        when(aiConfig.getSkill()).thenReturn(skillConfig);
        when(skillConfig.getServerConnector()).thenReturn(connectorConfig);
        when(sdkContext.getExtensions()).thenReturn(extensions);
        when(extensions.getPlugins()).thenReturn(supplier);
        when(supplier.getPlugin(PluginTypes.SKILL_CONNECTOR.getBaseType(),
                DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE)).thenReturn(skillConnector);
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");
        SkillGetResponse remote = new SkillGetResponse();
        remote.setContent("# wired");
        when(skillConnector.getSkill(request)).thenReturn(remote);
        DefaultSkillFlow flow = new DefaultSkillFlow();

        // Act
        flow.setSDKContext(sdkContext);
        SkillGetResponse result = flow.getSkill(request);

        // Assert
        assertThat(result.getContent()).isEqualTo("# wired");
        ArgumentCaptor<Destroyable> hookCaptor = ArgumentCaptor.forClass(Destroyable.class);
        verify(sdkContext).registerDestroyHook(hookCaptor.capture());
        hookCaptor.getValue().destroy();
    }

    /**
     * 测试目的：persist 目录非法时 handler 为空仍可 RPC
     * 测试场景：persistDir 指向普通文件
     * 验证内容：getSkill 仍返回远端结果
     */
    @Test
    public void testSetSDKContextWhenPersistDirIsFile() throws Exception {
        // Arrange
        File persistFile = temporaryFolder.newFile();
        SkillConnectorConfigImpl connectorConfig = new SkillConnectorConfigImpl();
        connectorConfig.setConnectorType(DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE);
        connectorConfig.setFallbackToLocalCache(true);
        connectorConfig.setPersistEnable(true);
        connectorConfig.setPersistDir(persistFile.getAbsolutePath());
        connectorConfig.setPersistMaxWriteRetry(1);
        connectorConfig.setPersistMaxReadRetry(0);
        connectorConfig.setPersistRetryInterval(10L);
        when(sdkContext.getConfig()).thenReturn(configuration);
        when(configuration.getAi()).thenReturn(aiConfig);
        when(aiConfig.getSkill()).thenReturn(skillConfig);
        when(skillConfig.getServerConnector()).thenReturn(connectorConfig);
        when(sdkContext.getExtensions()).thenReturn(extensions);
        when(extensions.getPlugins()).thenReturn(supplier);
        when(supplier.getPlugin(PluginTypes.SKILL_CONNECTOR.getBaseType(),
                DefaultPlugins.POLARIS_SKILL_CONNECTOR_TYPE)).thenReturn(skillConnector);
        SkillGetRequest request = new SkillGetRequest();
        request.setNamespace("default");
        request.setName("sql-analysis");
        request.setVersion("1.0.0");
        SkillGetResponse remote = new SkillGetResponse();
        remote.setContent("# no-handler");
        when(skillConnector.getSkill(request)).thenReturn(remote);
        DefaultSkillFlow flow = new DefaultSkillFlow();

        // Act
        flow.setSDKContext(sdkContext);
        SkillGetResponse result = flow.getSkill(request);

        // Assert
        assertThat(result.getContent()).isEqualTo("# no-handler");
    }
}
