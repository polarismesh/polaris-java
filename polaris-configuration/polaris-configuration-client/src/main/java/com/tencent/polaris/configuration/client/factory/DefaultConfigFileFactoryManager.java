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

    private final Map<ConfigFileMetadata, ConfigFileFactory> factories = Maps.newConcurrentMap();

    private static DefaultConfigFileFactoryManager instance;
    private final  SDKContext                      sdkContext;
    private        ConfigFileFactory               defaultConfigFileFactory;

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
    public ConfigFileFactory getFactory(ConfigFileMetadata configFileMetadata) {
        ConfigFileFactory factory = factories.get(configFileMetadata);

        if (factory != null) {
            return factory;
        }

        if (defaultConfigFileFactory == null) {
            defaultConfigFileFactory = DefaultConfigFileFactory.getInstance(sdkContext);
        }

        factories.put(configFileMetadata, defaultConfigFileFactory);

        return defaultConfigFileFactory;
    }

    @JustForTest
    void setDefaultConfigFileFactory(ConfigFileFactory factory) {
        this.defaultConfigFileFactory = factory;
    }
}
