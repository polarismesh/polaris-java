/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.plugin.filter.ConfigFileFilterChain;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.client.ConfigFileTestUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFileConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author lepdou 2022-03-08
 */
@RunWith(MockitoJUnitRunner.class)
public class RemoteConfigFileRepoTest {

    @Mock
    private SDKContext sdkContext;
    @Mock
    private ConfigFileConnector configFileConnector;
    @Mock
    private ConfigFileFilterChain configFileFilterChain;
    @Mock
    private ConfigFilePersistentHandler configFilePersistHandler;
    @Mock
    private ConfigFileLongPullService configFileLongPollingService;

    @Before
    public void before() {
        ConfigurationImpl configuration = new ConfigurationImpl();
        ConfigFileConfigImpl configFileConfig = new ConfigFileConfigImpl();
        ConnectorConfigImpl connectorConfig = new ConnectorConfigImpl();
        connectorConfig.setFallbackToLocalCache(true);
        configFileConfig.setServerConnector(connectorConfig);
        configuration.setConfigFile(configFileConfig);
        when(sdkContext.getConfig()).thenReturn(configuration);
        when(configFileConnector.isNotifiedVersionIncreaseStrictly()).thenReturn(true);
    }

    @Test
    public void testPullSuccess() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                ConfigFileTestUtils.testFileName);
        String content = "hello world";
        long version = 100;
        configFile.setContent(content);
        configFile.setVersion(version);
        ConfigFileResponse configFileResponse = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);

        when(configFileFilterChain.execute(any(), any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);

        verify(configFileFilterChain).execute(any(), any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(content, remoteConfigFileRepo.getContent());
        Assert.assertEquals(version, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testPullNotFoundConfigFile() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        ConfigFileResponse configFileResponse = new ConfigFileResponse(ServerCodes.NOT_FOUND_RESOURCE, "", null);

        when(configFileFilterChain.execute(any(), any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);

        verify(configFileFilterChain).execute(any(), any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertNull(remoteConfigFileRepo.getContent());
        Assert.assertEquals(0, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testPullWithUnexpectedResponseCode() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        ConfigFileResponse configFileResponse = new ConfigFileResponse(50000, "", null);

        when(configFileFilterChain.execute(any(), any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);

        //重试三次
        verify(configFileFilterChain, times(3)).execute(any(), any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertNull(remoteConfigFileRepo.getContent());
        Assert.assertEquals(0, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testPullWithRetryException() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        when(configFileFilterChain.execute(any(), any())).thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""));

        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);

        //重试三次
        verify(configFileFilterChain, times(3)).execute(any(), any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertNull(remoteConfigFileRepo.getContent());
        Assert.assertEquals(0, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testNotifyAndPullSecondTime() throws InterruptedException {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();

        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                ConfigFileTestUtils.testFileName);
        String content = "hello world";
        long version = 100;
        configFile.setContent(content);
        configFile.setVersion(version);
        ConfigFileResponse configFileResponse = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);

        when(configFileFilterChain.execute(any(), any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);

        AtomicInteger cbCnt = new AtomicInteger();
        //增加两个listener
        remoteConfigFileRepo.addChangeListener((configFileMetadata1, newContent) -> {
            cbCnt.getAndIncrement();
        });
        remoteConfigFileRepo.addChangeListener((configFileMetadata1, newContent) -> {
            cbCnt.getAndIncrement();
        });

        TimeUnit.MILLISECONDS.sleep(500);

        //第一次初始化拉取配置
        verify(configFileFilterChain).execute(any(), any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(0, cbCnt.get());
        Assert.assertEquals(content, remoteConfigFileRepo.getContent());
        Assert.assertEquals(version, remoteConfigFileRepo.getConfigFileVersion());

        //变更通知，重新拉取配置
        long newVersion = version + 1;
        String newContent = "hello world2";
        configFile.setContent(newContent);
        configFile.setVersion(newVersion);

        ConfigFileResponse configFileResponse2 = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);

        when(configFileFilterChain.execute(any(), any())).thenReturn(configFileResponse2);

        remoteConfigFileRepo.onLongPollNotified(newVersion);

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            //ignore
        }

        verify(configFileFilterChain, times(2)).execute(any(), any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(2, cbCnt.get()); //触发回调
        Assert.assertEquals(newContent, remoteConfigFileRepo.getContent());
        Assert.assertEquals(newVersion, remoteConfigFileRepo.getConfigFileVersion());

        //变更通知的版本号小于内存里缓存的版本号，则不会触发重新拉取配置
        long smallVersion = 100;
        remoteConfigFileRepo.onLongPollNotified(smallVersion);

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            //ignore
        }

        verify(configFileFilterChain, times(2)).execute(any(), any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(2, cbCnt.get()); //不触发回调，所以还是2次
        Assert.assertEquals(newContent, remoteConfigFileRepo.getContent());
        Assert.assertEquals(newVersion, remoteConfigFileRepo.getConfigFileVersion());
    }

    @Test
    public void testGetIdentifierWithNormalValues() {
        // 准备
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("testNamespace", "testGroup", "testFile");
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);
        // 验证
        assertThat(remoteConfigFileRepo.getIdentifier()).isEqualTo("testNamespace.testGroup.testFile");
    }

    @Test
    public void testGetIdentifierWithEmptyValues() {
        // 准备
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("", "", "");
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);
        // 验证
        assertThat(remoteConfigFileRepo.getIdentifier()).isEqualTo("..");
    }

    @Test
    public void testGetIdentifierWithSpecialCharacters() {
        // 准备
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("test@namespace", "test-group", "test_file.properties");
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);
        // 验证
        assertThat(remoteConfigFileRepo.getIdentifier()).isEqualTo("test@namespace.test-group.test_file.properties");
    }

    @Test
    public void testGetIdentifierWithNullValues() {
        // 准备
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata(null, null, null);
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);

        // 验证
        assertThat(remoteConfigFileRepo.getIdentifier()).isEqualTo("null.null.null");
    }

    @Test
    public void testGetIdentifierWithMixedValues() {
        // 准备
        ConfigFileMetadata configFileMetadata = new DefaultConfigFileMetadata("prod", "", "application.yaml");
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);

        // 验证
        assertThat(remoteConfigFileRepo.getIdentifier()).isEqualTo("prod..application.yaml");
    }
}
