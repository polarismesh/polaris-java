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

package com.tencent.polaris.api.plugin.common;

import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.auth.Authenticator;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.certificate.CertificateManager;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.circuitbreaker.InstanceCircuitBreaker;
import com.tencent.polaris.api.plugin.configuration.ConfigFileConnector;
import com.tencent.polaris.api.plugin.configuration.ConfigFileGroupConnector;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.plugin.event.EventReporter;
import com.tencent.polaris.api.plugin.filter.ConfigFileFilter;
import com.tencent.polaris.api.plugin.filter.Crypto;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.location.LocationProvider;
import com.tencent.polaris.api.plugin.lossless.LosslessPolicy;
import com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.plugin.stat.TraceReporter;
import com.tencent.polaris.api.plugin.weight.WeightAdjuster;

/**
 * The plugin types that our framework support.
 */
public enum PluginTypes {
    /**
     * 注册中心连接器扩展点
     */
    SERVER_CONNECTOR(new PluginType(ServerConnector.class, 0)),

    /**
     * 流程缓存扩展点
     */
    FLOW_CACHE(new PluginType(FlowCache.class, 0)),

    /**
     * 地理位置信息提供着扩展点
     */
    LOCAL_PROVIDER(new PluginType(LocationProvider.class, 0)),

    /**
     * 本地缓存扩展点
     */
    LOCAL_REGISTRY(new PluginType(LocalRegistry.class, 1)),

    /**
     * 服务路由扩展点
     */
    SERVICE_ROUTER(new PluginType(ServiceRouter.class, 2)),

    /**
     * 负载均衡扩展点
     */
    LOAD_BALANCER(new PluginType(LoadBalancer.class, 2)),

    /**
     * 健康探测扩展点
     */
    HEALTH_CHECKER(new PluginType(HealthChecker.class, 2)),

    /**
     * 节点熔断扩展点
     */
    INSTANCE_CIRCUIT_BREAKER(new PluginType(InstanceCircuitBreaker.class, 2)),

    /**
     * 资源熔断扩展点
     */
    CIRCUIT_BREAKER(new PluginType(CircuitBreaker.class, 2)),

    /**
     * 动态权重调整扩展点
     */
    WEIGHT_ADJUSTER(new PluginType(WeightAdjuster.class, 2)),

    /**
     * 统计监控上报扩展点
     */
    STAT_REPORTER(new PluginType(StatReporter.class, 2)),

    /**
     * 调用链上报扩展点
     */
    TRACE_REPORTER(new PluginType(TraceReporter.class, 2)),

    /**
     * 调用链上报扩展点
     */
    EVENT_REPORTER(new PluginType(EventReporter.class, 2)),

    /**
     * 限流器扩展点
     */
    SERVICE_LIMITER(new PluginType(ServiceRateLimiter.class, 2)),

    /**
     * 配置扩展点
     */
    CONFIG_FILTER(new PluginType(ConfigFileFilter.class, 2)),

    /**
     * 加密扩展点
     */
    CRYPTO(new PluginType(Crypto.class, 2)),

    /**
     * 配置文件加载器扩展点
     */
    CONFIG_FILE_CONNECTOR(new PluginType(ConfigFileConnector.class, 2)),

    /**
     * 配置连接器扩展点
     */
    CONFIG_FILE_GROUP_CONNECTOR(new PluginType(ConfigFileGroupConnector.class, 2)),

    /**
     * 无损上下线策略扩展点
     */
    LOSSLESS_POLICY(new PluginType(LosslessPolicy.class, 2)),

    /**
     * 服务鉴权扩展点
     */
    AUTHENTICATOR(new PluginType(Authenticator.class, 2)),

    /**
     * 证书管理扩展点
     */
    CERTIFICATE_MANAGER(new PluginType(CertificateManager.class, 2));

    private PluginType baseType;

    PluginTypes(PluginType baseType) {
        this.baseType = baseType;
    }

    public PluginType getBaseType() {
        return baseType;
    }
}
