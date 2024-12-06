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

package com.tencent.polaris.configuration.api.rpc;

import com.tencent.polaris.api.rpc.BaseEntity;

import java.util.HashMap;
import java.util.Map;

public class ConfigPublishRequest {

    private String namespace;

    private String group;

    private String filename;

    private String content;

    private String casMd5 = "";

    private String releaseName = "";

    private Map<String, String> labels = new HashMap<>();

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCasMd5() {
        return casMd5;
    }

    public void setCasMd5(String casMd5) {
        this.casMd5 = casMd5;
    }

    public String getReleaseName() {
        return releaseName;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public void verify() {

    }

    @Override
    public String toString() {
        return "ConfigPublishRequest{" +
                "namespace='" + namespace + '\'' +
                ", group='" + group + '\'' +
                ", filename='" + filename + '\'' +
                ", content='" + content + '\'' +
                ", casMd5='" + casMd5 + '\'' +
                ", releaseName='" + releaseName + '\'' +
                ", labels=" + labels +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String namespace;
        private String group;
        private String filename;
        private String content;
        private String casMd5;
        private String releaseName;
        private Map<String, String> labels;

        private Builder() {
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder casMd5(String casMd5) {
            this.casMd5 = casMd5;
            return this;
        }

        public Builder releaseName(String releaseName) {
            this.releaseName = releaseName;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public ConfigPublishRequest build() {
            ConfigPublishRequest configPublishRequest = new ConfigPublishRequest();
            configPublishRequest.setNamespace(namespace);
            configPublishRequest.setGroup(group);
            configPublishRequest.setFilename(filename);
            configPublishRequest.setContent(content);
            configPublishRequest.setCasMd5(casMd5);
            configPublishRequest.setReleaseName(releaseName);
            configPublishRequest.setLabels(labels);
            return configPublishRequest;
        }
    }
}
