/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.client.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.ClusterConfig;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.config.global.SystemConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Manager;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.Supplier;
import com.tencent.polaris.api.plugin.TypeProvider;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.plugin.impl.PluginManager;
import com.tencent.polaris.api.plugin.server.ReportClientRequest;
import com.tencent.polaris.api.plugin.server.ReportClientResponse;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.plugin.stat.ReporterMetaInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.flow.AbstractFlow;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.version.Version;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SDK初始化相关的上下文信息
 *
 * @author andrewshan, Haotian Zhang
 */
public class SDKContext extends Destroyable implements InitContext, AutoCloseable, Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(SDKContext.class);
    private static final String DEFAULT_ADDRESS = "127.0.0.1";

    /**
     * 客户端ID自增序列
     */
    private static final AtomicInteger CLIENT_ID_SEQ = new AtomicInteger(0);

    /**
     * 配置对象
     */
    private final Configuration configuration;
    /**
     * 初始化标识
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    /**
     * 插件管理器
     */
    private final Manager plugins;
    private final ValueContext valueContext;
    private final Extensions extensions = new Extensions();
    private final Object lock = new Object();
    private final List<Destroyable> destroyHooks = new ArrayList<>();
    private final Collection<ServerServiceInfo> serverServices;

    private final ScheduledExecutorService reportClientExecutorService;

    /**
     * 构造器
     *
     * @param configuration 配置
     * @param plugins       插件工厂
     * @param valueContext  上下文
     */
    public SDKContext(Configuration configuration, Manager plugins, ValueContext valueContext) {
        this.configuration = configuration;
        this.plugins = plugins;
        this.valueContext = valueContext;
        this.valueContext.setClientId(generateClientId(getHostNameOrDefaultToHost(this.valueContext.getHost())));
        LOG.info("init SDKContext with clientId={}", this.valueContext.getClientId());
        List<ServerServiceInfo> services = new ArrayList<>();
        //加载系统服务配置
        SystemConfig system = configuration.getGlobal().getSystem();
        ClusterConfig discoverCluster = system.getDiscoverCluster();
        if (clusterAvailable(discoverCluster)) {
            services.add(new ServerServiceInfo(ClusterType.SERVICE_DISCOVER_CLUSTER, discoverCluster));
        }
        ClusterConfig configCluster = system.getConfigCluster();
        if (clusterAvailable(configCluster)) {
            services.add(new ServerServiceInfo(ClusterType.SERVICE_CONFIG_CLUSTER, configCluster));
        }
        ClusterConfig healthCheckCluster = system.getHealthCheckCluster();
        if (clusterAvailable(healthCheckCluster)) {
            services.add(new ServerServiceInfo(ClusterType.HEALTH_CHECK_CLUSTER, healthCheckCluster));
        }
        ClusterConfig monitorCluster = system.getMonitorCluster();
        if (clusterAvailable(monitorCluster)) {
            services.add(new ServerServiceInfo(ClusterType.MONITOR_CLUSTER, monitorCluster));
        }
        this.serverServices = Collections.unmodifiableCollection(services);
        this.reportClientExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("polaris-report-client"));
    }

    private static String generateClientId(String host) {
        if (!StringUtils.equalsIgnoreCase(host, DEFAULT_ADDRESS)) {
            return host + "_" + getProcessId("0") + "_" + CLIENT_ID_SEQ.getAndIncrement();
        } else {
            return UUID.randomUUID().toString();
        }
    }

    private static String getHostNameOrDefaultToHost(String host) {
        try {
            String hostName = IPAddressUtils.getHostName();
            if (StringUtils.isBlank(hostName)) {
                hostName = host;
            }
            return hostName;
        } catch (Throwable throwable) {
            LOG.error("fail to get host name", throwable);
            return host;
        }
    }

    private static String getProcessId(String fallback) {
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            return fallback;
        }

        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException e) {
            // ignore
        }
        return fallback;
    }

    /**
     * 通过默认配置初始化SDKContext
     *
     * @return SDKContext对象
     * @throws PolarisException 初始化异常
     */
    public static SDKContext initContext() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return initContextByConfig(configuration);
    }

    /**
     * 通过配置对象初始化SDK上下文
     *
     * @param config 配置对象
     * @return SDK上下文
     * @throws PolarisException 初始化过程的异常
     */
    public static SDKContext initContextByConfig(Configuration config) throws PolarisException {
        try {
            ((ConfigurationImpl) config).setDefault();
            config.verify();
            ObjectMapper mapper = new JsonMapper();
            LOG.info("SDKContext config {} ", mapper.writeValueAsString(config));
        } catch (IllegalArgumentException e) {
            throw new PolarisException(ErrorCode.INVALID_CONFIG, "fail to verify configuration", e);
        } catch (JsonProcessingException ignore) {
        }
        ServiceLoader<TypeProvider> providers = ServiceLoader.load(TypeProvider.class);
        List<PluginType> types = new ArrayList<>();
        for (TypeProvider provider : providers) {
            types.addAll(provider.getTypes());
        }
        PluginManager manager = new PluginManager(types);
        ValueContext valueContext = new ValueContext();
        valueContext.setHost(parseHost(config));
        valueContext.setServerConnectorProtocol(parseServerConnectorProtocol(config));
        SDKContext initContext = new SDKContext(config, manager, valueContext);

        try {
            manager.initPlugins(initContext);
        } catch (Throwable e) {
            manager.destroyPlugins();
            if (e instanceof PolarisException) {
                throw e;
            }
            throw new PolarisException(ErrorCode.PLUGIN_ERROR, "plugin error", e);
        }
        return initContext;
    }

    public static String parseHost(Configuration configuration) {
        String hostAddress = configuration.getGlobal().getAPI().getBindIP();
        if (!StringUtils.isBlank(hostAddress)) {
            return hostAddress;
        }
        String nic = configuration.getGlobal().getAPI().getBindIf();
        if (StringUtils.isNotBlank(nic)) {
            return resolveAddress(nic);
        }
        try {
            return getHostByDial(configuration);
        } catch (IOException e) {
            LOG.info("[ReportClient]get address by dial failed: {}", e.getMessage());
        }
        return DEFAULT_ADDRESS;
    }

    /**
     * Get protocol of server connector, such as:
     * <ul>
     * <li>{@link DefaultPlugins#SERVER_CONNECTOR_COMPOSITE}</li>
     * <li>{@link DefaultPlugins#SERVER_CONNECTOR_GRPC}</li>
     * <li>{@link DefaultPlugins#SERVER_CONNECTOR_CONSUL}</li>
     * </ul>
     *
     * @param configuration
     * @return
     */
    public static String parseServerConnectorProtocol(Configuration configuration) {
        String protocol;
        if (CollectionUtils.isNotEmpty(configuration.getGlobal().getServerConnectors())) {
            // Composite server connector first
            protocol = DefaultPlugins.SERVER_CONNECTOR_COMPOSITE;
        } else {
            // If composite server connector does not exist.
            protocol = configuration.getGlobal().getServerConnector().getProtocol();
        }
        return protocol;
    }

    private static String getHostByDial(Configuration configuration) throws IOException {
        String serverAddress;
        if (CollectionUtils.isNotEmpty(configuration.getGlobal().getServerConnectors())) {
            // Composite server connector first
            serverAddress = configuration.getGlobal().getServerConnectors().get(0).getAddresses().get(0);
        } else {
            // If composite server connector does not exist.
            serverAddress = configuration.getGlobal().getServerConnector().getAddresses().get(0);
        }

        String[] tokens = serverAddress.split(":");
        try (Socket socket = new Socket(tokens[0], Integer.parseInt(tokens[1]))) {
            return socket.getLocalAddress().getHostAddress();
        }
    }

    private static NetworkInterface resolveNetworkInterface(String nic) {
        NetworkInterface ni = null;
        try {
            ni = NetworkInterface.getByName(nic);
        } catch (SocketException e) {
            LOG.error("[ReportClient]get nic failed, nic:{}", nic, e);
        }
        if (null != ni) {
            return ni;
        }
        //获取第一张网卡
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (!networkInterface.isLoopback() && networkInterface.getInetAddresses().hasMoreElements()) {
                    return networkInterface;
                }
            }
        } catch (SocketException e) {
            LOG.error("[ReportClient]get all network interfaces failed", e);
        }
        return null;
    }

    /**
     * 解析网卡IP
     *
     * @param nic 网卡标识，如eth1
     * @return 地址信息
     */
    private static String resolveAddress(String nic) {
        NetworkInterface ni = resolveNetworkInterface(nic);
        if (null == ni) {
            return DEFAULT_ADDRESS;
        }
        Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
        if (inetAddresses.hasMoreElements()) {
            InetAddress inetAddress = inetAddresses.nextElement();
            return inetAddress.getCanonicalHostName();
        }
        return DEFAULT_ADDRESS;
    }

    public synchronized void init() throws PolarisException {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        extensions.init(configuration, plugins, valueContext);
        plugins.postContextInitPlugins(extensions);
        extensions.initHttpServer(configuration, plugins);
        reportClient(extensions);
        registerDestroyHook(extensions);
    }

    private boolean clusterAvailable(ClusterConfig clusterConfig) {
        if (null == clusterConfig) {
            return false;
        }
        if (StringUtils.isBlank(clusterConfig.getNamespace()) || StringUtils.isBlank(clusterConfig.getService())) {
            return false;
        }
        if (clusterConfig.isSameAsBuiltin()) {
            return false;
        }
        return true;
    }

    /**
     * Report prometheus http server metadata periodic
     *
     * @param extensions extensions
     */
    private void reportClient(Extensions extensions) {
        if (extensions.getConfiguration().getGlobal().getAPI().isReportEnable()) {
            reportClientExecutorService.scheduleAtFixedRate(() -> {
                ServerConnector serverConnector = extensions.getServerConnector();
                ReportClientRequest reportClientRequest = new ReportClientRequest();
                reportClientRequest.setClientHost(extensions.getValueContext().getHost());
                reportClientRequest.setVersion(Version.VERSION);
                List<StatReporter> statPlugins = extensions.getStatReporters();
                List<ReporterMetaInfo> reporterMetaInfos = new ArrayList<>();
                for (StatReporter statPlugin : statPlugins) {
                    ReporterMetaInfo reporterMetaInfo = statPlugin.metaInfo();
                    if (StringUtils.isNotBlank(reporterMetaInfo.getProtocol())) {
                        reporterMetaInfos.add(reporterMetaInfo);
                    }
                }
                reportClientRequest.setReporterMetaInfos(reporterMetaInfos);
                reportClientRequest.setTimeoutMs(extensions.getConfiguration().getGlobal().getAPI().getTimeout());

                try {
                    ReportClientResponse reportClientResponse = serverConnector.reportClient(reportClientRequest);
                    LOG.debug("Report client success, response:{}", reportClientResponse);
                } catch (PolarisException e) {
                    LOG.error("Report client failed.", e);
                }
            }, 0L, 60L, TimeUnit.SECONDS);
        }
    }

    public Extensions getExtensions() {
        return extensions;
    }

    public Configuration getConfig() {
        return configuration;
    }

    public Supplier getPlugins() {
        return plugins;
    }

    @Override
    protected void doDestroy() {
        synchronized (lock) {
            for (Destroyable destroyable : destroyHooks) {
                destroyable.destroy();
            }
        }
        plugins.destroyPlugins();
        if (Objects.nonNull(reportClientExecutorService)) {
            reportClientExecutorService.shutdown();
        }
    }

    public ValueContext getValueContext() {
        return valueContext;
    }

    @Override
    public Collection<ServerServiceInfo> getServerServices() {
        return serverServices;
    }

    public void registerDestroyHook(Destroyable destroyable) {
        synchronized (lock) {
            destroyHooks.add(destroyable);
        }
    }

    @Override
    public void close() {
        destroy();
    }

    private static <T extends AbstractFlow> T loadFlow(String name, Class<T> clazz) {
        ServiceLoader<T> flows = ServiceLoader.load(clazz);
        for (T flow : flows) {
            if (StringUtils.equals(flow.getName(), name)) {
                return flow;
            }
        }
        throw new PolarisException(ErrorCode.INVALID_CONFIG,
                String.format("unknown flow name %s, type is %s", name, clazz.getCanonicalName()));
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractFlow> T getOrInitFlow(Class<T> clazz) {
        synchronized (clazz) {
            Object flowObject = valueContext.getValue(clazz.getCanonicalName());
            if (null != flowObject) {
                return (T) flowObject;
            }
            String flowName = configuration.getGlobal().getSystem().getFlow().getName();
            T flow = loadFlow(flowName, clazz);
            flow.setSDKContext(this);
            valueContext.setValue(clazz.getCanonicalName(), flow);
            return flow;
        }
    }
}
