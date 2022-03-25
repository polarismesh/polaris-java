package com.tencent.polaris.api.config.configuration;

import com.tencent.polaris.api.config.global.ServerConnectorConfig;


/**
 * 配置中心连接器配置
 *
 * @author lepdou 2022-03-11
 */
public interface ConnectorConfig extends ServerConnectorConfig {

    /**
     * 连接器类型
     *
     * @return 连接器类型
     */
    String getConnectorType();
}
