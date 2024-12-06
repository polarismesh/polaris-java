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

package com.tencent.polaris.api.plugin.configuration;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.rpc.BaseEntity;
import com.tencent.polaris.specification.api.v1.config.manage.ConfigFileProto;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author lepdou 2022-03-02
 */
public class ConfigPublishFile extends BaseEntity {

    //
    private String namespace;

    //
    private String fileGroup;

    //
    private String fileName;

    //
    private String releaseName;

    //
    private String content;

    //
    private String md5;

    private Map<String, String> labels;

    public String getReleaseName() {
        return releaseName;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getFileGroup() {
        return fileGroup;
    }

    public void setFileGroup(String fileGroup) {
        this.fileGroup = fileGroup;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "ConfigPublishFile{" +
                "namespace='" + namespace + '\'' +
                ", fileGroup='" + fileGroup + '\'' +
                ", fileName='" + fileName + '\'' +
                ", releaseName='" + releaseName + '\'' +
                ", content='" + content + '\'' +
                ", md5='" + md5 + '\'' +
                ", labels=" + labels +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigPublishFile that = (ConfigPublishFile) o;
        return Objects.equals(namespace, that.namespace) &&
                Objects.equals(fileGroup, that.fileGroup) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(releaseName, that.releaseName) &&
                Objects.equals(content, that.content) &&
                Objects.equals(md5, that.md5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                namespace,
                fileGroup,
                fileName,
                releaseName,
                content,
                md5);
    }

    public ConfigFileProto.ConfigFilePublishInfo toSpec() {
        return ConfigFileProto.ConfigFilePublishInfo.newBuilder()
                .setNamespace(StringValue.newBuilder().setValue(namespace).build())
                .setGroup(StringValue.newBuilder().setValue(fileGroup).build())
                .setFileName(StringValue.newBuilder().setValue(fileName).build())
                .setReleaseName(StringValue.newBuilder().setValue(releaseName).build())
                .setContent(StringValue.newBuilder().setValue(content).build())
                .addAllTags(labels.entrySet().stream().map(entry -> ConfigFileProto.ConfigFileTag.newBuilder()
                        .setKey(StringValue.newBuilder().setValue(entry.getKey()).build())
                        .setValue(StringValue.newBuilder().setValue(entry.getValue()).build())
                        .build()).collect(Collectors.toList()))
                .setMd5(StringValue.newBuilder().setValue(md5).build())
                .build();
    }
}
