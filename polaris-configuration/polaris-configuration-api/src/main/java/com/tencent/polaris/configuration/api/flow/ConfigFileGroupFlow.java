package com.tencent.polaris.configuration.api.flow;

import com.tencent.polaris.client.flow.AbstractFlow;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

import java.util.List;

public interface ConfigFileGroupFlow extends AbstractFlow {
    default List<ConfigFileMetadata> getConfigFileMetadataList(ConfigFileGroupMetadata metadata) {
        return null;
    }
}
