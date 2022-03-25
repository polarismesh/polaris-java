package com.tencent.polaris.factory.config.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.configuration.ConnectorConfig;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;


/**
 * 配置中心连接器配置
 *
 * @author lepdou 2022-03-11
 */
public class ConnectorConfigImpl extends ServerConnectorConfigImpl implements ConnectorConfig {

    @JsonProperty
    private String connectorType;


    @Override
    public void verify() {
        ConfigUtils.validateString(connectorType, "configConnectorType");
        super.verify();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject == null) {
            return;
        }
        if (defaultObject instanceof ServerConnectorConfig) {
            ServerConnectorConfig serverConnectorConfig = (ServerConnectorConfig) defaultObject;
            super.setDefault(serverConnectorConfig);
        }
        if (defaultObject instanceof ConnectorConfig) {
            ConnectorConfig connectorConfig = (ConnectorConfig) defaultObject;
            if (connectorType == null) {
                this.connectorType = connectorConfig.getConnectorType();
            }
        }
    }

    @Override
    public String getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(String connectorType) {
        this.connectorType = connectorType;
    }
}
