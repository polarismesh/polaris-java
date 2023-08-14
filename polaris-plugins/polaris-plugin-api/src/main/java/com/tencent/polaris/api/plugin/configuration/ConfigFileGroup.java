package com.tencent.polaris.api.plugin.configuration;

import java.util.Comparator;
import java.util.List;

public class ConfigFileGroup extends ConfigFileGroupMetadata {
    private List<ConfigFile> configFileList;

    public List<ConfigFile> getConfigFileList() {
        return this.getConfigFileList(defaultComparator);
    }

    public List<ConfigFile> getConfigFileList(Comparator<ConfigFile> comparator) {
        if (configFileList != null && comparator != null) {
            configFileList.sort(comparator);
        }
        return configFileList;
    }

    public void setConfigFileList(List<ConfigFile> configFileList) {
        this.configFileList = configFileList;
    }

    public static final Comparator<ConfigFile> defaultComparator = Comparator.comparing(ConfigFile::getReleaseTime).reversed();
}
