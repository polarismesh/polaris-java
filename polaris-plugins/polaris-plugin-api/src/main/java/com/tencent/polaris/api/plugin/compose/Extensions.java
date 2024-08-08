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

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig.When;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.config.global.LocationConfig;
import com.tencent.polaris.api.config.global.LocationProviderConfig;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.HttpServerAware;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.cache.FlowCache;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.circuitbreaker.InstanceCircuitBreaker;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.plugin.loadbalance.LoadBalancer;
import com.tencent.polaris.api.plugin.location.LocationProvider;
import com.tencent.polaris.api.plugin.lossless.LosslessPolicy;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.route.ServiceRouter;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.plugin.stat.TraceReporter;
import com.tencent.polaris.api.plugin.weight.WeightAdjuster;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 流程编排所需要用到的插件实例列表
 *
 * @author andrewshan, Haotian Zhang
 */
public class Extensions extends Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(Extensions.class);

    private static final int MAX_EXTEND_PORT_RANGE = 10;

    private static final int HTTP_SERVER_BACKLOG_SIZE = 5;

    private final List<InstanceCircuitBreaker> instanceCircuitBreakers = new ArrayList<>();
    private final List<HealthChecker> healthCheckers = new ArrayList<>();
    private final Map<String, HealthChecker> allHealthCheckers = new HashMap<>();
    private LocalRegistry localRegistry;
    private ServerConnector serverConnector;
    private LoadBalancer loadBalancer;
    private Configuration configuration;
    private CircuitBreaker resourceBreaker;

    private final List<StatReporter> statReporters = new ArrayList<>();

    private TraceReporter traceReporter;

    private Supplier plugins;

    //系统服务的路由链
    private RouterChainGroup sysRouterChainGroup;

    //配置文件中加载的路由链
    private RouterChainGroup configRouterChainGroup;

    //流程缓存引擎
    private FlowCache flowCache;

    //全局变量
    private ValueContext valueContext;

    //全局监听端口，如果SDK需要暴露端口，则通过这里初始化
    private Map<Node, HttpServer> httpServers;

    //插件与端口的映射关系
    private Map<String, Node> pluginToNodes;

    // 无损上下线策略列表，按照order排序
    private List<LosslessPolicy> losslessPolicies;

    private List<WeightAdjuster> weightAdjusters;

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
     * @param config       配置
     * @param plugins      插件工厂
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
                Plugin pluginValue = plugins
                        .getOptionalPlugin(PluginTypes.INSTANCE_CIRCUIT_BREAKER.getBaseType(), cbName);
                if (null != pluginValue) {
                    instanceCircuitBreakers.add((InstanceCircuitBreaker) pluginValue);
                    continue;
                }
                pluginValue = plugins
                        .getOptionalPlugin(PluginTypes.CIRCUIT_BREAKER.getBaseType(), cbName);
                if (null != pluginValue) {
                    resourceBreaker = (CircuitBreaker) pluginValue;
                }
            }
        }

        //加载探测器
        loadOutlierDetector(config, plugins);

        serverConnector = (ServerConnector) plugins.getPlugin(PluginTypes.SERVER_CONNECTOR.getBaseType(),
                valueContext.getServerConnectorProtocol());

        // 加载监控上报
        loadStatReporters(plugins);

        // 加载调用链上报
        loadTraceReporter(plugins);

        // 加载优雅上下线插件
        loadLosslessPolicies(config, plugins);

        // 加载预热插件
        loadWeightAdjusters(plugins);

        initLocation(config, valueContext);

    }

    public ValueContext getValueContext() {
        return valueContext;
    }

    /**
     * get sdk current location from {@link List<LocationProvider>} providers
     * have one {@link LocationProvider} get location, stop. if not, use next {@link LocationProvider} to get
     * chain order: local -> remote http -> remote service
     */
    private void initLocation(Configuration config, ValueContext valueContext) {
        LocationConfig locationConfig = config.getGlobal().getLocation();
        List<LocationProvider> providers = new ArrayList<>();

        for (LocationProviderConfig providerConfig : locationConfig.getProviders()) {
            Plugin pluginValue = plugins
                    .getOptionalPlugin(PluginTypes.LOCAL_PROVIDER.getBaseType(), providerConfig.getTye());
            if (null == pluginValue) {
                LOG.warn("locationProvider plugin {} not found", providerConfig.getTye());
                continue;
            }

            providers.add((LocationProvider) pluginValue);
        }

        providers.sort(Comparator.comparingInt(o -> o.getProviderType().getPriority()));

        for (LocationProvider provider : providers) {
            ModelProto.Location location = provider.getLocation();
            if (location == null) {
                LOG.info("locationProvider plugin {} not found location", provider.getName());
                continue;
            }
            valueContext.setValue(RoutingProto.NearbyRoutingConfig.LocationLevel.REGION.name(), location.getRegion().getValue());
            valueContext.setValue(RoutingProto.NearbyRoutingConfig.LocationLevel.ZONE.name(), location.getZone().getValue());
            valueContext.setValue(RoutingProto.NearbyRoutingConfig.LocationLevel.CAMPUS.name(), location.getCampus().getValue());
            valueContext.notifyAllForLocationReady();
            break;
        }
    }

    private void loadHealthCheckers(Supplier plugins) throws PolarisException {
        Collection<Plugin> checkers = plugins.getPlugins(PluginTypes.HEALTH_CHECKER.getBaseType());
        if (CollectionUtils.isNotEmpty(checkers)) {
            for (Plugin checker : checkers) {
                HealthChecker healthChecker = (HealthChecker) checker;
                allHealthCheckers.put(healthChecker.getName(), healthChecker);
            }
        }

    }

    private void loadOutlierDetector(Configuration config, Supplier plugins) throws PolarisException {
        loadHealthCheckers(plugins);
        boolean enable = config.getConsumer().getOutlierDetection().getWhen() != When.never;
        List<String> detectionChain = config.getConsumer().getOutlierDetection().getChain();
        if (enable && CollectionUtils.isNotEmpty(detectionChain)) {
            for (String detectorName : detectionChain) {
                HealthChecker pluginValue = allHealthCheckers.get(detectorName);
                if (null == pluginValue) {
                    LOG.warn("outlierDetector plugin {} not found", detectorName);
                    continue;
                }
                healthCheckers.add(pluginValue);
            }
        }
    }

    private void loadStatReporters(Supplier plugins) throws PolarisException {
        Collection<Plugin> reporters = plugins.getPlugins(PluginTypes.STAT_REPORTER.getBaseType());
        if (CollectionUtils.isNotEmpty(reporters)) {
            for (Plugin reporter : reporters) {
                statReporters.add((StatReporter) reporter);
            }
        }
    }

    private void loadTraceReporter(Supplier plugins) throws PolarisException {
        if (configuration.getGlobal().getTraceReporter().isEnable()) {
            Collection<Plugin> reporters = plugins.getPlugins(PluginTypes.TRACE_REPORTER.getBaseType());
            if (CollectionUtils.isNotEmpty(reporters)) {
                traceReporter = (TraceReporter) reporters.iterator().next();
            }
        }
    }

    private void loadLosslessPolicies(Configuration config, Supplier plugins) throws PolarisException {

        if (!config.getProvider().getLossless().isEnable()) {
            return;
        }
        Collection<Plugin> losslessPolicyPlugins = plugins.getPlugins(PluginTypes.LOSSLESS_POLICY.getBaseType());
        losslessPolicies = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(losslessPolicyPlugins)) {
            for (Plugin plugin : losslessPolicyPlugins) {
                losslessPolicies.add((LosslessPolicy) plugin);
            }
        }
        losslessPolicies.sort((o1, o2) -> o1.getOrder() - o2.getOrder());
    }

    private void loadWeightAdjusters(Supplier plugins) throws PolarisException {
        if (!configuration.getConsumer().getWeightAdjust().isEnable()) {
            return;
        }

        weightAdjusters = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(configuration.getConsumer().getWeightAdjust().getChain())) {
            for (String weightAdjusterName : configuration.getConsumer().getWeightAdjust().getChain()) {
                Plugin pluginValue = plugins
                        .getOptionalPlugin(PluginTypes.WEIGHT_ADJUSTER.getBaseType(), weightAdjusterName);
                if (null == pluginValue) {
                    LOG.warn("weightAdjuster plugin {} not found", weightAdjusterName);
                    continue;
                }
                weightAdjusters.add((WeightAdjuster) pluginValue);
            }
        }
    }

    public void initHttpServer(Supplier plugins) {
        // 遍历插件并获取监听器
        Map<Node, Map<String, HttpHandler>> allHandlers = buildHttpHandlers(plugins);
        if (allHandlers == null) return;
        //启动监听
        httpServers = new HashMap<>();
        for (Map.Entry<Node, Map<String, HttpHandler>> entry : allHandlers.entrySet()) {
            Node node = entry.getKey();
            Map<String, HttpHandler> handlers = entry.getValue();
            try {
                HttpServer httpServer = HttpServer.create(
                        new InetSocketAddress(node.getHost(), node.getPort()), HTTP_SERVER_BACKLOG_SIZE);
                for (Map.Entry<String, HttpHandler> handlerEntry : handlers.entrySet()) {
                    httpServer.createContext(handlerEntry.getKey(), handlerEntry.getValue());
                }
                NamedThreadFactory threadFactory = new NamedThreadFactory("polaris-java-http");
                ExecutorService executor = Executors.newFixedThreadPool(3, threadFactory);
                httpServer.setExecutor(executor);
                httpServers.put(node, httpServer);
                startServer(threadFactory, httpServer);
            } catch (IOException e) {
                LOG.error("create polaris http server exception. host:{}, port:{}, path:{}",
                        node.getHost(), node.getPort(), handlers.keySet(), e);
                throw new PolarisException(ErrorCode.INTERNAL_ERROR, "Create polaris http server failed!", e);
            }
        }
    }

    Map<Node, Map<String, HttpHandler>> buildHttpHandlers(Supplier plugins) {
        pluginToNodes = new HashMap<>();
        Map<Node, Boolean> nodeToDrift = new HashMap<>();
        Map<Node, Map<String, HttpHandler>> allHandlers = new HashMap<>();
        for (Plugin plugin : plugins.getAllPlugins()) {
            if (plugin instanceof HttpServerAware) {
                HttpServerAware httpServerAware = (HttpServerAware) plugin;
                Map<String, HttpHandler> handlers = httpServerAware.getHandlers();
                if (CollectionUtils.isEmpty(handlers)) {
                    LOG.info("plugin {} has no http handlers", plugin.getName());
                    continue;
                }
                int port = httpServerAware.getPort();
                String host = httpServerAware.getHost();
                LOG.info("plugin {} listen on {}:{}, expose paths {}", plugin.getName(), host, port, handlers.keySet());
                if (port <= 0) {
                    throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                            String.format("invalid port %d to bind by plugin %s", port, plugin.getName()));
                }
                if (StringUtils.isBlank(host)) {
                    throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                            String.format("empty host to bind by plugin %s", plugin.getName()));
                }
                Node node = new Node(host, port);
                if (!allHandlers.containsKey(node)) {
                    Node targetNode = null;
                    for (Node existNode : allHandlers.keySet()) {
                        if ((existNode.isAnyAddress() || node.isAnyAddress()) && existNode.getPort() == node.getPort()) {
                            targetNode = existNode;
                            break;
                        }
                    }
                    if (null == targetNode) {
                        allHandlers.put(node, handlers);
                    } else {
                        Node replcaceNode = null;
                        if (!targetNode.isAnyAddress() && node.isAnyAddress()) {
                            // make any address replace the specific address
                            replcaceNode = node;
                        }
                        mergeHandlers(plugin, allHandlers, targetNode, handlers, replcaceNode);
                    }
                } else {
                    mergeHandlers(plugin, allHandlers, node, handlers, null);
                }
                addNodeToDrift(plugin, node, httpServerAware, nodeToDrift);
            }
        }
        if (MapUtils.isEmpty(allHandlers)) {
            LOG.info("no http paths to exposed, will not listen on any ports");
            return null;
        }
        // 校验端口重复，并自动迁移
        for (Map.Entry<Node, Boolean> entry : nodeToDrift.entrySet()) {
            Node node = entry.getKey();
            Node targetNode = null;
            for (int i = 0; i <= MAX_EXTEND_PORT_RANGE; i++) {
                Node probeNode = new Node(node.getHost(), node.getPort() + i);
                if (!probeNode.equals(node) && allHandlers.containsKey(probeNode)) {
                    // check duplicated
                    continue;
                }
                if (isPortAvailable(probeNode)) {
                    targetNode = probeNode;
                    break;
                } else if (!entry.getValue()) {
                    // 检查是否允许端口漂移
                    throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                            String.format("host %s, port %d conflicted", probeNode.getHost(), probeNode.getPort()));
                }
            }
            if (targetNode == null) {
                LOG.error("fail to bind {}, port conflict within range [{}, {}]", node,
                        node.getPort(), node.getPort() + MAX_EXTEND_PORT_RANGE);
                throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                        String.format("port conflict in node %s", node));
            }
            if (!targetNode.equals(node)) {
                LOG.info("listen port has changed from {} to {}", node.getPort(), targetNode.getPort());
                allHandlers.put(targetNode, allHandlers.remove(node));
                for (Map.Entry<String, Node> entry1 : pluginToNodes.entrySet()) {
                    if (entry1.getValue().equals(node)) {
                        pluginToNodes.put(entry1.getKey(), targetNode);
                    }
                }
            }
        }
        return allHandlers;
    }

    private void addNodeToDrift(Plugin plugin, Node node, HttpServerAware httpServerAware, Map<Node, Boolean> nodeToDrift) {
        boolean allowPortDrift = httpServerAware.allowPortDrift();
        Node targetNode = null;
        for (Node existNode : nodeToDrift.keySet()) {
            if ((existNode.isAnyAddress() || node.isAnyAddress()) && existNode.getPort() == node.getPort()) {
                targetNode = existNode;
                break;
            }
        }
        if (null == targetNode) {
            pluginToNodes.put(plugin.getName(), node);
            if (!allowPortDrift) {
                nodeToDrift.put(node, allowPortDrift);
            } else {
                nodeToDrift.putIfAbsent(node, allowPortDrift);
            }
        } else {
            boolean targetAllowDrift = !allowPortDrift ? allowPortDrift : nodeToDrift.get(targetNode);
            if (targetNode.isAnyAddress()) {
                nodeToDrift.put(targetNode, targetAllowDrift);
                pluginToNodes.put(plugin.getName(), targetNode);
            } else {
                nodeToDrift.remove(targetNode);
                nodeToDrift.put(node, targetAllowDrift);
                for (Map.Entry<String, Node> entry : pluginToNodes.entrySet()) {
                    if (entry.getValue().equals(targetNode)) {
                        pluginToNodes.put(entry.getKey(), node);
                    }
                }
                pluginToNodes.put(plugin.getName(), node);
            }
        }
    }

    private static void mergeHandlers(Plugin plugin, Map<Node, Map<String, HttpHandler>> allHandlers, Node existNode, Map<String, HttpHandler> handlers, Node replaceNode) {
        Map<String, HttpHandler> existsHandlers = allHandlers.get(existNode);
        // validate duplicated paths
        for (String key : handlers.keySet()) {
            if (existsHandlers.containsKey(key)) {
                throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT,
                        String.format("duplicated path %s in plugin %s", key, plugin.getName()));
            }
        }
        existsHandlers.putAll(handlers);
        if (null != replaceNode) {
            allHandlers.remove(existNode);
            allHandlers.put(replaceNode, existsHandlers);
        }
    }

    public Node getHttpServerNodeByPlugin(String name) {
        return pluginToNodes.get(name);
    }

    private static void startServer(ThreadFactory threadFactory, HttpServer httpServer) {
        if (Thread.currentThread().isDaemon()) {
            httpServer.start();
            return;
        }
        Thread httpServerThread = threadFactory.newThread(httpServer::start);
        httpServerThread.start();
        try {
            httpServerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isPortAvailable(Node node) {
        try {
            bindPort(node.getHost(), node.getPort());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private static void bindPort(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.bind(new InetSocketAddress(host, port));
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

    public CircuitBreaker getResourceBreaker() {
        return resourceBreaker;
    }

    public List<InstanceCircuitBreaker> getInstanceCircuitBreakers() {
        return instanceCircuitBreakers;
    }

    public List<StatReporter> getStatReporters() {
        return statReporters;
    }

    public List<HealthChecker> getHealthCheckers() {
        return healthCheckers;
    }

    public Map<String, HealthChecker> getAllHealthCheckers() {
        return allHealthCheckers;
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

    public List<LosslessPolicy> getLosslessPolicies() {
        return losslessPolicies;
    }

    public TraceReporter getTraceReporter() {
        return traceReporter;
    }

	public List<WeightAdjuster> getWeightAdjusters() {
		return weightAdjusters;
	}

    @Override
    protected void doDestroy() {
        if (MapUtils.isNotEmpty(httpServers)) {
            for (Map.Entry<Node, HttpServer> entry : httpServers.entrySet()) {
                LOG.info("stop http server for {}", entry.getKey());
                HttpServer httpServer = entry.getValue();
                httpServer.stop(0);
                ((ExecutorService) httpServer.getExecutor()).shutdownNow();
                Utils.sleepUninterrupted(1000);
            }
        }
    }
}
