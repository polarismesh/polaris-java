package com.tencent.polaris.api.plugin.configuration;

public class ConfigFileGroupMetadata {
    private String namespace;
    private String fileGroupName;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getFileGroupName() {
        return fileGroupName;
    }

    public void setFileGroupName(String fileGroupName) {
        this.fileGroupName = fileGroupName;
    }
}
