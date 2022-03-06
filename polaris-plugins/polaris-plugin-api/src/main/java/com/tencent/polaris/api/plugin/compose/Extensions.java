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

package com.tencent.polaris.api.plugin.compose;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig.When;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.utils.CollectionUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流程编排所需要用到的插件实例列表
 *
 * @author andrewshan, Haotian Zhang
 */
public class Extensions {

    private static final Logger LOG = LoggerFactory.getLogger(Extensions.class);
    private final List<CircuitBreaker> circuitBreakers = new ArrayList<>();
    private final List<HealthChecker> healthCheckers = new ArrayList<>();
    private LocalRegistry localRegistry;
    private ServerConnector serverConnector;
    private LoadBalancer loadBalancer;
    private Configuration configuration;

    private Supplier plugins;

    //系统服务的路由链
    private RouterChainGroup sysRouterChainGroup;

    //配置文件中加载的路由链
    private RouterChainGroup configRouterChainGroup;

    //流程缓存引擎
    private FlowCache flowCache;

    //全局变量
    private ValueContext valueContext;

    public static List<ServiceRouter> loadServiceRouters(List<String> routerChain, Supplier plugins, boolean force) {
        List<ServiceRouter> routers = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(routerChain)) {
            for (String routerName : routerChain) {
                Plugin routerPlugin;
                if (force) {
                    routerPlugin = plugins.getPlugin(PluginTypes.SERVICE_ROUTER.getBaseType(), routerName);
                } else {
                    routerPlugin = plugins.getOptionalPlugin(PluginTypes.SERVICE_ROUTER.getBaseType(), routerName);
                }
                if (null == routerPlugin) {
                    LOG.warn("router {} not found", routerName);
                    continue;
                }
                routers.add((ServiceRouter) routerPlugin);
            }
        }
        return Collections.unmodifiableList(routers);
    }

    /**
     * 初始化
     *
     * @param config 配置
     * @param plugins 插件工厂
     * @param valueContext 全局变量
     * @throws PolarisException 异常
     */
    public void init(Configuration config, Supplier plugins, ValueContext valueContext) throws PolarisException {
        this.configuration = config;
        this.plugins = plugins;
        this.valueContext = valueContext;
        String localCacheType = config.getConsumer().getLocalCache().getType();
        localRegistry = (LocalRegistry) plugins.getPlugin(PluginTypes.LOCAL_REGISTRY.getBaseType(), localCacheType);
        String flowCacheName = config.getGlobal().getSystem().getFlowCache().getName();
        flowCache = (FlowCache) plugins.getPlugin(PluginTypes.FLOW_CACHE.getBaseType(), flowCacheName);
        String loadBalanceType = config.getConsumer().getLoadbalancer().getType();
        loadBalancer = (LoadBalancer) plugins.getPlugin(PluginTypes.LOAD_BALANCER.getBaseType(), loadBalanceType);

        List<ServiceRouter> beforeRouters = loadServiceRouters(config.getConsumer().getServiceRouter().getBeforeChain(),
                plugins, true);
        List<ServiceRouter> coreRouters = loadServiceRouters(config.getConsumer().getServiceRouter().getChain(),
                plugins, false);
        List<ServiceRouter> afterRouters = loadServiceRouters(config.getConsumer().getServiceRouter().getAfterChain(),
                plugins, true);
        configRouterChainGroup = new DefaultRouterChainGroup(beforeRouters, coreRouters, afterRouters);
        //加载系统路由链
        List<String> sysBefore = new ArrayList<>();
        sysBefore.add(ServiceRouterConfig.DEFAULT_ROUTER_ISOLATED);
        List<String> sysAfter = new ArrayList<>();
        sysAfter.add(ServiceRouterConfig.DEFAULT_ROUTER_RECOVER);
        List<ServiceRouter> sysBeforeRouters = loadServiceRouters(sysBefore, plugins, true);
        List<ServiceRouter> sysAfterRouters = loadServiceRouters(sysAfter, plugins, true);
        sysRouterChainGroup = new DefaultRouterChainGroup(sysBeforeRouters, Collections.emptyList(), sysAfterRouters);

        //加载熔断器
        boolean enable = config.getConsumer().getCircuitBreaker().isEnable();
        List<String> cbChain = config.getConsumer().getCircuitBreaker().getChain();
        if (enable && CollectionUtils.isNotEmpty(cbChain)) {
            for (String cbName : cbChain) {
                Plugin pluginValue = plugins.getOptionalPlugin(PluginTypes.CIRCUIT_BREAKER.getBaseType(), cbName);
                if (null == pluginValue) {
                    LOG.warn("circuitBreaker plugin {} not found", cbName);
                    continue;
                }
                circuitBreakers.add((CircuitBreaker) pluginValue);
            }
        }

        //加载探测器
        loadOutlierDetector(config, plugins);

        serverConnector = (ServerConnector) plugins.getPlugin(PluginTypes.SERVER_CONNECTOR.getBaseType(),
                valueContext.getServerConnectorProtocol());
    }

    public ValueContext getValueContext() {
        return valueContext;
    }

    private void loadOutlierDetector(Configuration config, Supplier plugins) throws PolarisException {
        boolean enable = config.getConsumer().getOutlierDetection().getWhen() != When.never;
        List<String> detectionChain = config.getConsumer().getOutlierDetection().getChain();
        if (enable && CollectionUtils.isNotEmpty(detectionChain)) {
            for (String detectorName : detectionChain) {
                Plugin pluginValue = plugins.getOptionalPlugin(PluginTypes.HEALTH_CHECKER.getBaseType(), detectorName);
                if (null == pluginValue) {
                    LOG.warn("outlierDetector plugin {} not found", detectorName);
                    continue;
                }
                healthCheckers.add((HealthChecker) pluginValue);
            }
        }
    }

    public Supplier getPlugins() {
        return plugins;
    }

    public LocalRegistry getLocalRegistry() {
        return localRegistry;
    }

    public LoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public List<CircuitBreaker> getCircuitBreakers() {
        return circuitBreakers;
    }

    public List<HealthChecker> getHealthCheckers() {
        return healthCheckers;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ServerConnector getServerConnector() {
        return serverConnector;
    }

    public RouterChainGroup getSysRouterChainGroup() {
        return sysRouterChainGroup;
    }

    public RouterChainGroup getConfigRouterChainGroup() {
        return configRouterChainGroup;
    }

    public FlowCache getFlowCache() {
        return flowCache;
    }
}
