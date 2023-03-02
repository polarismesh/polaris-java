package com.tencent.polaris.plugins.connector.openapi.model;

import java.util.Date;
import java.util.List;

public class ConfigClientFile {

    private String id;
    private String name;
    private String namespace;
    private String group;
    private String content;
    private String format;
    private String comment;
    private String status;
    private List<String> tags;
    private Date createTime;
    private String createBy;
    private Date modifyTime;
    private String modifyBy;
    private Date releaseTime;
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
