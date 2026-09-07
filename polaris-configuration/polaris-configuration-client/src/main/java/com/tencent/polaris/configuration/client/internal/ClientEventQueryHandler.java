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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tencent.polaris.api.utils.ClassUtils;
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueProvider;
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueRegistration;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKeyConflict;
import com.tencent.polaris.configuration.api.core.EffectiveValue;
import com.tencent.polaris.encrypt.util.AESUtil;
import com.tencent.polaris.encrypt.util.RSAUtil;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 配置生效查询处理器，解析服务端 PUSH 指令并组装 ACK content JSON。
 * <p>
 * 任何分支都必须返回可发送的 JSON：服务端同步等待 ACK，静默会把它挂到超时。
 * 本类与连接器的日志均只记文件坐标与规模，不输出 PUSH/ACK 全文。
 *
 * @author evelynwei
 */
public class ClientEventQueryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ClientEventQueryHandler.class);

    /**
     * ACK 中 content 的最大字节数，超限截断，避免触发 gRPC 服务端默认 4MB 消息体上限。
     */
    private static final int MAX_ACK_CONTENT_BYTES = 512 * 1024;

    /**
     * ACK 中 properties 数组的最大序列化字节数，避免属性明细挤爆 gRPC 消息体。
     */
    private static final int MAX_ACK_PROPERTIES_BYTES = 512 * 1024;

    private static final String KIND_CONFIG = "config";

    private static final String REASON_BAD_CONTENT = "bad_content";

    private static final String REASON_UNKNOWN_KIND = "unknown_kind";

    private static final String REASON_CONFIG_DISABLED = "config_disabled";

    private static final String REASON_NOT_WATCHED = "not_watched";

    /** 已监听该配置文件但尚未拉取生效（首次拉取失败/重试中）。 */
    private static final String REASON_PENDING = "pending";

    /**
     * BouncyCastle Provider 全限定名，用于在触碰 AESUtil / RSAUtil 之前预判其是否在位。
     */
    private static final String BOUNCY_CASTLE_PROVIDER = "org.bouncycastle.jce.provider.BouncyCastleProvider";

    /**
     * 序列化 ACK 自身失败时的兜底应答，避免服务端收到无法诊断的空对象。
     */
    private static final String MARSHAL_FAILED_ACK = "{\"applied\":false,\"reason\":\"marshal_failed\"}";

    private static final String INTERNAL_ERROR_ACK = "{\"applied\":false,\"reason\":\"internal_error\"}";

    private final ConfigWatchReportRequestCustomizer watchRegistry;

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private final AtomicReference<ConfigEffectiveValueProvider> providerRef = new AtomicReference<>();

    public ClientEventQueryHandler(ConfigWatchReportRequestCustomizer watchRegistry) {
        this.watchRegistry = watchRegistry;
    }

    /**
     * 处理一条服务端推送事件，返回应答内容。任何分支都必须返回可发送的 JSON。
     *
     * @param index 事件序号（本实现不使用，由连接器回传）
     * @param pushContent PUSH.content 原始 JSON
     * @return ACK content JSON
     */
    public String onPush(long index, String pushContent) {
        try {
            return doOnPush(index, pushContent);
        } catch (RuntimeException e) {
            LOG.warn("[Config] handle config effective query failed: {}", e.getMessage());
            return INTERNAL_ERROR_ACK;
        }
    }

    private String doOnPush(long index, String pushContent) {
        ClientEventQuery query = parseQuery(pushContent);
        if (query == null) {
            return marshalAck(newAck(null, null, REASON_BAD_CONTENT));
        }
        ClientEventQuery.QueryConfig cfg = query.getConfig();
        LOG.info("[Config] handle config effective query, index = {}, kind = {}, file = {}/{}/{}",
                index, query.getKind(), namespaceOf(cfg), groupOf(cfg), fileNameOf(cfg));
        if (!KIND_CONFIG.equals(query.getKind())) {
            return marshalAck(newAck(query.getKind(), cfg, REASON_UNKNOWN_KIND));
        }
        if (watchRegistry == null) {
            return marshalAck(newAck(query.getKind(), cfg, REASON_CONFIG_DISABLED));
        }
        return handleConfigQuery(query);
    }

    /**
     * 注册配置生效值提供者，返回注销句柄。
     *
     * @param provider 提供者
     * @return 注册句柄，close 注销
     */
    public ConfigEffectiveValueRegistration registerProvider(ConfigEffectiveValueProvider provider) {
        providerRef.set(provider);
        return () -> providerRef.compareAndSet(provider, null);
    }

    private ClientEventQuery parseQuery(String pushContent) {
        try {
            return gson.fromJson(pushContent, ClientEventQuery.class);
        } catch (RuntimeException e) {
            LOG.warn("[Config] unmarshal push content failed: {}", e.getMessage());
            return null;
        }
    }

    private String handleConfigQuery(ClientEventQuery query) {
        ClientEventQuery.QueryConfig cfg = query.getConfig();
        // null 一律归一为 ""，对齐 Go 零值查询：构造不出合法坐标时查不到 → not_watched
        ConfigFileMetadata metadata = new DefaultConfigFileMetadata(namespaceOf(cfg), groupOf(cfg), fileNameOf(cfg));
        RemoteConfigFileRepo repo = watchRegistry.getWatchedFile(metadata);
        if (repo == null) {
            return marshalAck(newAck(KIND_CONFIG, cfg, REASON_NOT_WATCHED));
        }
        ConfigFileSnapshot snapshot = repo.getSnapshot();
        if (snapshot == null) {
            // 已监听但尚未拉取生效（首次拉取失败/重试中/已被删除）：applied=false + pending，
            // version 回带 notifiedVersion 供服务端参考，不带 md5/content/effective_time
            ClientEventAck pendingAck = newAck(KIND_CONFIG, cfg, REASON_PENDING);
            if (repo.getNotifiedVersion() > 0) {
                pendingAck.setVersion(repo.getNotifiedVersion());
            }
            return marshalAck(pendingAck);
        }
        ClientEventAck ack = newAck(KIND_CONFIG, cfg, null);
        ack.setApplied(true);
        fillSnapshot(ack, snapshot, query.getPublicKey());
        fillContent(ack, snapshot, metadata);
        fillProperties(ack, metadata, snapshot);
        return marshalAck(ack);
    }

    private String namespaceOf(ClientEventQuery.QueryConfig cfg) {
        return cfg == null || cfg.getNamespace() == null ? "" : cfg.getNamespace();
    }

    private String groupOf(ClientEventQuery.QueryConfig cfg) {
        return cfg == null || cfg.getGroup() == null ? "" : cfg.getGroup();
    }

    private String fileNameOf(ClientEventQuery.QueryConfig cfg) {
        return cfg == null || cfg.getFileName() == null ? "" : cfg.getFileName();
    }

    private void fillSnapshot(ClientEventAck ack, ConfigFileSnapshot snapshot, String publicKey) {
        // 对齐 Go 的 omitempty: version/version_name/md5/effective_time 零值时省略字段
        if (snapshot.getVersion() > 0) {
            ack.setVersion(snapshot.getVersion());
        }
        if (snapshot.getVersionName() != null && !snapshot.getVersionName().isEmpty()) {
            ack.setVersionName(snapshot.getVersionName());
        }
        if (snapshot.getMd5() != null && !snapshot.getMd5().isEmpty()) {
            ack.setMd5(snapshot.getMd5());
        }
        if (snapshot.getEffectiveTime() > 0) {
            ack.setEffectiveTime(snapshot.getEffectiveTime());
        }
        if (snapshot.isEncrypted()) {
            ack.setEncrypted(true);
            if (snapshot.getEncryptAlgo() != null && !snapshot.getEncryptAlgo().isEmpty()) {
                ack.setEncryptAlgo(snapshot.getEncryptAlgo());
            }
            String wrappedDataKey = wrapAckDataKey(snapshot, publicKey);
            if (!wrappedDataKey.isEmpty()) {
                ack.setDataKey(wrappedDataKey);
            }
        }
    }

    /**
     * ACK 侧能否用快照里的 data_key 做加密，两个前提缺一不可。
     *
     * <p>一是密钥已被加密过滤器解包成明文 AES 密钥。判据取 encryptAlgo：它只由
     * {@code CryptoConfigFileFilter} 在解包成功时与明文密钥一起写入，连接器只写 encrypted 和
     * 服务端下发的包裹密钥。过滤器未启用时 dataKey 仍是 RSA 包裹态，再包一层发回去服务端解不开。
     *
     * <p>二是 BouncyCastle 在位。AESUtil 与 RSAUtil 的静态初始化依赖它，缺失时抛的是
     * NoClassDefFoundError 而非 RuntimeException，不预判就会让整个 ACK 变成 internal_error；
     * 而配置是否加密由服务端决定，与客户端有没有装 BouncyCastle 无关，这条路径确实可达。
     *
     * @param snapshot 配置快照
     * @return 可以使用返回 true
     */
    private boolean isAckCryptoAvailable(ConfigFileSnapshot snapshot) {
        return snapshot.getEncryptAlgo() != null && !snapshot.getEncryptAlgo().isEmpty()
                && ClassUtils.isClassPresent(BOUNCY_CASTLE_PROVIDER);
    }

    /**
     * Wrap the symmetric data key with the query RSA public key.
     * Missing key or encrypt failure returns empty so Gson omits data_key; never return plaintext.
     *
     * @param snapshot 配置快照，data_key 为 Base64 明文 AES 密钥
     * @param publicKey PKCS1 / X.509 / PEM public key from PUSH
     * @return RSA wrapped data key, or empty on failure
     */
    private String wrapAckDataKey(ConfigFileSnapshot snapshot, String publicKey) {
        String wrapped = "";
        String plainDataKey = snapshot.getDataKey();
        if (plainDataKey != null && !plainDataKey.isEmpty() && publicKey != null && !publicKey.isEmpty()
                && isAckCryptoAvailable(snapshot)) {
            try {
                byte[] rawKey = Base64.getDecoder().decode(plainDataKey);
                wrapped = RSAUtil.encryptToBase64(rawKey, publicKey);
            } catch (Throwable t) {
                LOG.warn("[Config] rsa wrap data_key failed: {}", t.getMessage());
                wrapped = "";
            }
        }
        return wrapped;
    }

    /**
     * 填充 content，按 UTF-8 字节截断。不能用 substring 按 char 截——与 Go 行为不一致且可能切坏多字节字符。
     */
    private void fillContent(ClientEventAck ack, ConfigFileSnapshot snapshot, ConfigFileMetadata metadata) {
        String content = snapshot.getContent() == null ? "" : snapshot.getContent();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= MAX_ACK_CONTENT_BYTES) {
            ack.setContent(content);
            return;
        }
        ack.setContent(truncateUtf8(bytes));
        ack.setContentTruncated(true);
        // content_length 仅截断时输出，与 Go 的 omitempty 一致
        ack.setContentLength(bytes.length);
        LOG.warn("[Config] ack content truncated, file = {}, total = {} bytes, limit = {} bytes",
                metadata, bytes.length, MAX_ACK_CONTENT_BYTES);
    }

    private String truncateUtf8(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes, 0, MAX_ACK_CONTENT_BYTES)).toString();
        } catch (CharacterCodingException e) {
            return "";
        }
    }

    /**
     * 填充 properties。服务端以该字段有无值判断是否存在配置冲突，因此仅在真正冲突时输出：
     * 其他被监听文件存在同名 key，或生效值被其他来源覆盖（file_value 与 effective_value 不一致）。
     * 无冲突时保持 null，Gson 省略该字段。未注册 Provider 时同样省略。
     * 加密文件：先组明文数组，再用快照 AES data_key 整体加密（与 content 同一把密钥、AES-CBC/IV=key[:16]），
     * ACK.properties 输出密文字符串；无密钥或加密失败则省略该字段，永不回传明文数组。
     */
    private void fillProperties(ClientEventAck ack, ConfigFileMetadata metadata, ConfigFileSnapshot snapshot) {
        ConfigEffectiveValueProvider provider = providerRef.get();
        List<String> keys = provider == null ? null : safeGetKeys(provider, metadata);
        if (provider != null && keys != null && !keys.isEmpty()) {
            applyProperties(ack, buildPropertyEntries(provider, keys, metadata), snapshot);
        }
    }

    private void applyProperties(ClientEventAck ack, List<ResolvedEntry> resolvedEntries,
            ConfigFileSnapshot snapshot) {
        if (!resolvedEntries.isEmpty() && snapshot.isEncrypted()) {
            setEncryptedProperties(ack, toPropertyEntries(resolvedEntries), snapshot);
        } else if (!resolvedEntries.isEmpty()) {
            // 明文 ACK：entries 是跨文件采集的，来自加密文件的值不得以明文回传或进入日志
            ack.setProperties(stripEncryptedSourceValues(resolvedEntries));
        }
    }

    /**
     * 明文 ACK 前的脱敏。conflicts 与 effectiveValue 由 {@link ConfigEffectiveValueProvider} 跨全部
     * 被监听文件采集，其中可能包含加密文件的值；本文件未加密时无密钥可用，只能去掉这些值，
     * 仅保留「此处存在冲突」这一事实与来源坐标 —— 服务端定位冲突并不需要值本身。
     *
     * <p>两个维度都按「来源文件坐标」判定加密态：conflicts 自带坐标；effectiveValue 的坐标由采集侧
     * 经 {@link EffectiveValue#getSourceFile()} 给出。坐标缺失时退回保守策略，见
     * {@link #shouldStripEffectiveValue}。
     *
     * @param resolvedEntries 待回传的属性明细及其生效值来源坐标
     * @return ACK 明细列表，元素已就地脱敏
     */
    private List<ClientEventAck.PropertyEntry> stripEncryptedSourceValues(List<ResolvedEntry> resolvedEntries) {
        boolean encryptedSourcePossible = watchRegistry != null && watchRegistry.hasEncryptedWatchedFile();
        List<ClientEventAck.PropertyEntry> entries = new ArrayList<>(resolvedEntries.size());
        for (ResolvedEntry resolved : resolvedEntries) {
            ClientEventAck.PropertyEntry entry = resolved.getEntry();
            stripEncryptedConflictValues(entry);
            if (shouldStripEffectiveValue(resolved, encryptedSourcePossible)) {
                entry.setEffectiveValue(null);
            }
            entries.add(entry);
        }
        return entries;
    }

    /**
     * 是否必须去掉生效值。按采集侧的来源归因分三种处置：
     *
     * <ul>
     * <li>来源是 polaris 配置文件：按坐标精确判定，仅该文件确为加密配置才去掉，明文来源照常回传。</li>
     * <li>来源不是 polaris 配置文件（环境变量、命令行、系统属性等）：该值不由配置中心下发，
     * 不可能是加密配置的明文，照常回传。</li>
     * <li>未归因（旧版本采集侧）：无从判断，退回 fail-closed —— 只要存在加密态被监听文件，
     * 且生效值与本文件 fileValue 不一致（说明该值来自别处），就去掉。生效值等于 fileValue 时
     * 即本文件自身的明文，保留不泄露。</li>
     * </ul>
     *
     * @param resolved 属性明细及其来源归因
     * @param encryptedSourcePossible 是否存在加密态被监听文件
     * @return 需要去掉生效值返回 true
     */
    private boolean shouldStripEffectiveValue(ResolvedEntry resolved, boolean encryptedSourcePossible) {
        boolean strip;
        if (resolved.getSourceKind() == EffectiveValue.SourceKind.POLARIS_FILE) {
            strip = isEncryptedWatchedFile(resolved.getSourceFile());
        } else if (resolved.getSourceKind() == EffectiveValue.SourceKind.EXTERNAL) {
            strip = false;
        } else {
            strip = encryptedSourcePossible && isOverriddenByOtherSource(resolved.getEntry());
        }
        return strip;
    }

    /**
     * 生效值是否来自本文件之外的来源。fileValue 为 null 时任何非空生效值都来自别处。
     */
    private boolean isOverriddenByOtherSource(ClientEventAck.PropertyEntry entry) {
        String effectiveValue = entry.getEffectiveValue();
        return effectiveValue != null && !effectiveValue.equals(entry.getFileValue());
    }

    private List<ClientEventAck.PropertyEntry> toPropertyEntries(List<ResolvedEntry> resolvedEntries) {
        List<ClientEventAck.PropertyEntry> entries = new ArrayList<>(resolvedEntries.size());
        for (ResolvedEntry resolved : resolvedEntries) {
            entries.add(resolved.getEntry());
        }
        return entries;
    }

    private void stripEncryptedConflictValues(ClientEventAck.PropertyEntry entry) {
        List<ClientEventAck.ConflictEntry> conflicts = entry.getConflicts();
        if (conflicts != null) {
            for (ClientEventAck.ConflictEntry conflict : conflicts) {
                if (conflict != null && isEncryptedWatchedFile(conflict)) {
                    conflict.setValue(null);
                }
            }
        }
    }

    /**
     * 判断冲突来源文件是否为加密配置。
     */
    private boolean isEncryptedWatchedFile(ClientEventAck.ConflictEntry conflict) {
        return isEncryptedWatchedFile(new DefaultConfigFileMetadata(emptyIfNull(conflict.getNamespace()),
                emptyIfNull(conflict.getGroup()), emptyIfNull(conflict.getFileName())));
    }

    /**
     * 判断给定坐标的文件是否为加密配置。加密状态取自本地已监听文件的快照，不依赖采集侧上报。
     */
    private boolean isEncryptedWatchedFile(ConfigFileMetadata metadata) {
        ConfigFileMetadata key = new DefaultConfigFileMetadata(emptyIfNull(metadata.getNamespace()),
                emptyIfNull(metadata.getFileGroup()), emptyIfNull(metadata.getFileName()));
        RemoteConfigFileRepo repo = watchRegistry == null ? null : watchRegistry.getWatchedFile(key);
        ConfigFileSnapshot snapshot = repo == null ? null : repo.getSnapshot();
        return snapshot != null && snapshot.isEncrypted();
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private List<ResolvedEntry> buildPropertyEntries(ConfigEffectiveValueProvider provider,
            List<String> keys, ConfigFileMetadata metadata) {
        List<ResolvedEntry> entries = new ArrayList<>();
        List<String> omittedKeys = new ArrayList<>();
        int serializedBytes = 2;
        for (String key : keys) {
            if (key != null) {
                ResolvedEntry resolved = buildPropertyEntry(provider, key, metadata);
                boolean conflicted = hasConflict(resolved.getEntry());
                if (conflicted) {
                    int entryBytes = gson.toJson(resolved.getEntry()).getBytes(StandardCharsets.UTF_8).length;
                    int separatorBytes = entries.isEmpty() ? 0 : 1;
                    if (serializedBytes + separatorBytes + entryBytes > MAX_ACK_PROPERTIES_BYTES) {
                        LOG.warn("[Config] ack properties truncated, file = {}, included = {}, total = {}, limit = {} bytes",
                                metadata, entries.size(), keys.size(), MAX_ACK_PROPERTIES_BYTES);
                        break;
                    }
                    entries.add(resolved);
                    serializedBytes += separatorBytes + entryBytes;
                } else {
                    omittedKeys.add(key);
                }
            }
        }
        logPropertySelection(metadata, entries, omittedKeys);
        return entries;
    }

    private void setEncryptedProperties(ClientEventAck ack, List<ClientEventAck.PropertyEntry> entries,
            ConfigFileSnapshot snapshot) {
        if (!isAckCryptoAvailable(snapshot)) {
            return;
        }
        byte[] aesKey = decodeAckAesKey(snapshot.getDataKey());
        if (aesKey == null) {
            return;
        }
        try {
            ack.setProperties(AESUtil.encrypt(gson.toJson(entries), aesKey));
        } catch (Throwable t) {
            LOG.warn("[Config] encrypt properties failed: {}", t.getMessage());
        }
    }

    private byte[] decodeAckAesKey(String plainDataKey) {
        byte[] aesKey = null;
        if (plainDataKey != null && !plainDataKey.isEmpty()) {
            try {
                aesKey = Base64.getDecoder().decode(plainDataKey);
            } catch (RuntimeException e) {
                LOG.warn("[Config] decode aes data_key failed: {}", e.getMessage());
            }
        }
        return aesKey;
    }

    private List<String> safeGetKeys(ConfigEffectiveValueProvider provider, ConfigFileMetadata metadata) {
        try {
            List<String> keys = provider.getKeys(metadata);
            return keys == null ? null : new ArrayList<>(keys);
        } catch (RuntimeException e) {
            LOG.warn("[Config] resolve keys failed, file = {}", metadata);
            return null;
        }
    }

    private void logPropertySelection(ConfigFileMetadata metadata, List<ResolvedEntry> entries,
            List<String> omittedKeys) {
        if (LOG.isDebugEnabled()) {
            List<String> conflictedKeys = new ArrayList<>(entries.size());
            for (ResolvedEntry resolved : entries) {
                conflictedKeys.add(resolved.getEntry().getKey());
            }
            LOG.debug("[Config] ack properties, file = {}, conflicted = {}, omitted = {}",
                    metadata, conflictedKeys, omittedKeys);
        }
    }

    private boolean hasConflict(ClientEventAck.PropertyEntry entry) {
        boolean conflicted = false;
        List<ClientEventAck.ConflictEntry> conflicts = entry.getConflicts();
        if (conflicts != null && !conflicts.isEmpty()) {
            conflicted = true;
        } else {
            String fileValue = entry.getFileValue();
            String effectiveValue = entry.getEffectiveValue();
            if (fileValue == null) {
                conflicted = effectiveValue != null;
            } else {
                conflicted = !fileValue.equals(effectiveValue);
            }
        }
        return conflicted;
    }

    private ResolvedEntry buildPropertyEntry(ConfigEffectiveValueProvider provider, String key,
            ConfigFileMetadata metadata) {
        ClientEventAck.PropertyEntry entry = new ClientEventAck.PropertyEntry();
        entry.setKey(key);
        EffectiveValue resolved = fillEffectiveValue(provider, key, metadata, entry);
        entry.setConflicts(buildConflicts(provider, key, metadata));
        return new ResolvedEntry(entry, resolved);
    }

    /**
     * 填充生效值三字段，并返回采集侧结果（含来源归因），采集失败时为 null。
     */
    private EffectiveValue fillEffectiveValue(ConfigEffectiveValueProvider provider, String key,
            ConfigFileMetadata metadata, ClientEventAck.PropertyEntry entry) {
        EffectiveValue effectiveValue;
        try {
            effectiveValue = provider.resolve(key, metadata);
        } catch (RuntimeException e) {
            LOG.warn("[Config] resolve effective value failed, key = {}", key);
            effectiveValue = null;
        }
        if (effectiveValue != null) {
            entry.setPropertySource(effectiveValue.getPropertySource());
            entry.setFileValue(effectiveValue.getFileValue());
            entry.setEffectiveValue(effectiveValue.getEffectiveValue());
        }
        return effectiveValue;
    }

    private List<ClientEventAck.ConflictEntry> buildConflicts(ConfigEffectiveValueProvider provider, String key,
            ConfigFileMetadata metadata) {
        List<ConfigKeyConflict> conflicts;
        try {
            List<ConfigKeyConflict> resolvedConflicts = provider.resolveConflicts(key, metadata);
            conflicts = resolvedConflicts == null ? null : new ArrayList<>(resolvedConflicts);
        } catch (RuntimeException e) {
            LOG.warn("[Config] resolve conflicts failed, key = {}", key);
            conflicts = null;
        }
        List<ClientEventAck.ConflictEntry> entries = new ArrayList<>();
        if (conflicts == null) {
            return entries;
        }
        for (ConfigKeyConflict conflict : conflicts) {
            if (conflict != null) {
                entries.add(toConflictEntry(conflict));
            }
        }
        return entries;
    }

    private ClientEventAck.ConflictEntry toConflictEntry(ConfigKeyConflict conflict) {
        ClientEventAck.ConflictEntry entry = new ClientEventAck.ConflictEntry();
        entry.setNamespace(conflict.getNamespace());
        entry.setGroup(conflict.getGroup());
        entry.setFileName(conflict.getFileName());
        entry.setValue(conflict.getValue());
        return entry;
    }

    private ClientEventAck newAck(String kind, ClientEventQuery.QueryConfig cfg, String reason) {
        ClientEventAck ack = new ClientEventAck();
        ack.setKind(kind == null ? "" : kind);
        ack.setConfig(toAckConfig(cfg));
        ack.setApplied(false);
        ack.setReason(reason);
        // content 无 omitempty：未命中也输出空串，供服务端区分"内容为空"与"未返回内容"
        ack.setContent("");
        return ack;
    }

    private ClientEventAck.AckConfig toAckConfig(ClientEventQuery.QueryConfig cfg) {
        // 对齐 Go 零值回带:config 三元组无 omitempty,缺省输出空串而非省略字段
        ClientEventAck.AckConfig ackConfig = new ClientEventAck.AckConfig();
        ackConfig.setNamespace(namespaceOf(cfg));
        ackConfig.setGroup(groupOf(cfg));
        ackConfig.setFileName(fileNameOf(cfg));
        return ackConfig;
    }

    private String marshalAck(ClientEventAck ack) {
        try {
            return gson.toJson(ack);
        } catch (RuntimeException e) {
            LOG.warn("[Config] marshal ack content failed: {}", e.getMessage());
            return MARSHAL_FAILED_ACK;
        }
    }

    /**
     * 采集结果的内部载体：ACK 明细 + 生效值来源的文件坐标。
     *
     * <p>坐标只用于客户端内部判定来源文件是否加密，**不放在**
     * {@link ClientEventAck.PropertyEntry} 上，以免随 ACK 序列化外泄，或让上报报文多出一个
     * 隐式协议字段。
     */
    private static final class ResolvedEntry {

        private final ClientEventAck.PropertyEntry entry;

        private final ConfigFileMetadata sourceFile;

        private final EffectiveValue.SourceKind sourceKind;

        ResolvedEntry(ClientEventAck.PropertyEntry entry, EffectiveValue resolved) {
            this.entry = entry;
            this.sourceFile = resolved == null ? null : resolved.getSourceFile();
            this.sourceKind = resolved == null ? EffectiveValue.SourceKind.UNKNOWN : resolved.getSourceKind();
        }

        ClientEventAck.PropertyEntry getEntry() {
            return entry;
        }

        ConfigFileMetadata getSourceFile() {
            return sourceFile;
        }

        EffectiveValue.SourceKind getSourceKind() {
            return sourceKind;
        }
    }
}
