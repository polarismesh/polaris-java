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
    /**
     * 配置源内容（加密配置为密文）。非加密配置该字段为空，源内容即 content。
     * 加密 filter 解密前保留原文到此字段，供配置生效查询 ACK 回传（与 md5 源内容摘要自洽，不回传解密明文）。
     */
    private String sourceContent;
    private long version;
    private String name;
    private String md5;
    private String publicKey;
    private String dataKey;
    private String encryptAlgo;
    private boolean encrypted = Boolean.FALSE;
    /**
     * 标记本对象的 content 是否为「本地缓存密文态」。
     *
     * <p>仅在本地缓存读写链路中有意义：写入时由持久化副本置为 true（表示 content 已是密文，
     * dataKey 为其 Base64 AES 密钥）；读取时依据该标记决定是否解密。
     * 内存中的业务对象该字段恒为 false。历史缓存文件无此字段，反序列化后为 false，按明文处理。
     *
     * <p>与 encrypted 的区别：encrypted 表示该配置在服务端是否为加密配置（业务语义，来自服务端）；
     * cacheEncrypted 表示本地缓存文件里的 content 当前是否为密文（存储语义，本地生成）。
     */
    private boolean cacheEncrypted = Boolean.FALSE;
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

    public String getSourceContent() {
        return sourceContent;
    }

    public void setSourceContent(String sourceContent) {
        this.sourceContent = sourceContent;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public boolean isCacheEncrypted() {
        return cacheEncrypted;
    }

    public void setCacheEncrypted(boolean cacheEncrypted) {
        this.cacheEncrypted = cacheEncrypted;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }

    public String getEncryptAlgo() {
        return encryptAlgo;
    }

    public void setEncryptAlgo(String encryptAlgo) {
        this.encryptAlgo = encryptAlgo;
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

    /**
     * 不输出 content 与 sourceContent：加密配置的正文不得进入日志文件。
     * md5 为源内容摘要，不可逆，保留以便问题定位。
     *
     * @return 仅含元数据的字符串
     */
    @Override
    public String toString() {
        return "ConfigFile{" +
               "namespace='" + namespace + '\'' +
               ", fileGroup='" + fileGroup + '\'' +
               ", fileName='" + fileName + '\'' +
               ", version=" + version +
                ", name=" + name +
               ", md5='" + md5 + '\'' +
               ", encrypted=" + encrypted +
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
