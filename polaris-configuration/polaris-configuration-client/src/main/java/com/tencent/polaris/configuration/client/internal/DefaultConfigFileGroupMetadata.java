package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;

import java.util.Objects;

public class DefaultConfigFileGroupMetadata implements ConfigFileGroupMetadata {
    private final String namespace;
    private final String fileGroupName;

    public DefaultConfigFileGroupMetadata(String namespace, String fileGroupName) {
        this.namespace = namespace;
        this.fileGroupName = fileGroupName;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getFileGroupName() {
        return fileGroupName;
    }

    @Override
    public String toString() {
        return "DefaultConfigFileGroupMetadata{" +
                "namespace='" + namespace + '\'' +
                ", fileGroupName='" + fileGroupName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultConfigFileGroupMetadata that = (DefaultConfigFileGroupMetadata) o;
        return Objects.equals(namespace, that.namespace) && Objects.equals(fileGroupName, that.fileGroupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, fileGroupName);
    }
}
