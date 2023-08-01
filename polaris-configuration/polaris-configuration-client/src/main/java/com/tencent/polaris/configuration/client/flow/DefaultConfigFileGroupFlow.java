package com.tencent.polaris.configuration.client.flow;

import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.flow.ConfigFileGroupFlow;
import com.tencent.polaris.configuration.client.internal.ConfigFileGroupManager;

import java.util.List;

public class DefaultConfigFileGroupFlow implements ConfigFileGroupFlow {
    private ConfigFileGroupManager manager;
    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.manager = new ConfigFileGroupManager(sdkContext);
    }

    @Override
    public List<ConfigFileMetadata> getConfigFileMetadataList(ConfigFileGroupMetadata metadata) {
        return manager.getConfigFileMetadataList(metadata);
    }
}
