package com.tencent.polaris.configuration.client.internal;

import com.google.common.collect.Maps;

import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.factory.ConfigFileFactory;
import com.tencent.polaris.configuration.client.factory.ConfigFileFactoryManager;
import com.tencent.polaris.configuration.client.factory.DefaultConfigFileFactoryManager;

import java.util.Map;

/**
 *
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileManager implements ConfigFileManager {

    private static DefaultConfigFileManager instance;

    private ConfigFileFactoryManager configFileFactoryManager;

    private final Map<ConfigFileMetadata, ConfigFile>   configFileCache           = Maps.newConcurrentMap();
    private final Map<ConfigFileMetadata, ConfigKVFile> configPropertiesFileCache = Maps.newConcurrentMap();

    private DefaultConfigFileManager(SDKContext sdkContext) {
        configFileFactoryManager = DefaultConfigFileFactoryManager.getInstance(sdkContext);
    }

    public static DefaultConfigFileManager getInstance(SDKContext sdkContext) {
        if (instance == null) {
            synchronized (DefaultConfigFileManager.class) {
                if (instance == null) {
                    instance = new DefaultConfigFileManager(sdkContext);
                }
            }
        }
        return instance;
    }

    @Override
    public ConfigFile getConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFile configFile = configFileCache.get(configFileMetadata);

        if (configFile == null) {
            synchronized (this) {
                configFile = configFileCache.get(configFileMetadata);
                if (configFile == null) {
                    ConfigFileFactory configFileFactory = configFileFactoryManager.getFactory(configFileMetadata);

                    configFile = configFileFactory.createConfigFile(configFileMetadata);

                    configFileCache.put(configFileMetadata, configFile);
                }
            }
        }

        return configFile;
    }

    @Override
    public ConfigKVFile getConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat fileFormat) {
        ConfigKVFile configFile = configPropertiesFileCache.get(configFileMetadata);

        if (configFile == null) {
            synchronized (this) {
                configFile = configPropertiesFileCache.get(configFileMetadata);
                if (configFile == null) {
                    ConfigFileFactory configFileFactory = configFileFactoryManager.getFactory(configFileMetadata);

                    configFile = configFileFactory.createConfigKVFile(configFileMetadata, fileFormat);

                    configPropertiesFileCache.put(configFileMetadata, configFile);
                }
            }
        }

        return configFile;
    }

    void setConfigFileFactoryManager(ConfigFileFactoryManager configFileFactoryManager) {
        this.configFileFactoryManager = configFileFactoryManager;
    }

}
