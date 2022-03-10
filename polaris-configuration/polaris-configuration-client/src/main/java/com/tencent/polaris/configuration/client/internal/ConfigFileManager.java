package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;

/**
 * @author lepdou 2022-03-02
 */
public interface ConfigFileManager {

    ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata);

    ConfigKVFile getConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat fileFormat);
}
