package com.tencent.polaris.api.config.configuration;

import com.tencent.polaris.api.config.verify.Verifier;

/**
 * 配置中心相关的配置项
 *
 * @author lepdou 2022-03-01
 */
public interface ConfigFileConfig extends Verifier {

    /**
     * 配置文件连接器
     *
     * @return 连接器配置对象
     */
    ConnectorConfig getServerConnector();

    /**
     * 值缓存的最大数量
     *
     * @return 最大数量
     */
    int getPropertiesValueCacheSize();

    /**
     * 缓存的过期时间，默认为 60s
     *
     * @return 值缓存过期时间
     */
    long getPropertiesValueExpireTime();

}
