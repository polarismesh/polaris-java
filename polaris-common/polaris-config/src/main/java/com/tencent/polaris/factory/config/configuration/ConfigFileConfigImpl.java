package com.tencent.polaris.factory.config.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * @author lepdou 2022-03-02
 */
public class ConfigFileConfigImpl implements ConfigFileConfig {

    @JsonProperty
    private String connectorType;
    @JsonProperty
    private int    propertiesValueCacheSize;
    @JsonProperty
    private long   propertiesValueExpireTime;

    @Override
    public String connectorType() {
        return connectorType;
    }

    @Override
    public int propertiesValueCacheSize() {
        return propertiesValueCacheSize;
    }

    @Override
    public long propertiesValueExpireTime() {
        return propertiesValueExpireTime;
    }

    @Override
    public void verify() {
        ConfigUtils.validateString(connectorType, "configFile.connectorType");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject != null) {
            ConfigFileConfig sourceConfig = (ConfigFileConfig) defaultObject;
            connectorType = sourceConfig.connectorType();
            propertiesValueCacheSize = sourceConfig.propertiesValueCacheSize();
            propertiesValueExpireTime = sourceConfig.propertiesValueExpireTime();
        }
    }
}
