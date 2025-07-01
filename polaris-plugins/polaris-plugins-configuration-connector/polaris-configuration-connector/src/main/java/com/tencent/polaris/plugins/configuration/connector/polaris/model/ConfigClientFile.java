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

package com.tencent.polaris.plugins.configuration.connector.polaris.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

/**
 * @author fabian4 2023-03-02
 */
public class ConfigClientFile {

    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("namespace")
    private String namespace;
    @SerializedName("group")
    private String group;
    @SerializedName("content")
    private String content;
    @SerializedName("format")
    private String format;
    @SerializedName("comment")
    private String comment;
    @SerializedName("status")
    private String status;
    @SerializedName("tags")
    private List<String> tags;
    @SerializedName("createTime")
    private Date createTime;
    @SerializedName("createBy")
    private String createBy;
    @SerializedName("modifyTime")
    private Date modifyTime;
    @SerializedName("modifyBy")
    private String modifyBy;
    @SerializedName("releaseTime")
    private Date releaseTime;
    @SerializedName("releaseBy")
    private String releaseBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
    }

    public String getModifyBy() {
        return modifyBy;
    }

    public void setModifyBy(String modifyBy) {
        this.modifyBy = modifyBy;
    }

    public Date getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime) {
        this.releaseTime = releaseTime;
    }

    public String getReleaseBy() {
        return releaseBy;
    }

    public void setReleaseBy(String releaseBy) {
        this.releaseBy = releaseBy;
    }

    @Override
    public String toString() {
        return "ConfigFile{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", group='" + group + '\'' +
                ", content='" + content + '\'' +
                ", format='" + format + '\'' +
                ", comment='" + comment + '\'' +
                ", status='" + status + '\'' +
                ", tags=" + tags +
                ", createTime=" + createTime +
                ", createBy='" + createBy + '\'' +
                ", modifyTime=" + modifyTime +
                ", modifyBy='" + modifyBy + '\'' +
                ", releaseTime=" + releaseTime +
                ", releaseBy='" + releaseBy + '\'' +
                '}';
    }
}
