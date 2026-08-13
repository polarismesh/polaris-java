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

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 客户端配置生效查询 ACK 应答，snake_case 与服务端配置中心一致。
 * <p>
 * 字段输出规则对齐 Go 端 client_event_ack：null 字段被省略（模拟 omitempty），
 * content 始终输出（未命中时为空串，供服务端区分"内容为空"与"未返回内容"）。
 *
 * @author evelynwei
 */
class ClientEventAck {

    @SerializedName("kind")
    private String kind;

    @SerializedName("config")
    private AckConfig config;

    @SerializedName("version")
    private Long version;

    @SerializedName("md5")
    private String md5;

    @SerializedName("effective_time")
    private Long effectiveTime;

    @SerializedName("content")
    private String content;

    @SerializedName("content_truncated")
    private Boolean contentTruncated;

    @SerializedName("content_length")
    private Integer contentLength;

    @SerializedName("applied")
    private boolean applied;

    @SerializedName("reason")
    private String reason;

    @SerializedName("properties")
    private List<PropertyEntry> properties;

    String getKind() {
        return kind;
    }

    void setKind(String kind) {
        this.kind = kind;
    }

    AckConfig getConfig() {
        return config;
    }

    void setConfig(AckConfig config) {
        this.config = config;
    }

    Long getVersion() {
        return version;
    }

    void setVersion(Long version) {
        this.version = version;
    }

    String getMd5() {
        return md5;
    }

    void setMd5(String md5) {
        this.md5 = md5;
    }

    Long getEffectiveTime() {
        return effectiveTime;
    }

    void setEffectiveTime(Long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    String getContent() {
        return content;
    }

    void setContent(String content) {
        this.content = content;
    }

    Boolean getContentTruncated() {
        return contentTruncated;
    }

    void setContentTruncated(Boolean contentTruncated) {
        this.contentTruncated = contentTruncated;
    }

    Integer getContentLength() {
        return contentLength;
    }

    void setContentLength(Integer contentLength) {
        this.contentLength = contentLength;
    }

    boolean isApplied() {
        return applied;
    }

    void setApplied(boolean applied) {
        this.applied = applied;
    }

    String getReason() {
        return reason;
    }

    void setReason(String reason) {
        this.reason = reason;
    }

    List<PropertyEntry> getProperties() {
        return properties;
    }

    void setProperties(List<PropertyEntry> properties) {
        this.properties = properties;
    }

    /**
     * ACK 中回带的配置文件三元组。
     */
    static class AckConfig {

        @SerializedName("namespace")
        private String namespace;

        @SerializedName("group")
        private String group;

        @SerializedName("file_name")
        private String fileName;

        String getNamespace() {
            return namespace;
        }

        void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        String getGroup() {
            return group;
        }

        void setGroup(String group) {
            this.group = group;
        }

        String getFileName() {
            return fileName;
        }

        void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }

    /**
     * 单个 key 的生效详情，对应服务端 properties[] 数组元素。
     */
    static class PropertyEntry {

        @SerializedName("key")
        private String key;

        @SerializedName("file_value")
        private String fileValue;

        @SerializedName("effective_value")
        private String effectiveValue;

        @SerializedName("property_source")
        private String propertySource;

        @SerializedName("conflicts")
        private List<ConflictEntry> conflicts;

        String getKey() {
            return key;
        }

        void setKey(String key) {
            this.key = key;
        }

        String getFileValue() {
            return fileValue;
        }

        void setFileValue(String fileValue) {
            this.fileValue = fileValue;
        }

        String getEffectiveValue() {
            return effectiveValue;
        }

        void setEffectiveValue(String effectiveValue) {
            this.effectiveValue = effectiveValue;
        }

        String getPropertySource() {
            return propertySource;
        }

        void setPropertySource(String propertySource) {
            this.propertySource = propertySource;
        }

        List<ConflictEntry> getConflicts() {
            return conflicts;
        }

        void setConflicts(List<ConflictEntry> conflicts) {
            this.conflicts = conflicts;
        }
    }

    /**
     * 同名 key 冲突项，对应服务端 properties[].conflicts[] 数组元素。
     */
    static class ConflictEntry {

        @SerializedName("namespace")
        private String namespace;

        @SerializedName("group")
        private String group;

        @SerializedName("file_name")
        private String fileName;

        @SerializedName("value")
        private String value;

        String getNamespace() {
            return namespace;
        }

        void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        String getGroup() {
            return group;
        }

        void setGroup(String group) {
            this.group = group;
        }

        String getFileName() {
            return fileName;
        }

        void setFileName(String fileName) {
            this.fileName = fileName;
        }

        String getValue() {
            return value;
        }

        void setValue(String value) {
            this.value = value;
        }
    }
}
