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
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueProvider;
import com.tencent.polaris.configuration.api.core.ConfigEffectiveValueRegistration;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKeyConflict;
import com.tencent.polaris.configuration.api.core.EffectiveValue;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 配置生效查询处理器，解析服务端 PUSH 指令并组装 ACK content JSON。
 * <p>
 * 任何分支都必须返回可发送的 JSON：服务端同步等待 ACK，静默会把它挂到超时。
 * 日志只记文件坐标、applied、reason、耗时与字节数，绝不记录配置值（含 DEBUG）。
 *
 * @author evelynwei
 */
public class ClientEventQueryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ClientEventQueryHandler.class);

    /**
     * ACK 中 content 的最大字节数，超限截断，避免触发 gRPC 服务端默认 4MB 消息体上限。
     */
    private static final int MAX_ACK_CONTENT_BYTES = 512 * 1024;

    private static final String KIND_CONFIG = "config";

    private static final String REASON_BAD_CONTENT = "bad_content";

    private static final String REASON_UNKNOWN_KIND = "unknown_kind";

    private static final String REASON_CONFIG_DISABLED = "config_disabled";

    private static final String REASON_NOT_WATCHED = "not_watched";

    /** 已监听该配置文件但尚未拉取生效（首次拉取失败/重试中）。 */
    private static final String REASON_PENDING = "pending";

    /**
     * 序列化 ACK 自身失败时的兜底应答，避免服务端收到无法诊断的空对象。
     */
    private static final String MARSHAL_FAILED_ACK = "{\"applied\":false,\"reason\":\"marshal_failed\"}";

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
        ClientEventQuery query = parseQuery(pushContent);
        if (query == null) {
            return marshalAck(newAck(null, null, REASON_BAD_CONTENT));
        }
        ClientEventQuery.QueryConfig cfg = query.getConfig();
        if (!KIND_CONFIG.equals(query.getKind())) {
            return marshalAck(newAck(query.getKind(), cfg, REASON_UNKNOWN_KIND));
        }
        if (watchRegistry == null) {
            return marshalAck(newAck(query.getKind(), cfg, REASON_CONFIG_DISABLED));
        }
        return handleConfigQuery(cfg);
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

    private String handleConfigQuery(ClientEventQuery.QueryConfig cfg) {
        // null 一律归一为 ""，对齐 Go 零值查询：构造不出合法坐标时查不到 → not_watched
        ConfigFileMetadata metadata = new DefaultConfigFileMetadata(namespaceOf(cfg), groupOf(cfg), fileNameOf(cfg));
        RemoteConfigFileRepo repo = watchRegistry.getWatchedFile(metadata);
        if (repo == null) {
            return marshalAck(newAck(KIND_CONFIG, cfg, REASON_NOT_WATCHED));
        }
        if (!repo.isPulled()) {
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
        ConfigFileSnapshot snapshot = repo.getSnapshot();
        fillSnapshot(ack, snapshot);
        fillContent(ack, snapshot, metadata);
        fillProperties(ack, metadata);
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

    private void fillSnapshot(ClientEventAck ack, ConfigFileSnapshot snapshot) {
        // 对齐 Go 的 omitempty:version/md5/effective_time 零值时省略字段
        if (snapshot.getVersion() > 0) {
            ack.setVersion(snapshot.getVersion());
        }
        if (snapshot.getMd5() != null && !snapshot.getMd5().isEmpty()) {
            ack.setMd5(snapshot.getMd5());
        }
        if (snapshot.getEffectiveTime() > 0) {
            ack.setEffectiveTime(snapshot.getEffectiveTime());
        }
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
     * 填充 properties[]。未注册 Provider 时保持 null，Gson 省略该字段，ACK 退化为与 Go 一致的形态。
     * 单个 key 解析失败只降级该 key，不影响整体 applied=true。
     */
    private void fillProperties(ClientEventAck ack, ConfigFileMetadata metadata) {
        ConfigEffectiveValueProvider provider = providerRef.get();
        if (provider == null) {
            return;
        }
        List<String> keys = safeGetKeys(provider, metadata);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        List<ClientEventAck.PropertyEntry> entries = new ArrayList<>(keys.size());
        for (String key : keys) {
            entries.add(buildPropertyEntry(provider, key, metadata));
        }
        ack.setProperties(entries);
    }

    private List<String> safeGetKeys(ConfigEffectiveValueProvider provider, ConfigFileMetadata metadata) {
        try {
            return provider.getKeys(metadata);
        } catch (RuntimeException e) {
            LOG.warn("[Config] resolve keys failed, file = {}", metadata);
            return null;
        }
    }

    private ClientEventAck.PropertyEntry buildPropertyEntry(ConfigEffectiveValueProvider provider, String key,
            ConfigFileMetadata metadata) {
        ClientEventAck.PropertyEntry entry = new ClientEventAck.PropertyEntry();
        entry.setKey(key);
        fillEffectiveValue(provider, key, metadata, entry);
        entry.setConflicts(buildConflicts(provider, key, metadata));
        return entry;
    }

    private void fillEffectiveValue(ConfigEffectiveValueProvider provider, String key, ConfigFileMetadata metadata,
            ClientEventAck.PropertyEntry entry) {
        EffectiveValue effectiveValue;
        try {
            effectiveValue = provider.resolve(key, metadata);
        } catch (RuntimeException e) {
            LOG.warn("[Config] resolve effective value failed, key = {}", key);
            effectiveValue = null;
        }
        if (effectiveValue != null) {
            entry.setFileValue(effectiveValue.getFileValue());
            entry.setEffectiveValue(effectiveValue.getEffectiveValue());
            entry.setPropertySource(effectiveValue.getPropertySource());
        }
    }

    private List<ClientEventAck.ConflictEntry> buildConflicts(ConfigEffectiveValueProvider provider, String key,
            ConfigFileMetadata metadata) {
        List<ConfigKeyConflict> conflicts;
        try {
            conflicts = provider.resolveConflicts(key, metadata);
        } catch (RuntimeException e) {
            LOG.warn("[Config] resolve conflicts failed, key = {}", key);
            conflicts = null;
        }
        List<ClientEventAck.ConflictEntry> entries = new ArrayList<>();
        if (conflicts == null) {
            return entries;
        }
        for (ConfigKeyConflict conflict : conflicts) {
            entries.add(toConflictEntry(conflict));
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
}
