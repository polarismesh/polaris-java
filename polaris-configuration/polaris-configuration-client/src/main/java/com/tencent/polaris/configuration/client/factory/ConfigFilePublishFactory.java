package com.tencent.polaris.configuration.client.factory;

import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

/**
 * @author fabian4 2022-03-08
 */
public interface ConfigFilePublishFactory {

    void releaseConfigFile(ConfigFileMetadata configFileMetadata);
}
