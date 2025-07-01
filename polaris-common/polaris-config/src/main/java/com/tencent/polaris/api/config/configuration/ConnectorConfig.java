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

    /**
     * 是否开启推空保护
     *
     * @return 是否开启推空保护
     */
    Boolean isEmptyProtectionEnable();

    /**
     * 推空保护过期时间，单位毫秒
     *
     * @return 推空保护过期时间
     */
    Long getEmptyProtectionExpiredInterval();
}
