package com.tencent.polaris.configuration.api.core;

import java.util.List;

public interface ConfigFileGroup extends ConfigFileGroupMetadata {
    List<ConfigFileMetadata> getConfigFileMetadataList();
}
