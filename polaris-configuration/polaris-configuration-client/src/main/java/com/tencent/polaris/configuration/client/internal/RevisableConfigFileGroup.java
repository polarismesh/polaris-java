package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileGroup;

public class RevisableConfigFileGroup {
    private ConfigFileGroup configFileGroup;
    private String revision;

    public RevisableConfigFileGroup(ConfigFileGroup configFileGroup) {
        this(configFileGroup, "");
    }

    public RevisableConfigFileGroup(ConfigFileGroup configFileGroup, String revision) {
        this.configFileGroup = configFileGroup;
        this.revision = revision;
    }

    public ConfigFileGroup getConfigFileGroup() {
        return configFileGroup;
    }

    public void setConfigFileGroup(ConfigFileGroup configFileGroup) {
        this.configFileGroup = configFileGroup;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
