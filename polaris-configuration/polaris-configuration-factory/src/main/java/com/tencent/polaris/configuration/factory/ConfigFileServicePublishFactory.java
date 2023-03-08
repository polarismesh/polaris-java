package com.tencent.polaris.configuration.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFilePublishService;
import com.tencent.polaris.configuration.client.DefaultConfigFilePublishService;
import com.tencent.polaris.factory.ConfigAPIFactory;

/**
 * @author fabian4 2022-03-08
 */
public class ConfigFileServicePublishFactory {

    private static DefaultConfigFilePublishService configFilePublishService;

    public static ConfigFilePublishService createConfigFilePublishService() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createConfigFilePublishService(configuration);
    }

    public static ConfigFilePublishService createConfigFilePublishService(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createConfigFilePublishService(context);
    }

    public static ConfigFilePublishService createConfigFilePublishService(SDKContext sdkContext) throws PolarisException {
        if (configFilePublishService == null) {
            synchronized (ConfigFileServiceFactory.class) {
                if (configFilePublishService == null) {
                    configFilePublishService = new DefaultConfigFilePublishService(sdkContext);
                    configFilePublishService.init();
                }
            }
        }
        return configFilePublishService;
    }
}
