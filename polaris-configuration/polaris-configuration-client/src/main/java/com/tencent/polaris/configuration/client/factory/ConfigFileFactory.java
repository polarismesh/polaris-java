package com.tencent.polaris.configuration.client.factory;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;

/**
 * @author lepdou 2022-03-02
 */
public interface ConfigFileFactory {

    ConfigFile createConfigFile(ConfigFileMetadata configFileMetadata);

    ConfigKVFile createConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat fileFormat);
}
