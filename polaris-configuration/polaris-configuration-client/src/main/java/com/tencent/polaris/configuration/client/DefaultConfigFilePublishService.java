package com.tencent.polaris.configuration.client;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFilePublishService;
import com.tencent.polaris.configuration.client.internal.ConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileManager;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;
import com.tencent.polaris.configuration.client.util.ConfigFileUtils;

/**
 * @author fabian4 2022-03-08
 */
public class DefaultConfigFilePublishService  extends BaseEngine implements ConfigFilePublishService {

    private ConfigFileManager configFileManager;

    public DefaultConfigFilePublishService(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() throws PolarisException {
        configFileManager = DefaultConfigFileManager.getInstance(sdkContext);
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
