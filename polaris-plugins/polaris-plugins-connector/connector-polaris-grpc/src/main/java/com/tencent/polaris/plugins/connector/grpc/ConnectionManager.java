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

package com.tencent.polaris.plugins.connector.grpc;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.compose.ServerServiceInfo;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.grpc.Connection.ConnID;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 用于管理与后端服务器的GRPC连接.
 *
 * @author andrewshan, Haotian Zhang
 */
public class ConnectionManager extends Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    /**
     * 首次连接控制锁
     */
    private final Object lock = new Object();
    private final long connectTimeoutMs;
    private final long switchIntervalMs;
    private final ScheduledExecutorService switchExecutorService;
    private final String protocol;
    private final Map<ClusterType, ServerAddressList> serverAddresses = new HashMap<>();
    private final Map<ClusterType, CompletableFuture<String>> readyNotifiers = new HashMap<>();
    private final String clientId;
    private Extensions extensions;
    private final ChannelTlsCertificates tlsCertificates;
    private Consumer<ConnID> callbackOnSwitched;

    /**
     * 构造器
     *
     * @param initContext 上下文
     * @param notifiers 回调函数
     */
    public ConnectionManager(InitContext initContext, ServerConnectorConfig serverConnectorConfig,
            Map<ClusterType, CompletableFuture<String>> notifiers) {
        this.clientId = initContext.getValueContext().getClientId();
        this.readyNotifiers.putAll(notifiers);

        if (serverConnectorConfig == null) {
            Configuration config = initContext.getConfig();
            serverConnectorConfig = config.getGlobal().getServerConnector();
        }

        this.connectTimeoutMs = serverConnectorConfig.getConnectTimeout();
        this.protocol = serverConnectorConfig.getProtocol();
        List<String> addresses = serverConnectorConfig.getAddresses();
        String lbPolicy = serverConnectorConfig.getLbPolicy();
        serverAddresses.put(ClusterType.BUILTIN_CLUSTER, new ServerAddressList(addresses, ClusterType.BUILTIN_CLUSTER, lbPolicy));
        Collection<ServerServiceInfo> serverServices = initContext.getServerServices();
        ServerServiceInfo discoverService = null;
        ServerServiceInfo healthCheckService = null;
        ServerServiceInfo configService = null;
        if (CollectionUtils.isNotEmpty(serverServices)) {
            for (ServerServiceInfo serverService : serverServices) {
                if (serverService.getClusterType() == ClusterType.SERVICE_DISCOVER_CLUSTER) {
                    discoverService = serverService;
                    continue;
                }
                if (serverService.getClusterType() == ClusterType.HEALTH_CHECK_CLUSTER) {
                    healthCheckService = serverService;
                }
                if (serverService.getClusterType() == ClusterType.SERVICE_CONFIG_CLUSTER) {
                    configService = serverService;
                }
            }
        }
        if (null == discoverService) {
            serverAddresses.put(ClusterType.SERVICE_DISCOVER_CLUSTER,
                    new ServerAddressList(addresses, ClusterType.SERVICE_DISCOVER_CLUSTER, lbPolicy));
        } else {
            serverAddresses
                    .put(ClusterType.SERVICE_DISCOVER_CLUSTER,
                            new ServerAddressList(discoverService, ClusterType.SERVICE_DISCOVER_CLUSTER));
        }
        if (null == configService) {
            serverAddresses.put(ClusterType.SERVICE_CONFIG_CLUSTER,
                    new ServerAddressList(addresses, ClusterType.SERVICE_CONFIG_CLUSTER, lbPolicy));
        } else {
            serverAddresses
                    .put(ClusterType.SERVICE_CONFIG_CLUSTER,
                            new ServerAddressList(configService, ClusterType.SERVICE_CONFIG_CLUSTER));
        }
        if (null == healthCheckService) {
            serverAddresses.put(ClusterType.HEALTH_CHECK_CLUSTER,
                    new ServerAddressList(addresses, ClusterType.HEALTH_CHECK_CLUSTER, lbPolicy));
        } else {
            serverAddresses
                    .put(ClusterType.HEALTH_CHECK_CLUSTER,
                            new ServerAddressList(healthCheckService, ClusterType.HEALTH_CHECK_CLUSTER));
        }
        switchIntervalMs = serverConnectorConfig.getServerSwitchInterval();
        switchExecutorService = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("connection-manager"));
        tlsCertificates = ChannelTlsCertificates.build(serverConnectorConfig);
    }

    public void setExtensions(Extensions extensions) {
        synchronized (lock) {
            this.extensions = extensions;
        }
        switchExecutorService.scheduleAtFixedRate(new SwitchServerTask(), switchIntervalMs, switchIntervalMs,
                TimeUnit.MILLISECONDS);
    }

    public boolean checkReady(ClusterType clusterType) {
        ServerAddressList serverAddressList = serverAddresses.get(clusterType);
        if (null == serverAddressList) {
            return false;
        }
        return serverAddressList.ready.get();
    }

    public Consumer<ConnID> getCallbackOnSwitched() {
        return callbackOnSwitched;
    }

    public void setCallbackOnSwitched(
            Consumer<ConnID> callbackOnSwitched) {
        this.callbackOnSwitched = callbackOnSwitched;
    }

    /**
     * 设置准备状态
     *
     * @param serviceEventKey 服务信息
     */
    public void makeReady(ServiceEventKey serviceEventKey) {
        for (ServerAddressList serverAddressList : serverAddresses.values()) {
            if (serverAddressList.checkAndSetReady(serviceEventKey)) {
                return;
            }
        }
    }

    /**
     * 获取连接
     *
     * @param opKey 操作key
     * @param clusterType 集群类型
     * @return 连接
     */
    public Connection getConnection(String opKey, ClusterType clusterType) {
        while (true) {
            Connection connection;
            try {
                connection = tryGetConnection(opKey, clusterType);
            } catch (PolarisException e) {
                LOG.error("fail to get connection, opKey is {}, cluster {}", opKey, clusterType, e);
                throw e;
            }
            if (connection.acquire(opKey)) {
                LOG.debug("connection id={} acquired", connection.getConnID());
                return connection;
            }
        }
    }

    private Connection tryGetConnection(String opKey, ClusterType clusterType) throws PolarisException {
        if (null == extensions) {
            throw new PolarisException(ErrorCode.INVALID_STATE, "connection manager not ready");
        }
        ServerAddressList serverAddressList = serverAddresses.get(clusterType);
        if (null == serverAddressList) {
            throw new PolarisException(ErrorCode.INVALID_CONFIG, String.format("unknown clusterType %s", clusterType));
        }
        return serverAddressList.tryGetConnection(opKey, connectTimeoutMs);
    }


    @Override
    public void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{switchExecutorService});
        for (Map.Entry<ClusterType, ServerAddressList> entry : serverAddresses.entrySet()) {
            ServerAddressList serverAddressList = entry.getValue();
            serverAddressList.shutdown();
        }
    }

    /**
     * 上报错误连接，要求重连
     *
     * @param connId 连接ID
     */
    public void reportFailConnection(ConnID connId) {
        if (isDestroyed()) {
            return;
        }
        LOG.debug("connection id={} reportFailConnection", connId);
        switchExecutorService.execute(new SwitchTargetTask(connId));
    }

    private class SwitchTargetTask implements Runnable {

        private final ConnID connID;

        public SwitchTargetTask(ConnID connID) {
            this.connID = connID;
        }


        @Override
        public void run() {
            ServerAddressList serverAddressList = serverAddresses.get(connID.getClusterType());
            if (null != serverAddressList) {
                try {
                    serverAddressList.switchClientOnFail(connID);
                } catch (PolarisException e) {
                    LOG.error("switch client on fail for {}, e:{}", connID, e);
                }
            }
        }
    }

    private class SwitchServerTask implements Runnable {

        @Override
        public void run() {
            for (Map.Entry<ClusterType, ServerAddressList> entry : serverAddresses.entrySet()) {
                ClusterType clusterType = entry.getKey();
                try {
                    ServerAddressList serverAddressList = entry.getValue();
                    serverAddressList.switchClient();
                } catch (PolarisException e) {
                    LOG.error("switch client for {}, e:{}", clusterType, e);
                }
            }
        }
    }

    private class ServerAddressList {

        private final ServerServiceInfo serverServiceInfo;

        private final ClusterType clusterType;

        private final AtomicReference<Connection> curConnectionValue = new AtomicReference<>();
        private final List<Node> nodes = new ArrayList<>();
        private final Object lock = new Object();
        private final AtomicBoolean ready = new AtomicBoolean(false);
        private String lbPolicy = LoadBalanceConfig.LOAD_BALANCE_ROUND_ROBIN;
        private int curIndex;
        private Node curNode;

        /**
         * 埋点集群
         *
         * @param addresses 地址列表
         */
        ServerAddressList(List<String> addresses, ClusterType clusterType, String lbPolicy) {
            for (String address : addresses) {
                int colonIdx = address.lastIndexOf(":");
                String host = IPAddressUtils.getIpCompatible(address.substring(0, colonIdx));
                int port = Integer.parseInt(address.substring(colonIdx + 1));
                nodes.add(new Node(host, port));
            }
            this.clusterType = clusterType;
            this.lbPolicy = lbPolicy;
            serverServiceInfo = null;
            makeReady();
        }

        /**
         * 服务发现集群
         *
         * @param serverServiceInfo 服务名
         */
        ServerAddressList(ServerServiceInfo serverServiceInfo, ClusterType clusterType) {
            this.clusterType = clusterType;
            this.serverServiceInfo = serverServiceInfo;
        }

        /**
         * 设置准备好状态
         *
         * @param serviceEventKey
         * @return 是否设置成功
         */
        public boolean checkAndSetReady(ServiceEventKey serviceEventKey) {
            if (null == serverServiceInfo) {
                return false;
            }
            if (serverServiceInfo.getServiceKey().equals(serviceEventKey.getServiceKey())) {
                makeReady();
                return true;
            }
            return false;
        }

        private void makeReady() {
            LOG.info("[ServerConnector]cluster {}, service {} has been made ready", clusterType, serverServiceInfo);
            if (ready.compareAndSet(false, true)) {
                CompletableFuture<String> future = readyNotifiers.get(clusterType);
                if (null != future) {
                    future.complete("ready");
                }
            }
        }

        /**
         * 获取连接
         *
         * @param opKey 来源的方法
         * @return 连接对象
         * @throws PolarisException
         */
        public Connection tryGetConnection(String opKey, long timeoutMs) throws PolarisException {
            Connection curConnection = curConnectionValue.get();
            if (Connection.isAvailableConnection(curConnection)) {
                return curConnection;
            }
            synchronized (lock) {
                curConnection = curConnectionValue.get();
                if (Connection.isAvailableConnection(curConnection)) {
                    return curConnection;
                }
                Node servAddress = getServerAddress();
                ServiceKey svcKey = null;
                if (null != serverServiceInfo) {
                    svcKey = serverServiceInfo.getServiceKey();
                }
                ConnID connID = new ConnID(svcKey, clusterType, servAddress.getHost(),
                        servAddress.getPort(), protocol);
                Connection connection = connectTarget(connID);
                LOG.info("connection {} created", connID);
                if (null != curConnection) {
                    curConnection.lazyClose();
                }
                curConnectionValue.set(connection);
                return connection;
            }
        }

        /**
         * 停止服务
         */
        public void shutdown() {
            synchronized (lock) {
                Connection curConnection = curConnectionValue.get();
                if (Connection.isAvailableConnection(curConnection)) {
                    curConnection.lazyClose();
                }
            }
        }

        private Node getServerAddress() throws PolarisException {
            if (null == serverServiceInfo) {
                switch (lbPolicy) {
                    case LoadBalanceConfig.LOAD_BALANCE_NEARBY_BACKUP:
                        curNode = LoadBalanceUtils.nearbyBackupLoadBalance(nodes, curNode);
                        return curNode;
                    case LoadBalanceConfig.LOAD_BALANCE_ROUND_ROBIN:
                    default:
                        // 使用取模运算确保索引不越界
                        int index = curIndex % nodes.size();
                        Node node = nodes.get(index);
                        if (LOG.isDebugEnabled()) {
                            LOG.info("Node {} is chosen with index {} in node list {}.", node, index, nodes);
                        } else {
                            LOG.info("Node {} is chosen with index {}.", node, index);
                        }
                        // 递增索引，并确保不超过nodes.size()
                        curIndex = (curIndex + 1) % nodes.size();
                        curNode = new Node(node);
                        return node;
                }
            }
            Extensions extensions = ConnectionManager.this.extensions;
            Instance instance = getDiscoverInstance(extensions);
            return new Node(IPAddressUtils.getIpCompatible(instance.getHost()), instance.getPort());
        }

        public void switchClientOnFail(ConnID lastConn) throws PolarisException {
            synchronized (lock) {
                Connection curConnection = curConnectionValue.get();
                if (null != curConnection && !curConnection.getConnID().equals(lastConn)) {
                    //已经完成切换，不处理
                    return;
                }
                doSwitchClient(curConnection);
            }
        }

        private void doSwitchClient(Connection curConnection) throws PolarisException {
            Node servAddress = getServerAddress();
            if (null == servAddress) {
                return;
            }
            String preAddress = null;
            if (null != curConnection) {
                curConnection.lazyClose();
                preAddress = String.format(
                        "%s:%d", curConnection.getConnID().getHost(), curConnection.getConnID().getPort());
            }
            String namespace = DefaultValues.DEFAULT_SYSTEM_NAMESPACE;
            String serviceName = DefaultValues.DEFAULT_BUILTIN_DISCOVER;
            if (null != serverServiceInfo) {
                namespace = serverServiceInfo.getServiceKey().getNamespace();
                serviceName = serverServiceInfo.getServiceKey().getService();
            }
            ConnID connID = new ConnID(new ServiceKey(namespace, serviceName), clusterType, servAddress.getHost(),
                    servAddress.getPort(), protocol);
            Connection connection = connectTarget(connID);
            curConnectionValue.set(connection);
            LOG.info("server {} connection switched from {} to {}:{}",
                    serviceName, preAddress, servAddress.getHost(), servAddress.getPort());
            if (null != callbackOnSwitched) {
                callbackOnSwitched.accept(connection.getConnID());
            }
        }

        public void switchClient() throws PolarisException {
            Connection curConnection = curConnectionValue.get();
            //只有成功后，才进行切换
            if (!Connection.isAvailableConnection(curConnection)) {
                return;
            }
            synchronized (lock) {
                curConnection = curConnectionValue.get();
                if (!Connection.isAvailableConnection(curConnection)) {
                    return;
                }
                doSwitchClient(curConnection);
            }
        }


        private Instance getDiscoverInstance(Extensions extensions) throws PolarisException {
            ServiceKey serviceKey = serverServiceInfo.getServiceKey();
            Instance instance = BaseFlow
                    .commonGetOneInstance(extensions, serviceKey, serverServiceInfo.getRouters(),
                            serverServiceInfo.getLbPolicy(), protocol, clientId);
            LOG.info("[ConnectionManager]success to get instance for service {}, instance is {}:{}", serviceKey,
                    instance.getHost(), instance.getPort());
            return instance;
        }

        private Connection connectTarget(ConnID connID) throws PolarisException {
            try {
                ManagedChannelBuilder<?> builder = ManagedChannelBuilder.forAddress(connID.getHost(), connID.getPort())
                        .keepAliveTime(2, TimeUnit.MINUTES)
                        .keepAliveWithoutCalls(true)
                        .usePlaintext();
                if (tlsCertificates != null) {
                    ManagedChannelUtil.setChannelTls(builder, tlsCertificates);
                    builder.useTransportSecurity();
                }
                ManagedChannel channel = builder.build();
                return new Connection(channel, connID, ConnectionManager.this);
            } catch (Throwable e) {
                throw new PolarisException(ErrorCode.NETWORK_ERROR,
                        String.format("[ConnectionManager]fail to create connection by %s", connID.toString()), e);
            }
        }
    }
}
