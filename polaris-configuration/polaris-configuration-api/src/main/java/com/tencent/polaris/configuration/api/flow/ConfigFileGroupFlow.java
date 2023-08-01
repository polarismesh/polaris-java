package com.tencent.polaris.configuration.api.flow;

import com.tencent.polaris.client.flow.AbstractFlow;
import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;

public interface ConfigFileGroupFlow extends AbstractFlow {
    default ConfigFileGroup getConfigFileGroup(ConfigFileGroupMetadata metadata) {
        return null;
    }
}
