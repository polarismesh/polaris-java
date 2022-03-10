package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import java.util.Objects;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileMetadata implements ConfigFileMetadata {

    private final String namespace;
    private final String fileGroup;
    private final String fileName;

    public DefaultConfigFileMetadata(String namespace, String fileGroup, String fileName) {
        this.namespace = namespace;
        this.fileGroup = fileGroup;
        this.fileName = fileName;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getFileGroup() {
        return fileGroup;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, fileGroup, fileName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultConfigFileMetadata that = (DefaultConfigFileMetadata) o;
        return namespace.equals(that.namespace) &&
               fileGroup.equals(that.fileGroup) &&
               fileName.equals(that.fileName);
    }

    @Override
    public String toString() {
        return "ConfigFile{" +
               "namespace='" + namespace + '\'' +
               ", fileGroup='" + fileGroup + '\'' +
               ", fileName='" + fileName + '\'' +
               '}';
    }
}
