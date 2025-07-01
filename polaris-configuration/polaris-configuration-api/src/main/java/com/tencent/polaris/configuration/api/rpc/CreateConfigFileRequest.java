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

package com.tencent.polaris.configuration.api.rpc;

import com.tencent.polaris.api.utils.StringUtils;

import java.util.Map;

public class CreateConfigFileRequest {

    private String namespace;

    private String group;

    private String filename;

    private String content;

    private Map<String, String> labels;

    public CreateConfigFileRequest() {
    }

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

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public void verify() {
        if (StringUtils.isBlank(getNamespace())) {
            throw new IllegalArgumentException("namespace cannot be empty.");
        }
        if (StringUtils.isBlank(getGroup())) {
            throw new IllegalArgumentException("file group cannot be empty.");
        }
        if (StringUtils.isBlank(getFilename())) {
            throw new IllegalArgumentException("file name cannot be empty.");
        }
    }

    public static final class Builder {
        private String namespace;
        private String group;
        private String filename;
        private String content;
        private Map<String, String> labels;

        private Builder() {
        }

        public static Builder aCreateConfigFileRequest() {
            return new Builder();
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

        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public CreateConfigFileRequest build() {
            CreateConfigFileRequest createConfigFileRequest = new CreateConfigFileRequest();
            createConfigFileRequest.setNamespace(namespace);
            createConfigFileRequest.setGroup(group);
            createConfigFileRequest.setFilename(filename);
            createConfigFileRequest.setContent(content);
            createConfigFileRequest.setLabels(labels);
            return createConfigFileRequest;
        }
    }
}
