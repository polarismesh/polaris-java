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
     * Name of Nacos Server Connector
     */
    String SERVER_CONNECTOR_NACOS = "nacos";

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


    /**
     * 基于组合规则的熔断插件名
     */
    String CIRCUIT_BREAKER_COMPOSITE = "composite";

    /**
     * polaris 配置中心连接器插件名
     */
    String POLARIS_FILE_CONNECTOR_TYPE = "polaris";

    /**
     * 本地配置连接器插件名
     */
    String LOCAL_FILE_CONNECTOR_TYPE = "local";

    /**
     * consul 配置中心连接器插件名
     */
    String CONSUL_FILE_CONNECTOR_TYPE = "consul";

    /**
     * logger 事件上报插件名
     */
    String LOGGER_EVENT_REPORTER_TYPE = "logger";

    /**
     * TSF 事件上报插件名
     */
    String TSF_EVENT_REPORTER_TYPE = "tsf";

    /**
     * PushGateway 事件上报插件名
     */
    String PUSH_GATEWAY_EVENT_REPORTER_TYPE = "pushgateway";

    /**
     * 黑白名单鉴权插件名
     */
    String BLOCK_ALLOW_LIST_AUTHENTICATOR_TYPE = "blockAllowList";

    /**
     * TSF证书管理插件名
     */
    String TSF_CERTIFICATE_MANAGER = "tsfCertificateManager";
}
