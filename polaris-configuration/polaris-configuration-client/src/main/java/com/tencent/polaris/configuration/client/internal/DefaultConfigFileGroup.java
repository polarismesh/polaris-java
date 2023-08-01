package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import java.util.List;

public class DefaultConfigFileGroup extends DefaultConfigFileGroupMetadata implements ConfigFileGroup {
    private List<ConfigFileMetadata> configFileMetadataList;

    public DefaultConfigFileGroup(String namespace, String fileGroupName, List<ConfigFileMetadata> configFileMetadataList) {
        super(namespace, fileGroupName);
        this.configFileMetadataList = configFileMetadataList;
    }

    @Override
    public List<ConfigFileMetadata> getConfigFileMetadataList() {
        return configFileMetadataList;
    }
}
