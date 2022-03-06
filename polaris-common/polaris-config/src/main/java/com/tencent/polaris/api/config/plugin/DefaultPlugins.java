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

package com.tencent.polaris.api.config.plugin;

/**
 * 默认插件名
 *
 * @author andrewshann, Haotian Zhang
 */
public interface DefaultPlugins {

    /**
     * GRPC连接器插件名
     */
    String SERVER_CONNECTOR_GRPC = "grpc";

    /**
     * Name of Consul server connector.
     */
    String SERVER_CONNECTOR_CONSUL = "consul";

    /**
     * Name of composite server connector.
     */
    String SERVER_CONNECTOR_COMPOSITE = "composite";

    /**
     * 基于内存的本地缓存插件名
     */
    String LOCAL_REGISTRY_IN_MEMORY = "inmemory";

    /**
     * 基于连续错误数的熔断插件名
     */
    String CIRCUIT_BREAKER_ERROR_COUNT = "errorCount";

    /**
     * 基于错误率的熔断插件名
     */
    String CIRCUIT_BREAKER_ERROR_RATE = "errorRate";


}
