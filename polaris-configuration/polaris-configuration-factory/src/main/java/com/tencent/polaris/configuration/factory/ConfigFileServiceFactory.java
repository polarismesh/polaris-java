package com.tencent.polaris.configuration.factory;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import com.tencent.polaris.configuration.client.DefaultConfigFileService;
import com.tencent.polaris.factory.ConfigAPIFactory;

/**
 * @author lepdou 2022-03-01
 */
public class ConfigFileServiceFactory {

    private static DefaultConfigFileService configFileService;

    public static ConfigFileService createConfigFileService() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createConfigFileService(configuration);
    }

    public static ConfigFileService createConfigFileService(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createConfigFileService(context);
    }

    public static ConfigFileService createConfigFileService(SDKContext sdkContext) throws PolarisException {
        if (configFileService == null) {
            synchronized (ConfigFileServiceFactory.class) {
                if (configFileService == null) {
                    configFileService = new DefaultConfigFileService(sdkContext);
                    configFileService.init();
                }
            }
        }
        return configFileService;
    }

}
