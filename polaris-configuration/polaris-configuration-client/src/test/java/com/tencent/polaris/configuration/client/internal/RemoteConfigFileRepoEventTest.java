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

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileResponse;
import com.tencent.polaris.api.plugin.event.BaseEvent;
import com.tencent.polaris.api.plugin.event.ConfigEvent;
import com.tencent.polaris.api.plugin.event.EventConstants;
import com.tencent.polaris.api.plugin.event.EventReporter;
import com.tencent.polaris.api.plugin.filter.ConfigFileFilterChain;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.client.ConfigFileTestUtils;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFileConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for initial load config event of {@link RemoteConfigFileRepo}.
 *
 * @author fishtailfu
 */
@RunWith(MockitoJUnitRunner.class)
public class RemoteConfigFileRepoEventTest {

    private static final String CLIENT_ID = "127.0.0.1_1234_0";

    private static final String CLIENT_IP = "127.0.0.1";

    @Mock
    private SDKContext sdkContext;
    @Mock
    private Extensions extensions;
    @Mock
    private EventReporter eventReporter;
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
        when(sdkContext.getExtensions()).thenReturn(extensions);
        lenient().when(configFileConnector.isNotifiedVersionIncreaseStrictly()).thenReturn(true);

        ValueContext valueContext = new ValueContext();
        valueContext.setClientId(CLIENT_ID);
        valueContext.setHost(CLIENT_IP);
        lenient().when(extensions.getValueContext()).thenReturn(valueContext);
        lenient().when(extensions.getEventReporterList()).thenReturn(Collections.singletonList(eventReporter));
        lenient().when(eventReporter.isEnabled()).thenReturn(true);
        lenient().when(eventReporter.reportEvent(any())).thenReturn(true);
    }

    /**
     * 测试目的：验证首次从远端加载成功后上报一次配置轨迹。
     * 测试场景：服务端返回版本名为 v1.0.0 的配置文件。
     * 验证内容：上报一条 ConfigUpdated 事件，坐标、版本名、客户端信息正确。
     */
    @Test
    public void testReportEventWhenInitialPullSuccess() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any())).thenReturn(
                new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", buildConfigFile(100, "v1.0.0")));

        // Act
        new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        ConfigEvent configEvent = captureSingleConfigEvent();
        assertThat(configEvent.getEventType()).isEqualTo(EventConstants.EventType.CONFIG);
        assertThat(configEvent.getEventName()).isEqualTo(EventConstants.EventName.ConfigUpdated);
        assertThat(configEvent.getTimestamp()).isNotNull();
        assertThat(configEvent.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(configEvent.getClientIp()).isEqualTo(CLIENT_IP);
        assertThat(configEvent.getNamespace()).isEqualTo(ConfigFileTestUtils.testNamespace);
        assertThat(configEvent.getConfigGroup()).isEqualTo(ConfigFileTestUtils.testGroup);
        assertThat(configEvent.getConfigFileName()).isEqualTo(ConfigFileTestUtils.testFileName);
        assertThat(configEvent.getConfigFileVersion()).isEqualTo("v1.0.0");
    }

    /**
     * 测试目的：验证远端不可用、降级到本地缓存时也上报首次加载轨迹。
     * 测试场景：远端拉取超时，本地缓存命中版本名为 v0.9.0 的配置。
     * 验证内容：上报一条事件，版本名取自本地缓存。
     */
    @Test
    public void testReportEventWhenFallbackToLocalCache() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any()))
                .thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean()))
                .thenReturn(buildConfigFile(90, "v0.9.0"));

        // Act
        new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        ConfigEvent configEvent = captureSingleConfigEvent();
        assertThat(configEvent.getConfigFileVersion()).isEqualTo("v0.9.0");
    }

    /**
     * 测试目的：验证远端不存在配置文件时不上报轨迹。
     * 测试场景：服务端返回 NOT_FOUND。
     * 验证内容：没有任何事件上报。
     */
    @Test
    public void testNotReportEventWhenConfigFileNotFound() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.NOT_FOUND_RESOURCE, "", null));

        // Act
        new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        verify(eventReporter, never()).reportEvent(any());
    }

    /**
     * 测试目的：验证首次加载失败且无本地缓存时不上报轨迹。
     * 测试场景：远端拉取持续超时，本地缓存未命中。
     * 验证内容：没有任何事件上报。
     */
    @Test
    public void testNotReportEventWhenInitialLoadFailed() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any()))
                .thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean())).thenReturn(null);

        // Act
        new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        verify(eventReporter, never()).reportEvent(any());
    }

    /**
     * 测试目的：验证 SDK 只上报首次加载，后续变更交给上层框架，避免重复上报。
     * 测试场景：首次加载 v1.0.0 后，长轮询通知新版本并拉取到 v1.0.1。
     * 验证内容：配置已更新为新版本，但事件只上报过一次且版本为 v1.0.0。
     */
    @Test
    public void testNotReportEventWhenLongPollNotified() throws InterruptedException {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", buildConfigFile(100, "v1.0.0")))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", buildConfigFile(101, "v1.0.1")));
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);

        // Act
        remoteConfigFileRepo.onLongPollNotified(101);
        TimeUnit.MILLISECONDS.sleep(500);

        // Assert
        assertThat(remoteConfigFileRepo.getConfigFileVersion()).isEqualTo(101);
        ConfigEvent configEvent = captureSingleConfigEvent();
        assertThat(configEvent.getConfigFileVersion()).isEqualTo("v1.0.0");
    }

    /**
     * 测试目的：验证上报异常不影响配置加载。
     * 测试场景：获取客户端信息时抛出运行时异常。
     * 验证内容：构造正常完成，配置内容可读，且未上报事件。
     */
    @Test
    public void testReportEventFailureNotAffectLoading() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any())).thenReturn(
                new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", buildConfigFile(100, "v1.0.0")));
        when(extensions.getValueContext()).thenThrow(new IllegalStateException("mock failure"));

        // Act
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        assertThat(remoteConfigFileRepo.getContent()).isEqualTo("hello world");
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);
        verify(eventReporter, never()).reportEvent(any());
    }

    /**
     * 测试目的：验证配置文件对象的 getFileVersion 跟随仓库中当前生效配置的版本名，
     * 上层框架在没有变更事件时（如启动期间补齐错过的变更）也能拿到版本名。
     * 测试场景：首次加载 v1.0.0，随后长轮询拉到 v1.0.1。
     * 验证内容：KV 文件对象的 getFileVersion 先返回 v1.0.0，变更后返回 v1.0.1。
     */
    @Test
    public void testFileVersionFollowsRepoVersionName() throws InterruptedException {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", buildConfigFile(100, "v1.0.0")))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", buildConfigFile(101, "v1.0.1")));
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);
        ConfigPropertiesFile configKVFile = new ConfigPropertiesFile(ConfigFileTestUtils.testNamespace,
                ConfigFileTestUtils.testGroup, ConfigFileTestUtils.testFileName, remoteConfigFileRepo,
                new ConfigFileConfigImpl());

        // Act
        String initialVersion = configKVFile.getFileVersion();
        remoteConfigFileRepo.onLongPollNotified(101);
        TimeUnit.MILLISECONDS.sleep(500);

        // Assert
        assertThat(initialVersion).isEqualTo("v1.0.0");
        assertThat(configKVFile.getFileVersion()).isEqualTo("v1.0.1");
    }

    /**
     * 测试目的：验证内存中没有配置时版本名为空。
     * 测试场景：服务端返回 NOT_FOUND。
     * 验证内容：仓库与配置文件对象的版本名均为 null。
     */
    @Test
    public void testFileVersionNullWhenConfigFileNotFound() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.NOT_FOUND_RESOURCE, "", null));

        // Act
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);
        DefaultConfigFile configFile = new DefaultConfigFile(ConfigFileTestUtils.testNamespace,
                ConfigFileTestUtils.testGroup, ConfigFileTestUtils.testFileName, remoteConfigFileRepo,
                new ConfigFileConfigImpl());

        // Assert
        assertThat(remoteConfigFileRepo.getVersionName()).isNull();
        assertThat(configFile.getFileVersion()).isNull();
    }

    private ConfigFile buildConfigFile(long version, String versionName) {
        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                ConfigFileTestUtils.testFileName);
        configFile.setContent("hello world");
        configFile.setVersion(version);
        configFile.setName(versionName);
        configFile.setMd5("md5abc");
        return configFile;
    }

    private ConfigEvent captureSingleConfigEvent() {
        ArgumentCaptor<BaseEvent> captor = ArgumentCaptor.forClass(BaseEvent.class);
        verify(eventReporter, times(1)).reportEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(ConfigEvent.class);
        return (ConfigEvent) captor.getValue();
    }
}
