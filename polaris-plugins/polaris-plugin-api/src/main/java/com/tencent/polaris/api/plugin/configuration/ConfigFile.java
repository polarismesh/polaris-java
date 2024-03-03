/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt64Value;
import com.tencent.polaris.api.rpc.BaseEntity;
import com.tencent.polaris.specification.api.v1.config.manage.ConfigFileProto;

import java.util.Date;
import java.util.Objects;

/**
 * @author lepdou 2022-03-02
 */
public class ConfigFile extends BaseEntity {

    private String namespace;
    private String fileGroup;
    private String fileName;
    private String content;
    private long version;
    private String md5;
    private String publicKey;
    private String dataKey;
    private boolean encrypted = Boolean.FALSE;
    private Date releaseTime;

    public ConfigFile(String namespace, String fileGroup, String fileName) {
        this.namespace = namespace;
        this.fileGroup = fileGroup;
        this.fileName = fileName;
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigFile that = (ConfigFile) o;
        return version == that.version &&
               namespace.equals(that.namespace) &&
               fileGroup.equals(that.fileGroup) &&
               fileName.equals(that.fileName) &&
               Objects.equals(content, that.content) &&
               Objects.equals(md5, that.md5) &&
               Objects.equals(releaseTime, that.releaseTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, fileGroup, fileName, content, version, md5, releaseTime);
    }

    @Override
    public String toString() {
        return "ConfigFile{" +
               "namespace='" + namespace + '\'' +
               ", fileGroup='" + fileGroup + '\'' +
               ", fileName='" + fileName + '\'' +
               ", content='" + content + '\'' +
               ", version=" + version +
               ", md5='" + md5 + '\'' +
               ", releaseTime=" + releaseTime + '\'' +
               '}';
    }

    public ConfigFileProto.ClientConfigFileInfo toClientConfigFileInfo() {
        ConfigFileProto.ClientConfigFileInfo.Builder builder = ConfigFileProto.ClientConfigFileInfo.newBuilder();

        builder.setNamespace(StringValue.newBuilder().setValue(getNamespace()).build());
        builder.setGroup(StringValue.newBuilder().setValue(getFileGroup()).build());
        builder.setFileName(StringValue.newBuilder().setValue(getFileName()).build());
        builder.setVersion(UInt64Value.newBuilder().setValue(getVersion()).build());
        if (isEncrypted()) {
            builder.setEncrypted(BoolValue.newBuilder().setValue(isEncrypted()).buildPartial());
            builder.setPublicKey(StringValue.newBuilder().setValue(getPublicKey()).build());
        }

        return builder.build();
    }
}
