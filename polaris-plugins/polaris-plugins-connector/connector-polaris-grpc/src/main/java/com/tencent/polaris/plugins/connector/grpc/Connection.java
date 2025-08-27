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

package com.tencent.polaris.plugins.connector.grpc;

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.config.verify.DefaultValues;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.logging.LoggerFactory;
import io.grpc.ManagedChannel;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

/**
 * 封装的连接对象
 *
 * @author andrewshan
 * @date 2019/8/22
 */
public class Connection {

    private static final Logger LOG = LoggerFactory.getLogger(Connection.class);

    /**
     * 连接标识
     */
    private final ConnID connID;

    /**
     * GRPC连接
     */
    private final ManagedChannel channel;

    /**
     * 连接管理器
     */
    private final ConnectionManager connectionManager;


    /**
     * 创建时间
     */
    private final long createTimeMs;

    /**
     * 引用数
     */
    private final AtomicInteger ref = new AtomicInteger(0);

    /**
     * 是否已经开始销毁
     */
    private final AtomicBoolean lazyDestroy = new AtomicBoolean(false);

    /**
     * 申请锁
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 连接是否已经关闭
     */
    private boolean closed;

    public Connection(ManagedChannel channel, ConnID connID, ConnectionManager connectionManager) {
        this.connID = connID;
        this.channel = channel;
        this.createTimeMs = System.currentTimeMillis();
        this.connectionManager = connectionManager;
    }

    /**
     * 是否可用连接
     *
     * @param connection 连接对象
     * @return boolean
     */
    public static boolean isAvailableConnection(Connection connection) {
        if (null == connection) {
            return false;
        }
        return !connection.lazyDestroy.get();
    }

    /**
     * 尝试占据连接，ref+1
     *
     * @return 占据成功返回true，否则返回false
     */
    public boolean acquire(String opKey) {
        if (lazyDestroy.get()) {
            LOG.warn("connection {}: acquired for op {} need destroy, curRef is {}", connID, opKey, ref.get());
            return false;
        }
        boolean locked = false;
        try {
            // retry outside.
            locked = lock.tryLock(1, TimeUnit.SECONDS);
            if (locked) {
                if (lazyDestroy.get()) {
                    return false;
                }
                int curRef = ref.incrementAndGet();
                LOG.debug("connection {}: acquired for op {}, curRef is {}", connID, opKey, curRef);
                return true;
            } else {
                LOG.warn("connection {}: acquired for op {} timeout, curRef is {}", connID, opKey, ref.get());
                return false;
            }
        } catch (Exception e) {
            LOG.warn("connection {}: acquired for op {} occur exception, curRef is {}, msg:{}", connID, opKey, ref.get(), e.getMessage());
            return false;
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * 关闭连接
     */
    public void closeConnection() {
        lock.lock();
        try {
            if (ref.get() <= 0 && !closed) {
                LOG.info("connection {}: closed", connID);
                closed = true;
                // Gracefully shutdown the gRPC managed-channel.
                if (channel != null && !channel.isShutdown()) {
                    try {
                        channel.shutdown();
                        if (!channel.awaitTermination(1, TimeUnit.SECONDS)) {
                            LOG.warn("Timed out gracefully shutting down connection: {}. ", connID);
                        }
                    } catch (Exception e) {
                        LOG.error("Unexpected exception while waiting for channel {} gracefully termination", connID, e);
                    }
                }

                // Forcefully shutdown if still not terminated.
                if (channel != null && !channel.isTerminated()) {
                    try {
                        channel.shutdownNow();
                        if (!channel.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                            LOG.warn("Timed out forcefully shutting down connection: {}. ", connID);
                        }
                        LOG.debug("Success to forcefully shutdown connection: {}. ", connID);
                    } catch (Exception e) {
                        LOG.error("Unexpected exception while waiting for channel {} forcefully termination", connID, e);
                    }
                } else {
                    LOG.debug("Success to gracefully shutdown connection: {}. ", connID);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 懒回收
     */
    public void lazyClose() {
        //设置状态，不允许该连接再继续分配
        lazyDestroy.set(true);
        int curRef = ref.get();
        LOG.info("connection {}: lazyClose, curRef is {}", connID, curRef);
        if (curRef <= 0) {
            closeConnection();
        }
    }

    /**
     * 释放连接占用
     *
     * @param opKey 操作key
     */
    public void release(String opKey) {
        int nextValue = ref.decrementAndGet();
        LOG.debug("connection {}: pending to release for op {}, curRef is {}", connID, opKey, nextValue);
        if (nextValue == 0 && lazyDestroy.get()) {
            closeConnection();
        }
    }

    public void reportFail(ErrorCode errorCode) {
        if (!Objects.equals(ErrorCode.NETWORK_ERROR, errorCode)) {
            return;
        }
        connectionManager.reportFailConnection(connID);
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    public long getCreateTimeMs() {
        return createTimeMs;
    }

    public ConnID getConnID() {
        return connID;
    }

    public static class ConnID {

        /**
         * 连接ID，UUID
         */
        private final String id;

        /**
         * 所属服务
         */
        private final ServiceKey serviceKey;

        /**
         * 集群类型
         */
        private final ClusterType clusterType;

        /**
         * Server端主机信息
         */
        private final String host;

        /**
         * Server端端口信息
         */
        private final int port;

        /**
         * 协议信息
         */
        private final String protocol;

        public ConnID(ServiceKey serviceKey, ClusterType clusterType, String host, int port, String protocol) {
            this.id = UUID.randomUUID().toString();
            if (null != serviceKey) {
                this.serviceKey = serviceKey;
            } else {
                this.serviceKey = new ServiceKey(DefaultValues.DEFAULT_SYSTEM_NAMESPACE,
                        DefaultValues.DEFAULT_BUILTIN_DISCOVER);
            }
            this.clusterType = clusterType;
            this.host = host;
            this.port = port;
            this.protocol = protocol;
        }

        public String getId() {
            return id;
        }

        public ServiceKey getServiceKey() {
            return serviceKey;
        }

        public ClusterType getClusterType() {
            return clusterType;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getProtocol() {
            return protocol;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConnID)) {
                return false;
            }
            ConnID connID = (ConnID) o;
            return port == connID.port &&
                    Objects.equals(id, connID.id) &&
                    Objects.equals(serviceKey, connID.serviceKey) &&
                    clusterType == connID.clusterType &&
                    Objects.equals(host, connID.host) &&
                    Objects.equals(protocol, connID.protocol);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, serviceKey, clusterType, host, port, protocol);
        }

        @Override
        public String toString() {
            return "ConnID{" +
                    "id='" + id + '\'' +
                    ", clusterType=" + clusterType +
                    ", host='" + host + '\'' +
                    ", port=" + port +
                    '}';
        }
    }
}
