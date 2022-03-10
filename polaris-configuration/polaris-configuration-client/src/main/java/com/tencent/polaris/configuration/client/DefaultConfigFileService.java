package com.tencent.polaris.configuration.client;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.ConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import com.tencent.polaris.configuration.client.util.ConfigFileUtils;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileService extends BaseEngine implements ConfigFileService {

    private ConfigFileManager configFileManager;

    public DefaultConfigFileService(SDKContext sdkContext) {
        super(sdkContext);
    }

    @JustForTest
    DefaultConfigFileService(SDKContext sdkContext, ConfigFileManager configFileManager) {
        super(sdkContext);
        this.configFileManager = configFileManager;
    }

    @Override
    protected void subInit() throws PolarisException {
        configFileManager = DefaultConfigFileManager.getInstance(sdkContext);
    }

    @Override
    public ConfigKVFile getConfigPropertiesFile(String namespace, String fileGroup, String fileName) {
        return getConfigPropertiesFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName));
    }

    @Override
    public ConfigKVFile getConfigPropertiesFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        return configFileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Properties);
    }

    @Override
    public ConfigKVFile getConfigYamlFile(String namespace, String fileGroup, String fileName) {
        return getConfigYamlFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName));
    }

    @Override
    public ConfigKVFile getConfigYamlFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        return configFileManager.getConfigKVFile(configFileMetadata, ConfigFileFormat.Yaml);
    }

    @Override
    public ConfigFile getConfigFile(String namespace, String fileGroup, String fileName) {
        return getConfigFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName));
    }

    @Override
    public ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        return configFileManager.getConfigFile(configFileMetadata);
    }
}
