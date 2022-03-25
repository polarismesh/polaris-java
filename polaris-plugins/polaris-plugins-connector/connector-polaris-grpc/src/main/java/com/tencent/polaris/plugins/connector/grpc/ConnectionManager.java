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

package com.tencent.polaris.plugins.connector.grpc;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
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
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.flow.BaseFlow;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.plugins.connector.grpc.Connection.ConnID;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        serverAddresses.put(ClusterType.BUILTIN_CLUSTER, new ServerAddressList(addresses, ClusterType.BUILTIN_CLUSTER));
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
                    new ServerAddressList(addresses, ClusterType.SERVICE_DISCOVER_CLUSTER));
        } else {
            serverAddresses
                    .put(ClusterType.SERVICE_DISCOVER_CLUSTER,
                            new ServerAddressList(discoverService, ClusterType.SERVICE_DISCOVER_CLUSTER));
        }
        if (null == configService) {
            serverAddresses.put(ClusterType.SERVICE_CONFIG_CLUSTER,
                                new ServerAddressList(addresses, ClusterType.SERVICE_CONFIG_CLUSTER));
        } else {
            serverAddresses
                .put(ClusterType.SERVICE_CONFIG_CLUSTER,
                     new ServerAddressList(configService, ClusterType.SERVICE_CONFIG_CLUSTER));
        }
        if (null == healthCheckService) {
            serverAddresses.put(ClusterType.HEALTH_CHECK_CLUSTER,
                    new ServerAddressList(addresses, ClusterType.HEALTH_CHECK_CLUSTER));
        } else {
            serverAddresses
                    .put(ClusterType.HEALTH_CHECK_CLUSTER,
                            new ServerAddressList(healthCheckService, ClusterType.HEALTH_CHECK_CLUSTER));
        }
        switchIntervalMs = serverConnectorConfig.getServerSwitchInterval();
        switchExecutorService = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("connection-manager"));
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
            if (connection.acquire()) {
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
                serverAddressList.switchClientOnFail(connID);
            }
        }
    }

    private class SwitchServerTask implements Runnable {

        @Override
        public void run() {
            for (Map.Entry<ClusterType, ServerAddressList> entry : serverAddresses.entrySet()) {
                ClusterType clusterType = entry.getKey();
                if (clusterType == ClusterType.BUILTIN_CLUSTER) {
                    continue;
                }
                ServerAddressList serverAddressList = entry.getValue();
                serverAddressList.switchClient();
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
        private int curIndex;

        /**
         * 埋点集群
         *
         * @param addresses 地址列表
         */
        ServerAddressList(List<String> addresses, ClusterType clusterType) {
            for (String address : addresses) {
                int colonIdx = address.lastIndexOf(":");
                String host = address.substring(0, colonIdx);
                int port = Integer.parseInt(address.substring(colonIdx + 1));
                nodes.add(new Node(host, port));
            }
            this.clusterType = clusterType;
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
                Node node = nodes.get(curIndex % nodes.size());
                curIndex++;
                return node;
            }
            Extensions extensions = ConnectionManager.this.extensions;
            Instance instance = getDiscoverInstance(extensions);
            return new Node(instance.getHost(), instance.getPort());
        }

        public void switchClientOnFail(ConnID lastConn) throws PolarisException {
            synchronized (lock) {
                Connection curConnection = curConnectionValue.get();
                if (null != curConnection && !curConnection.getConnID().equals(lastConn)) {
                    //已经完成切换，不处理
                    return;
                }
                Node servAddress = getServerAddress();
                if (null == servAddress) {
                    return;
                }
                if (null != curConnection) {
                    if (servAddress.getHost().equals(curConnection.getConnID().getHost())
                            && servAddress.getPort() == curConnection.getConnID().getPort()) {
                        return;
                    }
                    curConnection.lazyClose();
                }
                ConnID connID = new ConnID(serverServiceInfo.getServiceKey(), clusterType, servAddress.getHost(),
                        servAddress.getPort(), protocol);
                Connection connection = connectTarget(connID);
                curConnectionValue.set(connection);
            }
        }

        public void switchClient() throws PolarisException {
            Connection curConnection = curConnectionValue.get();
            //只有成功后，才进行切换
            if (!Connection.isAvailableConnection(curConnection)) {
                return;
            }
            LOG.info("start switch for {}", serverServiceInfo.getServiceKey());
            synchronized (lock) {
                curConnection = curConnectionValue.get();
                if (!Connection.isAvailableConnection(curConnection)) {
                    return;
                }
                Node servAddress = getServerAddress();
                if (null == servAddress) {
                    return;
                }
                if (servAddress.getHost().equals(curConnection.getConnID().getHost())
                        && servAddress.getPort() == curConnection.getConnID().getPort()) {
                    return;
                }
                ConnID connID = new ConnID(serverServiceInfo.getServiceKey(), clusterType, servAddress.getHost(),
                        servAddress.getPort(), protocol);
                Connection connection = connectTarget(connID);
                curConnection.lazyClose();
                curConnectionValue.set(connection);
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
                        .usePlaintext();
                ManagedChannel channel = builder.build();
                return new Connection(channel, connID, ConnectionManager.this);
            } catch (Throwable e) {
                throw new PolarisException(ErrorCode.NETWORK_ERROR,
                        String.format("[ConnectionManager]fail to create connection by %s", connID.toString()), e);
            }
        }
    }
}
