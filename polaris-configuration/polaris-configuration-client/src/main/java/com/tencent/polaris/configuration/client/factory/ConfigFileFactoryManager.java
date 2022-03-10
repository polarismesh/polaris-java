package com.tencent.polaris.configuration.client.factory;

import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;

/**
 * @author lepdou 2022-03-02
 */
public interface ConfigFileFactoryManager {

    ConfigFileFactory getFactory(ConfigFileMetadata configFileMetadata);
}
