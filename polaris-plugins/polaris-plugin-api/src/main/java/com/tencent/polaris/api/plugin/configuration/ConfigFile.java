package com.tencent.polaris.api.plugin.configuration;

import java.util.Objects;

/**
 * @author lepdou 2022-03-02
 */
public class ConfigFile {

    private String namespace;
    private String fileGroup;
    private String fileName;
    private String content;
    private long   version;
    private String md5;

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
               Objects.equals(md5, that.md5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, fileGroup, fileName, content, version, md5);
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
               '}';
    }
}
