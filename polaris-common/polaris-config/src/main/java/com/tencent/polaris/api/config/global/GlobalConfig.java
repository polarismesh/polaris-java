/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.api.config.global;

import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import java.util.List;

/**
 * 全局配置对象
 *
 * @author andrewshan, Haotian Zhang
 */
public interface GlobalConfig extends Verifier {

    /**
     * 获取系统配置
     *
     * @return SystemConfig
     */
    SystemConfig getSystem();

    /**
     * services.global.api前缀开头的所有配置项
     *
     * @return APIConfig
     */
    APIConfig getAPI();

    /**
     * global.serverConnector前缀开头的所有配置项
     *
     * @return ServerConnectorConfig
     */
    ServerConnectorConfig getServerConnector();

    /**
     * Configuration of prefix of "global.serverConnectors". This has higher priority over
     * ${@link GlobalConfig#getServerConnector()}.
     *
     * @return List of ServerConnectorConfig
     */
    List<ServerConnectorConfigImpl> getServerConnectors();

    /**
     * Configuration of prefix of "global.statReporter"
     *
     * @return StatReporterConfig
     */
    StatReporterConfig getStatReporter();

    /**
     * Configuration of prefix of "global.traceReporter"
     * @return
     */
    TraceReporterConfig getTraceReporter();

    /**
     * 获取地理位置提供者配置信息
     *
     * @return location
     */
    LocationConfig getLocation();
}
