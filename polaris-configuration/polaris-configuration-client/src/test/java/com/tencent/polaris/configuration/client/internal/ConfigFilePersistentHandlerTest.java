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
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.client.LogCapture;
import com.tencent.polaris.encrypt.util.AESUtil;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.configuration.ConfigFileConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ConfigFilePersistentHandler}.
 *
 * @author evelynwei
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigFilePersistentHandlerTest {

    private static final String NAMESPACE = "default";

    private static final String FILE_GROUP = "biz-group";

    private static final String FILE_NAME = "biz-config";

    private static final String PLAIN_CONTENT = "jdbc.password=very-secret-value";

    /**
     * 固定的错误密钥，供「密钥不匹配」用例使用，避免随机密钥带来的偶发通过。
     */
    private static final byte[] MISMATCHED_AES_KEY = new byte[] {
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};

    private static final long VERSION = 100L;

    private static final String MD5 = "md5-of-source-content";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private SDKContext sdkContext;

    private String persistDir;

    private ConfigFilePersistentHandler handler;

    private byte[] aesKey;

    private String cipherContent;

    @Before
    public void setUp() throws IOException {
        persistDir = temporaryFolder.newFolder("config-cache").getAbsolutePath();
        ConfigurationImpl configuration = new ConfigurationImpl();
        ConfigFileConfigImpl configFileConfig = new ConfigFileConfigImpl();
        ConnectorConfigImpl connectorConfig = new ConnectorConfigImpl();
        connectorConfig.setConnectorType("polaris");
        connectorConfig.setPersistEnable(true);
        connectorConfig.setPersistDir(persistDir);
        connectorConfig.setPersistMaxWriteRetry(1);
        connectorConfig.setPersistMaxReadRetry(0);
        connectorConfig.setPersistRetryInterval(1L);
        configFileConfig.setServerConnector(connectorConfig);
        configuration.setConfigFile(configFileConfig);
        when(sdkContext.getConfig()).thenReturn(configuration);
        handler = new ConfigFilePersistentHandler(sdkContext);
        // 模拟服务端加密行为：AES 密钥由服务端生成，正文用该密钥加密后下发
        aesKey = AESUtil.generateAesKey();
        cipherContent = AESUtil.encrypt(PLAIN_CONTENT, aesKey);
    }

    /**
     * 测试目的：验证加密配置落盘为密文态。
     * 测试场景：过滤器解密后的配置对象（content 明文、sourceContent 密文）写入缓存。
     * 验证内容：缓存文件中 content 为密文、带 cacheEncrypted 标记与 dataKey，且不含明文正文。
     */
    @Test
    public void testSaveEncryptedConfigFilePersistsCipherText() throws IOException {
        // Arrange
        ConfigFile configFile = assembleEncryptedConfigFile();

        // Act
        handler.saveConfigFile(configFile);

        // Assert
        String persisted = readPersistedText();
        assertThat(persisted).doesNotContain(PLAIN_CONTENT);
        assertThat(persisted).contains(cipherContent);
        assertThat(persisted).contains("cacheEncrypted: true");
        assertThat(persisted).contains(Base64.getEncoder().encodeToString(aesKey));
    }

    /**
     * 测试目的：验证持久化副本与业务内存对象隔离。
     * 测试场景：对正在被业务使用的明文对象执行落盘。
     * 验证内容：落盘后源对象 content 仍为明文，cacheEncrypted 仍为 false。
     */
    @Test
    public void testSaveEncryptedConfigFileNotModifySource() {
        // Arrange
        ConfigFile configFile = assembleEncryptedConfigFile();

        // Act
        handler.saveConfigFile(configFile);

        // Assert
        assertThat(configFile.getContent()).isEqualTo(PLAIN_CONTENT);
        assertThat(configFile.getSourceContent()).isEqualTo(cipherContent);
        assertThat(configFile.isCacheEncrypted()).isFalse();
    }

    /**
     * 测试目的：验证进程重启后可从本地密文缓存恢复明文（本次改动的核心场景）。
     * 测试场景：落盘后新建 handler 实例（不复用任何内存状态）读取缓存。
     * 验证内容：content 解密为明文，sourceContent 保留密文，cacheEncrypted 被重置为 false，元数据不丢失。
     */
    @Test
    public void testLoadEncryptedConfigFileAfterRestart() throws IOException {
        // Arrange
        handler.saveConfigFile(assembleEncryptedConfigFile());
        ConfigFilePersistentHandler restartedHandler = new ConfigFilePersistentHandler(sdkContext);

        // Act
        ConfigFile loaded = restartedHandler.loadPersistedConfigFile(assembleConfigFileReq(), false);

        // Assert
        assertThat(loaded).isNotNull();
        assertThat(loaded.getContent()).isEqualTo(PLAIN_CONTENT);
        assertThat(loaded.getSourceContent()).isEqualTo(cipherContent);
        assertThat(loaded.isCacheEncrypted()).isFalse();
        assertThat(loaded.isEncrypted()).isTrue();
        assertThat(loaded.getDataKey()).isEqualTo(Base64.getEncoder().encodeToString(aesKey));
        assertThat(loaded.getEncryptAlgo()).isEqualTo("AES");
        assertThat(loaded.getMd5()).isEqualTo(MD5);
        assertThat(loaded.getVersion()).isEqualTo(VERSION);
    }

    /**
     * 测试目的：验证未加密配置行为完全不变。
     * 测试场景：sourceContent 为空的普通配置落盘后读回。
     * 验证内容：缓存文件为明文、cacheEncrypted 为 false，读回内容一致。
     */
    @Test
    public void testPlainConfigFileRoundTripUnchanged() throws IOException {
        // Arrange
        ConfigFile configFile = assembleConfigFileReq();
        configFile.setContent(PLAIN_CONTENT);
        configFile.setVersion(VERSION);
        configFile.setMd5(MD5);

        // Act
        handler.saveConfigFile(configFile);
        ConfigFile loaded = handler.loadPersistedConfigFile(assembleConfigFileReq(), false);

        // Assert
        assertThat(readPersistedText()).contains(PLAIN_CONTENT);
        assertThat(readPersistedText()).contains("cacheEncrypted: false");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getContent()).isEqualTo(PLAIN_CONTENT);
        assertThat(loaded.isCacheEncrypted()).isFalse();
    }

    /**
     * 测试目的：验证解开过密文却拿不到密钥时放弃落盘，绝不把明文写到磁盘。
     * 测试场景：sourceContent 非空但 dataKey 缺失（解密链路异常）。
     * 验证内容：缓存文件根本没被创建，读回为 null，磁盘上不存在明文。
     */
    @Test
    public void testSaveEncryptedConfigFileWithoutDataKeySkipsPersist() {
        // Arrange
        ConfigFile configFile = assembleEncryptedConfigFile();
        configFile.setDataKey(null);

        // Act
        handler.saveConfigFile(configFile);
        ConfigFile loaded = handler.loadPersistedConfigFile(assembleConfigFileReq(), false);

        // Assert
        assertThat(cacheFilePath()).as("plaintext must not be persisted").doesNotExist();
        assertThat(loaded).isNull();
    }

    /**
     * 测试目的：验证缓存文件权限收紧到仅属主可读写。
     * 测试场景：加密配置落盘，文件内同时有密文与解开它的 dataKey，等同于凭据文件。
     * 验证内容：POSIX 文件系统下权限为 rw-------。
     */
    @Test
    public void testPersistedCacheFileIsOwnerReadableOnly() throws IOException {
        // Arrange
        Path path = cacheFilePath();
        assumeTrue(path.getFileSystem().supportedFileAttributeViews().contains("posix"));

        // Act
        handler.saveConfigFile(assembleEncryptedConfigFile());

        // Assert
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(path))).isEqualTo("rw-------");
    }

    /**
     * 测试目的：验证升级前留下的「加密配置却落明文」的缓存会被丢弃，不让明文长期留存。
     * 测试场景：旧版本写入的缓存文件 encrypted=true、content 为明文、无 cacheEncrypted 字段。
     * 验证内容：返回 null 且缓存文件被删除，交由后续拉取重新落成密文态。
     */
    @Test
    public void testLoadLegacyPlaintextCacheOfEncryptedConfigIsDiscarded() throws IOException {
        // Arrange
        writeCacheFile(PLAIN_CONTENT, Base64.getEncoder().encodeToString(aesKey), "AES", null);

        // Act
        ConfigFile loaded = handler.loadPersistedConfigFile(assembleConfigFileReq(), false);

        // Assert
        assertThat(loaded).isNull();
        assertThat(cacheFilePath()).as("legacy plaintext cache must be deleted").doesNotExist();
    }

    /**
     * 测试目的：验证未开启加密的场景行为完全不变。
     * 测试场景：历史缓存文件 encrypted=false、content 为明文、无 cacheEncrypted 字段。
     * 验证内容：按明文原样返回，不丢弃、不删除、不触发解密。
     */
    @Test
    public void testLoadLegacyPlainCacheOfPlainConfigStillLoads() throws IOException {
        // Arrange
        writeCacheFile(PLAIN_CONTENT, null, null, null, false);

        // Act
        ConfigFile loaded = handler.loadPersistedConfigFile(assembleConfigFileReq(), false);

        // Assert
        assertThat(loaded).isNotNull();
        assertThat(loaded.getContent()).isEqualTo(PLAIN_CONTENT);
        assertThat(loaded.isCacheEncrypted()).isFalse();
        assertThat(cacheFilePath()).exists();
    }

    /**
     * 测试目的：验证密文缓存缺失密钥时不把密文当明文返回。
     * 测试场景：缓存标记 cacheEncrypted=true 但 dataKey 缺失。
     * 验证内容：返回 null，交由上层重试或降级。
     */
    @Test
    public void testLoadCacheFileMissingDataKey() throws IOException {
        // Arrange
        writeCacheFile(cipherContent, null, "AES", Boolean.TRUE);

        // Act
        ConfigFile loaded = handler.loadPersistedConfigFile(assembleConfigFileReq(), false);

        // Assert
        assertThat(loaded).isNull();
    }

    /**
     * 测试目的：验证非法 Base64 密钥不会把异常抛给调用方。
     * 测试场景：缓存 dataKey 为非法 Base64 串。
     * 验证内容：返回 null 且不抛异常。
     */
    @Test
    public void testLoadCacheFileWithInvalidDataKey() throws IOException {
        // Arrange
        writeCacheFile(cipherContent, "not-a-valid-base64-@@@", "AES", Boolean.TRUE);

        // Act & Assert
        assertThat(handler.loadPersistedConfigFile(assembleConfigFileReq(), false)).isNull();
    }

    /**
     * 测试目的：验证密钥不匹配时不返回乱码。
     * 测试场景：缓存密文与 dataKey 由不同密钥产生。
     * 验证内容：返回 null。
     *
     * <p>错误密钥固定而非随机：AES-CBC 无完整性校验，随机错误密钥有约 1/256 概率恰好通过
     * PKCS7 padding 校验从而解出乱码，会让本用例偶发失败。
     */
    @Test
    public void testLoadCacheFileWithMismatchedDataKey() throws IOException {
        // Arrange
        writeCacheFile(cipherContent, Base64.getEncoder().encodeToString(MISMATCHED_AES_KEY), "AES",
                Boolean.TRUE);

        // Act & Assert
        assertThat(handler.loadPersistedConfigFile(assembleConfigFileReq(), false)).isNull();
    }

    /**
     * 测试目的：验证未知加密算法快速失败。
     * 测试场景：缓存 encryptAlgo 为本版本不支持的算法。
     * 验证内容：返回 null，不尝试用 AES 解密。
     */
    @Test
    public void testLoadCacheFileWithUnsupportedAlgo() throws IOException {
        // Arrange
        writeCacheFile(cipherContent, Base64.getEncoder().encodeToString(aesKey), "SM4", Boolean.TRUE);

        // Act & Assert
        assertThat(handler.loadPersistedConfigFile(assembleConfigFileReq(), false)).isNull();
    }

    /**
     * 测试目的：验证缺失 encryptAlgo 的密文缓存按 AES 兼容处理。
     * 测试场景：缓存标记 cacheEncrypted=true 但无 encryptAlgo 字段。
     * 验证内容：仍能正常解密出明文。
     */
    @Test
    public void testLoadCacheFileWithoutAlgoUsesAes() throws IOException {
        // Arrange
        writeCacheFile(cipherContent, Base64.getEncoder().encodeToString(aesKey), null, Boolean.TRUE);

        // Act
        ConfigFile loaded = handler.loadPersistedConfigFile(assembleConfigFileReq(), false);

        // Assert
        assertThat(loaded).isNotNull();
        assertThat(loaded.getContent()).isEqualTo(PLAIN_CONTENT);
    }

    /**
     * 测试目的：验证落盘成功路径的日志不泄露正文与密钥。
     * 测试场景：加密配置落盘成功，捕获 start / end 两条 INFO 日志。
     * 验证内容：日志含文件坐标便于定位，但不含明文、密文与 dataKey。
     */
    @Test
    public void testSaveConfigFileSuccessLogsWithoutSecrets() {
        // Arrange
        ConfigFile configFile = assembleEncryptedConfigFile();

        // Act
        try (LogCapture capture = LogCapture.attach(ConfigFilePersistentHandler.class, Level.INFO)) {
            handler.saveConfigFile(configFile);

            // Assert
            String logs = capture.text();
            assertThat(logs).contains("start to save config file");
            assertThat(logs).contains("end to save config file");
            assertThat(logs).contains(FILE_NAME);
            assertThat(logs).doesNotContain(PLAIN_CONTENT);
            assertThat(logs).doesNotContain(cipherContent);
            assertThat(logs).doesNotContain(configFile.getDataKey());
        }
    }

    /**
     * 测试目的：验证落盘失败路径（含重试超限）的日志不泄露正文与密钥。
     * 测试场景：把锁文件路径预先占为目录，使加锁必然抛 IOException 触发重试与最终 error。
     * 验证内容：输出重试失败信息，且不含明文、密文与 dataKey。
     */
    @Test
    public void testSaveConfigFileFailureLogsWithoutSecrets() throws IOException {
        // Arrange：锁文件位置被目录占用，RandomAccessFile 打开必然失败
        Files.createDirectory(Paths.get(persistDir, cacheFileName() + ".lock"));
        ConfigFile configFile = assembleEncryptedConfigFile();

        // Act
        try (LogCapture capture = LogCapture.attach(ConfigFilePersistentHandler.class, Level.INFO)) {
            handler.saveConfigFile(configFile);

            // Assert
            String logs = capture.text();
            assertThat(logs).contains("fail to persist config file");
            assertThat(logs).doesNotContain(PLAIN_CONTENT);
            assertThat(logs).doesNotContain(cipherContent);
            assertThat(logs).doesNotContain(configFile.getDataKey());
        }
    }

    private ConfigFile assembleConfigFileReq() {
        return new ConfigFile(NAMESPACE, FILE_GROUP, FILE_NAME);
    }

    /**
     * 构造过滤器解密后的内存态对象：content 为明文，sourceContent 为服务端密文，
     * dataKey 为 Base64 编码的明文 AES 密钥。
     */
    private ConfigFile assembleEncryptedConfigFile() {
        ConfigFile configFile = assembleConfigFileReq();
        configFile.setContent(PLAIN_CONTENT);
        configFile.setSourceContent(cipherContent);
        configFile.setVersion(VERSION);
        configFile.setMd5(MD5);
        configFile.setName("v1.0.0");
        configFile.setEncrypted(true);
        configFile.setEncryptAlgo("AES");
        configFile.setDataKey(Base64.getEncoder().encodeToString(aesKey));
        return configFile;
    }

    private String cacheFileName() {
        return String.format("%s#%s#%s.yaml", NAMESPACE, FILE_GROUP, FILE_NAME);
    }

    private Path cacheFilePath() {
        return Paths.get(persistDir, cacheFileName());
    }

    private String readPersistedText() throws IOException {
        return new String(Files.readAllBytes(cacheFilePath()), StandardCharsets.UTF_8);
    }

    /**
     * 直接构造缓存文件，用于覆盖历史格式与损坏格式等无法由本版本写出的场景。
     */
    private void writeCacheFile(String content, String dataKey, String encryptAlgo, Boolean cacheEncrypted)
            throws IOException {
        writeCacheFile(content, dataKey, encryptAlgo, cacheEncrypted, true);
    }

    /**
     * 直接构造缓存文件，可指定 encrypted 字段，用于区分加密配置与普通配置的历史缓存。
     */
    private void writeCacheFile(String content, String dataKey, String encryptAlgo, Boolean cacheEncrypted,
            boolean encrypted) throws IOException {
        StringBuilder yaml = new StringBuilder();
        yaml.append("content: \"").append(content).append("\"\n");
        yaml.append("md5: \"").append(MD5).append("\"\n");
        yaml.append("version: ").append(VERSION).append("\n");
        yaml.append("encrypted: ").append(encrypted).append("\n");
        if (dataKey != null) {
            yaml.append("dataKey: \"").append(dataKey).append("\"\n");
        }
        if (encryptAlgo != null) {
            yaml.append("encryptAlgo: \"").append(encryptAlgo).append("\"\n");
        }
        if (cacheEncrypted != null) {
            yaml.append("cacheEncrypted: ").append(cacheEncrypted).append("\n");
        }
        Files.write(cacheFilePath(), yaml.toString().getBytes(StandardCharsets.UTF_8));
    }
}
