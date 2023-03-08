package com.tencent.polaris.configuration.client;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.*;
import com.tencent.polaris.configuration.client.internal.ConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import com.tencent.polaris.configuration.client.util.ConfigFileUtils;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileService extends BaseEngine implements ConfigFileService, ConfigFilePublishAPI {

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

    @Override
    public void createConfigFile(String namespace, String fileGroup, String fileName, String content) {
        createConfigFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName), content);
    }

    @Override
    public void createConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        configFileManager.createConfigFile(configFileMetadata, content);
    }

    @Override
    public void updateConfigFile(String namespace, String fileGroup, String fileName, String content) {
        updateConfigFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName), content);
    }

    @Override
    public void updateConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        configFileManager.updateConfigFile(configFileMetadata, content);
    }

    @Override
    public void releaseConfigFile(String namespace, String fileGroup, String fileName) {
        releaseConfigFile(new DefaultConfigFileMetadata(namespace, fileGroup, fileName));
    }

    @Override
    public void releaseConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileUtils.checkConfigFileMetadata(configFileMetadata);
        configFileManager.releaseConfigFile(configFileMetadata);
    }

}
