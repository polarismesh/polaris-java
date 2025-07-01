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

package com.tencent.polaris.api.plugin.event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 配置事件。
 */
public class ConfigEvent implements BaseEvent {

    /**
     * 事件类型
     */
    @JsonProperty("event_type")
    private final EventConstants.EventType eventType;

    /**
     * 事件名称
     */
    @JsonProperty("event_name")
    private final EventConstants.EventName eventName;

    /**
     * 时间戳
     */
    @JsonProperty("event_time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss:SSSS")
    private final LocalDateTime timestamp;

    /**
     * 客户端ID
     */
    @JsonProperty("client_id")
    private final String clientId;

    /**
     * 客户端IP
     */
    @JsonProperty("client_ip")
    private final String clientIp;

    /**
     * 被调命名空间
     */
    @JsonProperty("namespace")
    private final String namespace;

    @JsonProperty("config_group")
    private final String configGroup;

    @JsonProperty("config_file_name")
    private final String configFileName;

    @JsonProperty("config_file_version")
    private final String configFileVersion;
    /**
     * 当前事件状态
     */
    private final EventConstants.Status currentStatus;

    /**
     * 上一次事件状态
     */
    private final EventConstants.Status previousStatus;

    /**
     * 资源类型
     */
    private final EventConstants.ResourceType resourceType;

    /**
     * 状态转换原因
     */
    @JsonProperty("reason")
    private final String reason;

    private final Map<String, String> additionalParams;

    private ConfigEvent(Builder builder) {
        this.eventType = builder.eventType;
        this.eventName = builder.eventName;
        this.timestamp = builder.timestamp;
        this.clientId = builder.clientId;
        this.clientIp = builder.clientIp;
        this.namespace = builder.namespace;
        this.configGroup = builder.configGroup;
        this.configFileName = builder.configFileName;
        this.configFileVersion = builder.configVersion;
        this.currentStatus = builder.currentStatus;
        this.previousStatus = builder.previousStatus;
        this.resourceType = builder.resourceType;
        this.reason = builder.reason;
        additionalParams = new HashMap<>();
    }

    public static class Builder {
        private EventConstants.EventType eventType;
        private EventConstants.EventName eventName;
        private LocalDateTime timestamp;
        private String clientId = "";
        private String clientIp = "";
        private String namespace = "";
        private String configGroup = "";
        private String configFileName = "";
        private String configVersion = "";
        private EventConstants.Status currentStatus;
        private EventConstants.Status previousStatus;
        private EventConstants.ResourceType resourceType;
        private String reason = "";

        public Builder withEventType(EventConstants.EventType eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder withEventName(EventConstants.EventName eventName) {
            this.eventName = eventName;
            return this;
        }

        public Builder withTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder withClientIp(String clientIp) {
            this.clientIp = clientIp;
            return this;
        }

        public Builder withNamespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder withConfigGroup(String configGroup) {
            this.configGroup = configGroup;
            return this;
        }

        public Builder withConfigFileName(String configFileName) {
            this.configFileName = configFileName;
            return this;
        }

        public Builder withConfigVersion(String configVersion) {
            this.configVersion = configVersion;
            return this;
        }

        public Builder withCurrentStatus(EventConstants.Status currentStatus) {
            this.currentStatus = currentStatus;
            return this;
        }

        public Builder withPreviousStatus(EventConstants.Status previousStatus) {
            this.previousStatus = previousStatus;
            return this;
        }

        public Builder withResourceType(EventConstants.ResourceType resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder withReason(String reason) {
            this.reason = reason;
            return this;
        }

        public ConfigEvent build() {
            return new ConfigEvent(this);
        }
    }

    public EventConstants.EventType getEventType() {
        return eventType;
    }

    public EventConstants.EventName getEventName() {
        return eventName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public String getConfigFileName() {
        return configFileName;
    }

    public String getConfigFileVersion() {
        return configFileVersion;
    }

    public EventConstants.Status getCurrentStatus() {
        return currentStatus;
    }

    public EventConstants.Status getPreviousStatus() {
        return previousStatus;
    }

    public EventConstants.ResourceType getResourceType() {
        return resourceType;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String convertMessage() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault());
        String formattedDateTime = "";
        if (this.getTimestamp() != null) {
            formattedDateTime = formatter.format(this.getTimestamp());
        }

        String eventType = "";
        if (this.getEventType() != null) {
            eventType = this.getEventType().name();
        }

        String eventName = "";
        if (this.getEventName() != null) {
            eventName = this.getEventName().name();
        }

        return eventType + "|" + eventName + "|" + formattedDateTime + "|" + this.getClientId() + "|"
                + this.getClientIp() + "|" + this.getNamespace() + "|" + this.getConfigGroup() + "|"
                + this.getConfigFileName() + "|" + this.getConfigFileVersion();
    }

    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }

    @Override
    public String toString() {
        return "ConfigEvent{" +
                "eventType=" + eventType +
                ", eventName=" + eventName +
                ", timestamp=" + timestamp +
                ", clientId='" + clientId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                ", namespace='" + namespace + '\'' +
                ", configGroup='" + configGroup + '\'' +
                ", configFileName='" + configFileName + '\'' +
                ", configFileVersion='" + configFileVersion + '\'' +
                ", currentStatus=" + currentStatus +
                ", previousStatus=" + previousStatus +
                ", resourceType=" + resourceType +
                ", reason='" + reason + '\'' +
                ", additionalParams=" + additionalParams +
                '}';
    }
}