/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.api.config.configuration;

import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.config.configuration.ConfigFilterConfigImpl;
import com.tencent.polaris.factory.config.configuration.ConnectorConfigImpl;

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
    ConnectorConfigImpl getServerConnector();

    /**
     * 配置文件过滤器
     *
     * @return 过滤器配置对象
     */
    ConfigFilterConfigImpl getConfigFilterConfig();

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
