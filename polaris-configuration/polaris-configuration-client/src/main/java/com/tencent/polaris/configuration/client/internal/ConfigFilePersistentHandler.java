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


import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.tencent.polaris.api.plugin.configuration.ConfigFile;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.encrypt.EncryptConstants;
import com.tencent.polaris.encrypt.util.AESUtil;
import com.tencent.polaris.factory.util.FileUtils;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.LOCAL_FILE_CONNECTOR_TYPE;
import static com.tencent.polaris.api.config.verify.DefaultValues.PATTERN_CONFIG_FILE;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * 配置文件持久化处理器
 *
 * @author rod.xu
 * @date 2022/09/26
 */
public class ConfigFilePersistentHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigFilePersistentHandler.class);

    private final String persistDirPath;
    private final int maxWriteRetry;
    private final int maxReadRetry;
    private final long retryInterval;
    private final boolean isAllowPersist;
    private final String connectorType;
    private static final ExecutorService persistExecutor = Executors
            .newSingleThreadExecutor(new NamedThreadFactory("configFile-persistent-handler"));

    public ConfigFilePersistentHandler(SDKContext sdkContext) throws IOException {
        String persistDir = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistDir();
        this.maxReadRetry = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistMaxReadRetry();
        this.maxWriteRetry = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistMaxWriteRetry();
        this.retryInterval = sdkContext.getConfig().getConfigFile().getServerConnector()
                .getPersistRetryInterval();
        this.isAllowPersist = sdkContext.getConfig().getConfigFile().getServerConnector().getPersistEnable();
        this.connectorType = sdkContext.getConfig().getConfigFile().getServerConnector().getConnectorType();
        this.persistDirPath = Utils.translatePath(persistDir);
        FileUtils.dirPathCheck(this.persistDirPath);
    }

    private boolean isAllowPersistToFile() {
        return isAllowPersist && !LOCAL_FILE_CONNECTOR_TYPE.equals(connectorType);
    }

    public void asyncDeleteConfigFile(ConfigFile configFile) {
        if (!persistExecutor.isShutdown() && isAllowPersistToFile()) {
            persistExecutor.execute(new DeleteTask(configFile));
        }
    }

    /**
     * 删除服务缓存数据
     *
     * @param configFile config metadata
     */
    public void deleteFileConfig(ConfigFile configFile) {
        String fileName = configFileToFileName(configFile);
        String persistFilePath = persistDirPath + File.separator + fileName;
        try {
            Files.deleteIfExists(FileSystems.getDefault().getPath(persistFilePath));
        } catch (IOException e) {
            LOG.error("fail to delete cache file {}", persistFilePath);
        }
        String lockFileName = fileName + ".lock";
        String persistFileLockPath = persistDirPath + File.separator + lockFileName;
        try {
            Files.deleteIfExists(FileSystems.getDefault().getPath(persistFileLockPath));
        } catch (IOException e) {
            LOG.error("fail to delete cache lock file {}", persistFileLockPath);
        }
    }

    public void asyncSaveConfigFile(ConfigFile configFile) {
        if (!persistExecutor.isShutdown() && isAllowPersistToFile()) {
            persistExecutor.execute(new SaveTask(configFile));
        }
    }

    /**
     * 持久化配置文件
     *
     * @param configFile config file
     */
    public void saveConfigFile(ConfigFile configFile) {
        // 先定副本：拒绝落盘（会写出明文）时直接放弃，不进重试循环
        ConfigFile persistCopy = copyForPersist(configFile);
        if (persistCopy != null) {
            saveWithRetry(configFile, persistCopy);
        }
    }

    private void saveWithRetry(ConfigFile configFile, ConfigFile persistCopy) {
        String meta = configMeta(configFile);
        int retryTimes = 0;
        LOG.info("start to save config file {}", meta);
        while (retryTimes <= maxWriteRetry) {
            retryTimes++;
            Path path = doSaveConfigFile(configFile, persistCopy);
            if (null != path) {
                LOG.info("end to save config file {} to {}", meta, path);
                return;
            }
        }
        LOG.error("fail to persist config file {} after retry {}", meta, retryTimes);
    }

    /**
     * 日志用元数据串，不含任何配置正文与密钥。
     *
     * @param configFile 配置文件
     * @return 元数据串
     */
    private String configMeta(ConfigFile configFile) {
        return String.format("[namespace=%s, fileGroup=%s, fileName=%s, version=%d, encrypted=%s]",
                configFile.getNamespace(), configFile.getFileGroup(), configFile.getFileName(),
                configFile.getVersion(), configFile.isEncrypted());
    }

    /**
     * 创建持久化副本，与业务内存对象隔离，避免把业务正在使用的明文对象改成密文。
     *
     * <p>加密配置（sourceContent 与 dataKey 均非空）：content 取 sourceContent（服务端原始密文，
     * 无需二次加密），dataKey 原样保留 Base64(明文 AES 密钥) 随文件持久化以支撑进程重启后解密，
     * cacheEncrypted 置为 true。普通配置完全保持原有明文落盘行为。
     *
     * <p>判据用 sourceContent 而非 encrypted：加密 filter 在请求前就把 encrypted 置为 true 用于
     * 向服务端声明支持加密，因此 encrypted=true 不代表服务端真的返回了加密内容；而 sourceContent
     * 只在解密成功后被赋值，是「确实拿到并解开了密文」的可靠信号。
     *
     * <p>解开过密文但拿不到密钥时返回 null 表示放弃落盘：此时 content 已是明文，落盘会把明文写到
     * 磁盘上且标记为未加密，后续加载不再尝试解密，明文将静默留存。宁可没有缓存，也不能落明文。
     *
     * @param source 业务内存对象
     * @return 持久化副本；不应落盘时返回 null
     */
    private ConfigFile copyForPersist(ConfigFile source) {
        ConfigFile copy = null;
        String cipherText = source.getSourceContent();
        boolean decrypted = StringUtils.isNotBlank(cipherText);
        if (decrypted && StringUtils.isBlank(source.getDataKey())) {
            LOG.error("config file {} was decrypted but data key is missing, skip persisting to avoid "
                    + "writing plaintext to disk", configMeta(source));
        } else {
            copy = new ConfigFile(source.getNamespace(), source.getFileGroup(), source.getFileName());
            copy.setVersion(source.getVersion());
            copy.setName(source.getName());
            copy.setMd5(source.getMd5());
            copy.setEncrypted(source.isEncrypted());
            copy.setEncryptAlgo(source.getEncryptAlgo());
            copy.setReleaseTime(source.getReleaseTime());
            copy.setDataKey(source.getDataKey());
            if (decrypted) {
                // 加密配置：落盘密文态，content 与 sourceContent 一致以保持字段语义自洽
                copy.setContent(cipherText);
                copy.setSourceContent(cipherText);
                copy.setCacheEncrypted(true);
            } else {
                copy.setContent(source.getContent());
                copy.setSourceContent(source.getSourceContent());
                copy.setCacheEncrypted(false);
            }
        }
        return copy;
    }

    private static String configFileToFileName(ConfigFile configFile) {
        try {
            String encodedNamespace = URLEncoder.encode(configFile.getNamespace(), "UTF-8");
            String encodedFileGroup = URLEncoder.encode(configFile.getFileGroup(), "UTF-8");
            String encodeFileName = URLEncoder.encode(configFile.getFileName(), "UTF-8");
            return String.format(PATTERN_CONFIG_FILE, encodedNamespace, encodedFileGroup, encodeFileName);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is unknown");
        }
    }

    private void writeTmpFile(File persistTmpFile, File persistLockFile, ConfigFile persistCopy) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(persistLockFile, "rw");
             FileChannel channel = raf.getChannel()) {
            FileLock lock = channel.tryLock();
            if (lock == null) {
                throw new IOException("fail to lock file " + persistTmpFile
                        .getAbsolutePath() + ", ignore and retry later");
            }
            //执行保存
            try {
                doWriteTmpFile(persistTmpFile, persistCopy);
            } finally {
                lock.release();
            }
        }
    }

    private void doWriteTmpFile(File persistTmpFile, ConfigFile persistCopy) throws IOException {
        if (!persistTmpFile.exists()) {
            if (!persistTmpFile.createNewFile()) {
                LOG.warn("tmp file {} already exists", persistTmpFile.getAbsolutePath());
            }
        }
        //先收权限再写内容：否则会出现「内容已落盘、权限仍是 umask 默认」的窗口。
        //ATOMIC_MOVE 保留 inode 与权限，最终缓存文件同样是 0600
        restrictToOwnerOnly(persistTmpFile);
        try (FileOutputStream outputFile = new FileOutputStream(persistTmpFile)) {
            String jsonAsYaml = new YAMLMapper().writeValueAsString(persistCopy);
            outputFile.write(jsonAsYaml.getBytes(StandardCharsets.UTF_8));
            outputFile.flush();
        }
    }

    /**
     * 把缓存文件权限收紧到仅属主可读写。
     *
     * <p>加密配置的缓存文件里同时有密文和解开它的 dataKey：RSA 密钥对由 RSAService 每进程重新生成、
     * 不落盘，服务端下发的包裹密钥重启后必然解不开，要支持重启后仍能用缓存降级就只能落明文 AES 密钥。
     * 所以这份文件等同于凭据文件，按 ssh 私钥的方式用文件权限保护。
     *
     * <p>非 POSIX 文件系统（如 Windows）静默跳过，权限收紧失败也只告警不影响落盘。
     *
     * @param file 待收紧权限的文件
     */
    private void restrictToOwnerOnly(File file) {
        Path path = file.toPath();
        if (!path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"));
        } catch (IOException | UnsupportedOperationException e) {
            LOG.warn("fail to restrict permissions of cache file {}", path);
        }
    }

    private Path doSaveConfigFile(ConfigFile configFile, ConfigFile persistCopy) {
        String fileName = configFileToFileName(configFile);
        String tmpFileName = fileName + ".tmp";
        String lockFileName = fileName + ".lock";
        String persistFilePathStr = persistDirPath + File.separator + fileName;
        Path persistPath = FileSystems.getDefault().getPath(persistFilePathStr);
        File persistTmpFile = new File(persistDirPath + File.separator + tmpFileName);
        File persistLockFile = new File(persistDirPath + File.separator + lockFileName);
        try {
            if (!persistLockFile.exists()) {
                if (!persistLockFile.createNewFile()) {
                    LOG.warn("lock file {} already exists", persistLockFile.getAbsolutePath());
                }
            }
            writeTmpFile(persistTmpFile, persistLockFile, persistCopy);
            Files.move(FileSystems.getDefault().getPath(persistTmpFile.getAbsolutePath()),
                    persistPath, REPLACE_EXISTING, ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.error("fail to write file :" + persistTmpFile, e);
            return null;
        }
        return persistPath.toAbsolutePath();
    }

    /**
     * 从缓存目录加载缓存的配置文件
     *
     * @param configFile 配置文件
     * @return 配置文件
     */
    public ConfigFile loadPersistedConfigFile(ConfigFile configFile, boolean needRetry) {
        String fileName = configFileToFileName(configFile);
        String persistFilePathStr = persistDirPath + File.separator + fileName;
        Path persistPath = FileSystems.getDefault().getPath(persistFilePathStr);
        ConfigFile resConfigFile = null;
        if (needRetry) {
            int retryTimes = 0;
            while (retryTimes <= maxReadRetry) {
                retryTimes++;
                resConfigFile = loadConfigFile(persistPath.toFile(), configFile);
                if (null == resConfigFile) {
                    Utils.sleepUninterrupted(retryInterval);
                    continue;
                }
                break;
            }
            if (null == resConfigFile) {
                LOG.debug("fail to read config file from {} after retry {} times", fileName, retryTimes);
                return null;
            }
        } else {
            resConfigFile = loadConfigFile(persistPath.toFile(), configFile);
            if (null == resConfigFile) {
                LOG.debug("fail to read config file from {}.", fileName);
                return null;
            }
        }
        return resConfigFile;
    }

    private ConfigFile loadConfigFile(File persistFile, ConfigFile configFile) {
        if (null == persistFile || !persistFile.exists()) {
            return null;
        }
        Map<String, Object> jsonMap;
        // 先读完并关闭句柄再解析：Windows 不允许删除仍被打开的文件，历史明文缓存会删不掉
        try (InputStream inputStream = new FileInputStream(persistFile);
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            jsonMap = new Yaml().load(reader);
        } catch (IOException e) {
            LOG.warn("fail to read file :" + persistFile.getAbsoluteFile(), e);
            return null;
        }
        return parseConfigFile(jsonMap, persistFile, configFile);
    }

    /**
     * 把缓存文件解析结果回填成配置对象，并按缓存形态决定解密、丢弃或原样返回。
     *
     * @param jsonMap 缓存文件解析出的键值
     * @param persistFile 缓存文件，此时句柄已关闭，可安全删除
     * @param configFile 发起加载的配置坐标
     * @return 可用的配置文件；缓存不可用时返回 null
     */
    private ConfigFile parseConfigFile(Map<String, Object> jsonMap, File persistFile, ConfigFile configFile) {
        ConfigFile resConfigFile = new ConfigFile(configFile.getNamespace(),
                configFile.getFileGroup(), configFile.getFileName());
        resConfigFile.setContent(jsonMap.get("content").toString());
        resConfigFile.setMd5(jsonMap.get("md5").toString());
        resConfigFile.setVersion(Long.valueOf(String.valueOf(jsonMap.get("version"))));
        Object name = jsonMap.get("name");
        if (name != null) {
            resConfigFile.setName(name.toString());
        }
        Object sourceContent = jsonMap.get("sourceContent");
        if (sourceContent != null) {
            resConfigFile.setSourceContent(sourceContent.toString());
        }
        Object encrypted = jsonMap.get("encrypted");
        boolean encryptedValue = encrypted == null ? configFile.isEncrypted()
                : Boolean.parseBoolean(encrypted.toString());
        resConfigFile.setEncrypted(encryptedValue);
        Object encryptAlgo = jsonMap.get("encryptAlgo");
        if (encryptAlgo != null) {
            resConfigFile.setEncryptAlgo(encryptAlgo.toString());
        }
        Object dataKey = jsonMap.get("dataKey");
        if (dataKey != null) {
            resConfigFile.setDataKey(dataKey.toString());
        }
        // 历史缓存无 cacheEncrypted 字段，解析为 false 后走明文分支
        Object cacheEncrypted = jsonMap.get("cacheEncrypted");
        boolean isCacheEncrypted = cacheEncrypted != null && Boolean.parseBoolean(cacheEncrypted.toString());
        resConfigFile.setCacheEncrypted(isCacheEncrypted);
        if (isCacheEncrypted) {
            return decryptCachedContent(resConfigFile, persistFile.getName());
        }
        return discardIfLegacyPlaintextOfEncryptedConfig(resConfigFile, encryptedValue, persistFile);
    }

    /**
     * 丢弃「加密配置却落着明文」的历史缓存：本版本之前留下的文件没有 cacheEncrypted 标记，
     * 若原样返回，明文会一直留在磁盘上。删除后由后续拉取重新落成密文态。
     *
     * @param resConfigFile 已完成字段回填的缓存对象
     * @param encrypted 服务端标记的加密态
     * @param persistFile 缓存文件
     * @return 普通配置原样返回；加密配置的明文缓存返回 null
     */
    private ConfigFile discardIfLegacyPlaintextOfEncryptedConfig(ConfigFile resConfigFile, boolean encrypted,
            File persistFile) {
        ConfigFile result = resConfigFile;
        if (encrypted) {
            LOG.warn("cached config file {} is an encrypted config persisted as plaintext by an older version, "
                    + "discard and delete it", persistFile.getName());
            deletePlaintextCacheOfEncryptedConfig(persistFile);
            result = null;
        }
        return result;
    }

    /**
     * 删除「加密配置却落着明文」的历史缓存文件及其 lock 文件。删除失败只告警，不影响启动流程。
     *
     * @param persistFile 缓存文件
     */
    private void deletePlaintextCacheOfEncryptedConfig(File persistFile) {
        try {
            Files.deleteIfExists(persistFile.toPath());
            Files.deleteIfExists(FileSystems.getDefault().getPath(persistFile.getAbsolutePath() + ".lock"));
        } catch (IOException e) {
            LOG.warn("fail to delete legacy plaintext cache file {}", persistFile.getName());
        }
    }

    /**
     * 解密密文态缓存，进程重启后从磁盘取回密钥完成恢复。
     *
     * <p>cacheEncrypted=true 是本 SDK 自己写入的确定性标记，此时 content 一定是密文，因此任何
     * 不确定因素一律返回 null（视为缓存不可用，由上层重试或降级），绝不把密文当明文交给业务。
     *
     * @param resConfigFile 已完成字段回填的缓存对象，content 为密文
     * @param fileName 缓存文件名，仅用于日志定位
     * @return 解密后的配置文件，失败返回 null
     */
    private ConfigFile decryptCachedContent(ConfigFile resConfigFile, String fileName) {
        String dataKey = resConfigFile.getDataKey();
        String algo = resConfigFile.getEncryptAlgo();
        if (StringUtils.isBlank(dataKey)) {
            LOG.error("cached config file {} marked as encrypted but dataKey is missing, discard this cache",
                    fileName);
            return null;
        }
        // 只认 AES：encryptAlgo 由本 SDK 写入，未知算法说明缓存来自不兼容版本；缺失则按 AES 兼容处理
        if (StringUtils.isNotBlank(algo) && !EncryptConstants.ALGO_AES.equalsIgnoreCase(algo)) {
            LOG.error("cached config file {} uses unsupported encrypt algo {}, discard this cache", fileName, algo);
            return null;
        }
        // BouncyCastle 为可选依赖，缺失时加载 AESUtil 会抛 NoClassDefFoundError，须提前预判
        if (!EncryptConstants.isBouncyCastlePresent()) {
            LOG.error("cached config file {} is encrypted but bouncycastle is absent, discard this cache", fileName);
            return null;
        }
        return doDecryptCachedContent(resConfigFile, fileName);
    }

    private ConfigFile doDecryptCachedContent(ConfigFile resConfigFile, String fileName) {
        try {
            byte[] aesKey = Base64.getDecoder().decode(resConfigFile.getDataKey());
            String cipherText = resConfigFile.getContent();
            String plainText = AESUtil.decrypt(cipherText, aesKey);
            // 保留密文到 sourceContent，语义与远端拉取一致；content 交给业务的是明文
            resConfigFile.setSourceContent(cipherText);
            resConfigFile.setContent(plainText);
            resConfigFile.setCacheEncrypted(false);
            return resConfigFile;
        } catch (Throwable t) {
            // 捕 Throwable：密钥或算法相关失败可能以 Error 形式出现。不输出正文与密钥
            LOG.error("fail to decrypt cached config file {}, error: {}", fileName, t.getMessage());
            return null;
        }
    }

    private class DeleteTask implements Runnable {

        private ConfigFile configFile;

        public DeleteTask(ConfigFile configFile) {
            this.configFile = configFile;
        }

        @Override
        public void run() {
            deleteFileConfig(configFile);
        }
    }

    private class SaveTask implements Runnable {

        private ConfigFile configFile;

        public SaveTask(ConfigFile configFile) {
            this.configFile = configFile;
        }

        @Override
        public void run() {
            saveConfigFile(configFile);
        }
    }

    protected void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{persistExecutor});
    }
}
