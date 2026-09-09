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

import ch.qos.logback.classic.Level;
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
import com.tencent.polaris.configuration.client.LogCapture;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFileConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        configFile.setName("v1.0.0");
        configFile.setMd5("md5abc");
        ConfigFileResponse configFileResponse = new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile);

        when(configFileFilterChain.execute(any(), any())).thenReturn(configFileResponse);

        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain, configFileConnector,
                        configFileMetadata, configFilePersistHandler);

        verify(configFileFilterChain).execute(any(), any());
        verify(configFileLongPollingService).addConfigFile(remoteConfigFileRepo);

        Assert.assertEquals(content, remoteConfigFileRepo.getContent());
        Assert.assertEquals(version, remoteConfigFileRepo.getConfigFileVersion());
        ConfigFileSnapshot snapshot = remoteConfigFileRepo.getSnapshot();
        assertThat(snapshot.getVersion()).isEqualTo(version);
        assertThat(snapshot.getVersionName()).isEqualTo("v1.0.0");
        assertThat(snapshot.getMd5()).isEqualTo("md5abc");
        assertThat(remoteConfigFileRepo.isEncrypted()).isFalse();
    }

    /**
     * 测试目的：加密标记从服务端响应对象透出到 repo，供上层框架在首次加载时判断敏感性。
     * 测试场景：服务端返回 encrypted=true 的配置文件。
     * 验证内容：repo.isEncrypted() 为 true，且与快照的加密标记一致。
     */
    @Test
    public void testIsEncryptedReflectsServerResponse() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                ConfigFileTestUtils.testFileName);
        configFile.setContent("jdbc.password=secret");
        configFile.setVersion(100);
        configFile.setMd5("md5abc");
        configFile.setEncrypted(true);
        configFile.setEncryptAlgo("AES");
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile));

        // Act
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        assertThat(remoteConfigFileRepo.isEncrypted()).isTrue();
        assertThat(remoteConfigFileRepo.getSnapshot().isEncrypted()).isTrue();
    }

    /**
     * 测试目的：未拉到配置时加密标记不应误报为 true。
     * 测试场景：服务端返回 NOT_FOUND，内存中无配置对象。
     * 验证内容：isEncrypted() 为 false，不抛 NPE。
     */
    @Test
    public void testIsEncryptedFalseWhenNoConfigPulled() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.NOT_FOUND_RESOURCE, "", null));

        // Act
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        assertThat(remoteConfigFileRepo.isEncrypted()).isFalse();
    }

    /**
     * 测试目的：加密配置查询只返回源密文。
     * 测试场景：过滤链返回同时包含解密内容和 sourceContent 的配置。
     * 验证内容：快照 content 为密文。
     */
    @Test
    public void testEncryptedSnapshotUsesSourceContent() {
        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                ConfigFileTestUtils.testFileName);
        configFile.setContent("plain-text");
        configFile.setSourceContent("cipher-text");
        configFile.setEncrypted(true);
        configFile.setEncryptAlgo("AES");
        configFile.setDataKey("UDEyMzQ1Njc4OTAxMjM0NQ==");
        configFile.setVersion(1);
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile));

        RemoteConfigFileRepo repo = new RemoteConfigFileRepo(sdkContext, configFileLongPollingService,
                configFileFilterChain, configFileConnector, ConfigFileTestUtils.assembleDefaultConfigFileMeta(),
                configFilePersistHandler);

        ConfigFileSnapshot snapshot = repo.getSnapshot();
        assertThat(snapshot.getContent()).isEqualTo("cipher-text");
        assertThat(snapshot.isEncrypted()).isTrue();
        assertThat(snapshot.getEncryptAlgo()).isEqualTo("AES");
        assertThat(snapshot.getDataKey()).isEqualTo("UDEyMzQ1Njc4OTAxMjM0NQ==");
    }

    /**
     * 测试目的：旧加密缓存缺少源密文时禁止回退到解密明文。
     * 测试场景：加密配置只有 content，没有 sourceContent。
     * 验证内容：快照 content 为空串。
     */
    @Test
    public void testEncryptedSnapshotWithoutSourceDoesNotExposePlainText() {
        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                ConfigFileTestUtils.testFileName);
        configFile.setContent("plain-text");
        configFile.setEncrypted(true);
        configFile.setVersion(1);
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile));

        RemoteConfigFileRepo repo = new RemoteConfigFileRepo(sdkContext, configFileLongPollingService,
                configFileFilterChain, configFileConnector, ConfigFileTestUtils.assembleDefaultConfigFileMeta(),
                configFilePersistHandler);

        assertThat(repo.getSnapshot().getContent()).isEmpty();
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
        Assert.assertNull(remoteConfigFileRepo.getSnapshot());
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

    /**
     * 测试目的：内存为空且本地缓存命中时，第一次远端失败立即回落，不再走 1/2/4 秒退避。
     * 测试场景：过滤链抛超时，持久化层返回缓存。
     * 验证内容：只拉一次远端，业务内容来自缓存。
     */
    @Test
    public void testFallbackToLocalCacheOnFirstFailure() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile cachedConfigFile = new ConfigFile(ConfigFileTestUtils.testNamespace,
                ConfigFileTestUtils.testGroup, ConfigFileTestUtils.testFileName);
        cachedConfigFile.setContent("cached-plain");
        cachedConfigFile.setVersion(12);
        when(configFileFilterChain.execute(any(), any())).thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean())).thenReturn(cachedConfigFile);

        long startedAt = System.currentTimeMillis();
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);
        long elapsedMs = System.currentTimeMillis() - startedAt;

        verify(configFileFilterChain, times(1)).execute(any(), any());
        verify(configFilePersistHandler).loadPersistedConfigFile(any(), anyBoolean());
        assertThat(remoteConfigFileRepo.getContent()).isEqualTo("cached-plain");
        assertThat(remoteConfigFileRepo.getConfigFileVersion()).isEqualTo(12);
        assertThat(elapsedMs).as("must not wait exponential backoff 1+2+4s").isLessThan(2000);
    }

    /**
     * 测试目的：降级本地缓存后后台补拉一次，远端恢复即收敛，不必等长轮询。
     * 测试场景：首次拉取抛超时命中缓存，随后远端恢复返回更高版本。
     * 验证内容：构造完成时用缓存内容，补拉执行后内容与版本更新为远端值。
     */
    @Test
    public void testCatchUpPullConvergesAfterFallback() throws InterruptedException {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile cachedConfigFile = new ConfigFile(ConfigFileTestUtils.testNamespace,
                ConfigFileTestUtils.testGroup, ConfigFileTestUtils.testFileName);
        cachedConfigFile.setContent("cached-plain");
        cachedConfigFile.setVersion(12);
        ConfigFile freshConfigFile = new ConfigFile(ConfigFileTestUtils.testNamespace,
                ConfigFileTestUtils.testGroup, ConfigFileTestUtils.testFileName);
        freshConfigFile.setContent("remote-fresh");
        freshConfigFile.setVersion(13);
        when(configFileFilterChain.execute(any(), any()))
                .thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", freshConfigFile));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean())).thenReturn(cachedConfigFile);

        // Act
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        assertThat(remoteConfigFileRepo.getContent()).isEqualTo("cached-plain");
        TimeUnit.SECONDS.sleep(6);
        assertThat(remoteConfigFileRepo.getContent()).isEqualTo("remote-fresh");
        assertThat(remoteConfigFileRepo.getConfigFileVersion()).isEqualTo(13);
    }

    /**
     * 测试目的：补拉只试一次，且失败后不重复加载已在内存中的缓存。
     * 测试场景：远端持续失败，首次拉取降级命中缓存。
     * 验证内容：远端共两次调用（首拉 + 补拉），持久化层只读一次，内存配置保持不变。
     */
    @Test
    public void testCatchUpPullTriesOnceAndKeepsMemoryConfig() throws InterruptedException {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile cachedConfigFile = new ConfigFile(ConfigFileTestUtils.testNamespace,
                ConfigFileTestUtils.testGroup, ConfigFileTestUtils.testFileName);
        cachedConfigFile.setContent("cached-plain");
        cachedConfigFile.setVersion(12);
        when(configFileFilterChain.execute(any(), any())).thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean())).thenReturn(cachedConfigFile);

        // Act
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);
        TimeUnit.SECONDS.sleep(6);

        // Assert
        verify(configFileFilterChain, times(2)).execute(any(), any());
        verify(configFilePersistHandler, times(1)).loadPersistedConfigFile(any(), anyBoolean());
        assertThat(remoteConfigFileRepo.getContent()).isEqualTo("cached-plain");
    }

    /**
     * 测试目的：无本地缓存时仍耗尽三次远端重试。
     * 测试场景：过滤链持续失败，持久化层返回 null。
     * 验证内容：过滤链执行三次。
     */
    @Test
    public void testPullRetriesWhenLocalCacheMissing() {
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any())).thenThrow(new RetriableException(ErrorCode.API_TIMEOUT, ""));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean())).thenReturn(null);

        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);

        verify(configFileFilterChain, times(3)).execute(any(), any());
        verify(configFilePersistHandler, times(2)).loadPersistedConfigFile(any(), anyBoolean());
        Assert.assertNull(remoteConfigFileRepo.getContent());
    }

    /**
     * 测试目的：验证降级读本地缓存的日志不泄露正文与密钥。
     * 测试场景：远端拉取失败触发 fallback，缓存命中一个加密配置（正文为敏感串）。
     * 验证内容：成功日志只含坐标与版本，不含明文、密文与 dataKey。
     */
    @Test
    public void testLoadLocalCacheLogsWithoutSecrets() {
        // Arrange
        String sensitiveContent = "jdbc.password=cache-secret-value";
        String dataKey = "UDEyMzQ1Njc4OTAxMjM0NQ==";
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile cachedConfigFile = new ConfigFile(ConfigFileTestUtils.testNamespace,
                ConfigFileTestUtils.testGroup, ConfigFileTestUtils.testFileName);
        cachedConfigFile.setContent(sensitiveContent);
        cachedConfigFile.setSourceContent("cache-cipher-text");
        cachedConfigFile.setVersion(100);
        cachedConfigFile.setMd5("md5abc");
        cachedConfigFile.setEncrypted(true);
        cachedConfigFile.setDataKey(dataKey);
        when(configFileFilterChain.execute(any(), any())).thenReturn(new ConfigFileResponse(50000, "", null));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean())).thenReturn(cachedConfigFile);

        // Act
        try (LogCapture capture = LogCapture.attach(AbstractConfigFileRepo.class, Level.INFO)) {
            new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                    configFileConnector, configFileMetadata, configFilePersistHandler);

            // Assert
            String logs = capture.text();
            assertThat(logs).contains("load local cache success");
            assertThat(logs).contains(ConfigFileTestUtils.testFileName);
            assertThat(logs).contains("encrypted=true");
            assertThat(logs).doesNotContain(sensitiveContent);
            assertThat(logs).doesNotContain("cache-cipher-text");
            assertThat(logs).doesNotContain(dataKey);
        }
    }

    /**
     * 测试目的：验证缓存未命中时的失败日志同样只含坐标。
     * 测试场景：远端拉取失败且本地缓存返回 null。
     * 验证内容：输出 load local cache fail 与文件坐标，不含任何正文字段。
     */
    @Test
    public void testLoadLocalCacheFailLogsOnlyMetadata() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        when(configFileFilterChain.execute(any(), any())).thenReturn(new ConfigFileResponse(50000, "", null));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean())).thenReturn(null);

        // Act
        try (LogCapture capture = LogCapture.attach(AbstractConfigFileRepo.class, Level.INFO)) {
            new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                    configFileConnector, configFileMetadata, configFilePersistHandler);

            // Assert
            String logs = capture.text();
            assertThat(logs).contains("load local cache fail");
            assertThat(logs).contains(ConfigFileTestUtils.testFileName);
            assertThat(logs).doesNotContain("content=");
            assertThat(logs).doesNotContain("dataKey");
        }
    }

    /**
     * 测试目的：验证内存业务对象恒为明文态，不会继承缓存密文标记。
     * 测试场景：过滤链返回的对象被误置 cacheEncrypted=true。
     * 验证内容：deepCloneConfigFile 产生的内存对象 cacheEncrypted 仍为 false。
     */
    @Test
    public void testDeepCloneNotCopyCacheEncrypted() throws Exception {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile configFile = new ConfigFile(ConfigFileTestUtils.testNamespace, ConfigFileTestUtils.testGroup,
                ConfigFileTestUtils.testFileName);
        configFile.setContent("hello world");
        configFile.setVersion(100);
        configFile.setMd5("md5abc");
        configFile.setCacheEncrypted(true);
        when(configFileFilterChain.execute(any(), any()))
                .thenReturn(new ConfigFileResponse(ServerCodes.EXECUTE_SUCCESS, "", configFile));

        // Act
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        Field field = RemoteConfigFileRepo.class.getDeclaredField("remoteConfigFile");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        AtomicReference<ConfigFile> holder = (AtomicReference<ConfigFile>) field.get(remoteConfigFileRepo);
        assertThat(holder.get()).isNotNull();
        assertThat(holder.get().isCacheEncrypted()).isFalse();
        assertThat(holder.get().getContent()).isEqualTo("hello world");
    }

    /**
     * 测试目的：验证从本地密文缓存降级恢复后，加密语义与 ACK 链路仍然正确。
     * 测试场景：远端拉取失败降级读本地缓存，缓存已由持久化层解密（content 明文、sourceContent 密文）。
     * 验证内容：业务读到明文，而快照回传的是密文与密钥，不泄露明文。
     */
    @Test
    public void testFallbackToEncryptedLocalCache() {
        // Arrange
        ConfigFileMetadata configFileMetadata = ConfigFileTestUtils.assembleDefaultConfigFileMeta();
        ConfigFile cachedConfigFile = new ConfigFile(ConfigFileTestUtils.testNamespace,
                ConfigFileTestUtils.testGroup, ConfigFileTestUtils.testFileName);
        cachedConfigFile.setContent("plain-text");
        cachedConfigFile.setSourceContent("cipher-text");
        cachedConfigFile.setVersion(100);
        cachedConfigFile.setMd5("md5abc");
        cachedConfigFile.setEncrypted(true);
        cachedConfigFile.setEncryptAlgo("AES");
        cachedConfigFile.setDataKey("UDEyMzQ1Njc4OTAxMjM0NQ==");
        when(configFileFilterChain.execute(any(), any())).thenReturn(new ConfigFileResponse(50000, "", null));
        when(configFilePersistHandler.loadPersistedConfigFile(any(), anyBoolean())).thenReturn(cachedConfigFile);

        // Act
        RemoteConfigFileRepo remoteConfigFileRepo =
                new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, configFileFilterChain,
                        configFileConnector, configFileMetadata, configFilePersistHandler);

        // Assert
        assertThat(remoteConfigFileRepo.getContent()).isEqualTo("plain-text");
        ConfigFileSnapshot snapshot = remoteConfigFileRepo.getSnapshot();
        assertThat(snapshot.getContent()).isEqualTo("cipher-text");
        assertThat(snapshot.getMd5()).isEqualTo("md5abc");
        assertThat(snapshot.isEncrypted()).isTrue();
        assertThat(snapshot.getDataKey()).isEqualTo("UDEyMzQ1Njc4OTAxMjM0NQ==");
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
