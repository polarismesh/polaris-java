package com.tencent.polaris.factory.config.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.api.config.configuration.ConnectorConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * @author lepdou 2022-03-02
 */
public class ConfigFileConfigImpl implements ConfigFileConfig {

    @JsonProperty
    private ConnectorConfigImpl serverConnector;
    @JsonProperty
    private int                 propertiesValueCacheSize;
    @JsonProperty
    private long                propertiesValueExpireTime;

    @Override
    public void verify() {
        ConfigUtils.validateNull(serverConnector, "config server connector");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject != null) {
            ConfigFileConfig sourceConfig = (ConfigFileConfig) defaultObject;
            if (serverConnector == null) {
                serverConnector = new ConnectorConfigImpl();
            }
            serverConnector.setDefault(sourceConfig.getServerConnector());
            propertiesValueCacheSize = sourceConfig.getPropertiesValueCacheSize();
            propertiesValueExpireTime = sourceConfig.getPropertiesValueExpireTime();
        }
    }

    @Override
    public ConnectorConfig getServerConnector() {
        return serverConnector;
    }

    @Override
    public int getPropertiesValueCacheSize() {
        return propertiesValueCacheSize;
    }

    @Override
    public long getPropertiesValueExpireTime() {
        return propertiesValueExpireTime;
    }

    public void setServerConnector(ConnectorConfigImpl serverConnector) {
        this.serverConnector = serverConnector;
    }

    public void setPropertiesValueCacheSize(int propertiesValueCacheSize) {
        this.propertiesValueCacheSize = propertiesValueCacheSize;
    }

    public void setPropertiesValueExpireTime(long propertiesValueExpireTime) {
        this.propertiesValueExpireTime = propertiesValueExpireTime;
    }
}
