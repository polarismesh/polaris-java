package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;

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
}
