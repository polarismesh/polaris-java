package com.tencent.polaris.api.plugin.configuration;

import com.tencent.polaris.api.plugin.Plugin;

public interface ConfigFileGroupConnector extends Plugin {
    ConfigFileGroupResponse GetConfigFileMetadataList(ConfigFileGroupMetadata configFileGroupMetadata, String revision);
}
