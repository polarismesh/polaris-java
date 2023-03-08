package com.tencent.polaris.configuration.client.factory;

import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileFormat;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigKVFile;
import com.tencent.polaris.configuration.client.internal.ConfigFileLongPollingService;
import com.tencent.polaris.configuration.client.internal.ConfigFileRepo;
import com.tencent.polaris.configuration.client.internal.ConfigPropertiesFile;
import com.tencent.polaris.configuration.client.internal.ConfigYamlFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFile;
import com.tencent.polaris.configuration.client.internal.DefaultConfigFileLongPollingService;
import com.tencent.polaris.configuration.client.internal.RemoteConfigFileRepo;

/**
 * @author lepdou 2022-03-01
 */
public class DefaultConfigFileFactory implements ConfigFileFactory {

    private static DefaultConfigFileFactory instance;

    private final SDKContext sdkContext;

    private final ConfigFileConnector configFileConnector;

    private final ConfigFileLongPollingService configFileLongPollingService;

    private DefaultConfigFileFactory(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
        this.configFileLongPollingService = DefaultConfigFileLongPollingService.getInstance(sdkContext);
        String configFileConnectorType = sdkContext.getConfig().getConfigFile().getServerConnector()
                .getConnectorType();
        this.configFileConnector = (ConfigFileConnector) sdkContext.getExtensions().getPlugins()
                .getPlugin(PluginTypes.CONFIG_FILE_CONNECTOR.getBaseType(), configFileConnectorType);
    }

    public static DefaultConfigFileFactory getInstance(SDKContext sdkContext) {
        if (instance == null) {
            synchronized (DefaultConfigFileFactory.class) {
                if (instance == null) {
                    instance = new DefaultConfigFileFactory(sdkContext);
                }
            }
        }
        return instance;
    }

    @Override
    public ConfigFile createConfigFile(ConfigFileMetadata configFileMetadata) {
        ConfigFileRepo configFileRepo = createConfigFileRepo(configFileMetadata);

        return new DefaultConfigFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                                     configFileMetadata.getFileName(), configFileRepo,
                                     sdkContext.getConfig().getConfigFile());
    }

    @Override
    public ConfigKVFile createConfigKVFile(ConfigFileMetadata configFileMetadata, ConfigFileFormat format) {
        ConfigFileRepo configFileRepo = createConfigFileRepo(configFileMetadata);
        switch (format) {
            case Properties: {
                return new ConfigPropertiesFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                                                configFileMetadata.getFileName(), configFileRepo,
                                                sdkContext.getConfig().getConfigFile());
            }
            case Yaml: {
                return new ConfigYamlFile(configFileMetadata.getNamespace(), configFileMetadata.getFileGroup(),
                                          configFileMetadata.getFileName(), configFileRepo,
                                          sdkContext.getConfig().getConfigFile());
            }
            default:
                throw new IllegalArgumentException("KV file only support properties and yaml file.");
        }
    }

    @Override
    public void createConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent(content);
        configFileConnector.createConfigFile(configFile);
    }

    @Override
    public void updateConfigFile(ConfigFileMetadata configFileMetadata, String content) {
        com.tencent.polaris.api.plugin.configuration.ConfigFile configFile = new com.tencent.polaris.api.plugin.configuration.ConfigFile(configFileMetadata.getNamespace(),
                configFileMetadata.getFileGroup(),
                configFileMetadata.getFileName());
        configFile.setContent(content);
        configFileConnector.updateConfigFile(configFile);
    }

    private ConfigFileRepo createConfigFileRepo(ConfigFileMetadata configFileMetadata) {
        return new RemoteConfigFileRepo(sdkContext, configFileLongPollingService, null, configFileMetadata);
    }
}
