package com.tencent.polaris.configuration.api.core;

import java.util.List;

public class ConfigFileGroupChangedEvent {
    private final ConfigFileGroupMetadata configFileGroupMetadata;
    private final List<ConfigFileMetadata> configFileMetadataList;

    public ConfigFileGroupChangedEvent(ConfigFileGroupMetadata configFileGroupMetadata, List<ConfigFileMetadata> configFileMetadataList) {
        this.configFileGroupMetadata = configFileGroupMetadata;
        this.configFileMetadataList = configFileMetadataList;
    }

    public ConfigFileGroupMetadata getConfigFileGroupMetadata() {
        return configFileGroupMetadata;
    }

    public List<ConfigFileMetadata> getConfigFileMetadataList() {
        return configFileMetadataList;
    }
}
