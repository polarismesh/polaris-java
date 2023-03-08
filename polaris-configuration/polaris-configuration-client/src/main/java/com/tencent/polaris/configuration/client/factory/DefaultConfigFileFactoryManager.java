package com.tencent.polaris.configuration.client.factory;

import com.google.common.collect.Maps;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.client.JustForTest;

import java.util.Map;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileFactoryManager implements ConfigFileFactoryManager {

    private final SDKContext sdkContext;
    private ConfigFileFactory defaultConfigFileFactory;
    private ConfigFilePublishFactory defaultConfigFilePublishFactory;

    private final Map<ConfigFileMetadata, ConfigFileFactory> configFileFactories = Maps.newConcurrentMap();

    private final Map<ConfigFileMetadata, ConfigFilePublishFactory> configFilePublishFactories = Maps.newConcurrentMap();

    private static DefaultConfigFileFactoryManager instance;

    private DefaultConfigFileFactoryManager(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
    }

    public static DefaultConfigFileFactoryManager getInstance(SDKContext sdkContext) {
        if (instance == null) {
            synchronized (DefaultConfigFileFactoryManager.class) {
                if (instance == null) {
                    instance = new DefaultConfigFileFactoryManager(sdkContext);
                }
            }
        }
        return instance;
    }

    @Override
    public ConfigFileFactory getConfigFileFactory(ConfigFileMetadata configFileMetadata) {
        ConfigFileFactory factory = configFileFactories.get(configFileMetadata);

        if (factory != null) {
            return factory;
        }

        if (defaultConfigFileFactory == null) {
            defaultConfigFileFactory = DefaultConfigFileFactory.getInstance(sdkContext);
        }

        configFileFactories.put(configFileMetadata, defaultConfigFileFactory);

        return defaultConfigFileFactory;
    }

    @Override
    public ConfigFilePublishFactory getConfigFilePublishFactory(ConfigFileMetadata configFileMetadata) {
        ConfigFilePublishFactory factory = configFilePublishFactories.get(configFileMetadata);

        if (factory != null) {
            return factory;
        }

        if (defaultConfigFilePublishFactory == null) {
            defaultConfigFilePublishFactory = DefaultConfigFilePublishFactory.getInstance(sdkContext);
        }

        configFilePublishFactories.put(configFileMetadata, defaultConfigFilePublishFactory);

        return defaultConfigFilePublishFactory;
    }

    @JustForTest
    void setDefaultConfigFileFactory(ConfigFileFactory factory) {
        this.defaultConfigFileFactory = factory;
    }

}
