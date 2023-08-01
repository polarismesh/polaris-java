package com.tencent.polaris.api.plugin.configuration;

import java.util.List;

public class ConfigFileGroup extends ConfigFileGroupMetadata {
    private List<ConfigFile> configFileList;

    public List<ConfigFile> getConfigFileList() {
        return configFileList;
    }

    public void setConfigFileList(List<ConfigFile> configFileList) {
        this.configFileList = configFileList;
    }
}
