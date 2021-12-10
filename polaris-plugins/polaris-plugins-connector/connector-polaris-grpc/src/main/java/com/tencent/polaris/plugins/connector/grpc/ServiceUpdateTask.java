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

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.server.EventHandler;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceUpdateTask implements Runnable, Comparable<ServiceUpdateTask> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceUpdateTask.class);

    private final GrpcConnector grpcConnector;

    @Override
    public int compareTo(ServiceUpdateTask o) {
        return taskType.get().ordinal() - o.taskType.get().ordinal();
    }


    public enum Type {
        /**
         * 首次调度
         */
        FIRST,
        /**
         * 长稳调度
         */
        LONG_RUNNING,
        /**
         * 已经销毁
         */
        TERMINATED
    }

    private final AtomicReference<Type> taskType = new AtomicReference<>();

    public enum Status {
        /**
         * 调度中
         */
        RUNNING,
        /**
         * 已经就绪
         */
        READY,
    }

    private final AtomicReference<Status> taskStatus = new AtomicReference<>();

    private final long refreshIntervalMs;

    private final EventHandler eventHandler;

    private final ServiceEventKey serviceEventKey;

    private final AtomicLong msgSendTime = new AtomicLong(0);

    private final AtomicLong lastUpdateTime = new AtomicLong(0);

    private final AtomicLong totalRequests = new AtomicLong(0);

    private final AtomicLong successUpdates = new AtomicLong(0);

    private final AtomicReference<ClusterType> targetClusterType = new AtomicReference<>();

    public ServiceUpdateTask(ServiceEventHandler handler, GrpcConnector connector) {
        this.grpcConnector = connector;
        this.serviceEventKey = handler.getServiceEventKey();
        this.refreshIntervalMs = handler.getRefreshIntervalMs() + (new Random()).nextInt(1000);
        this.eventHandler = handler.getEventHandler();
        taskType.set(Type.FIRST);
        taskStatus.set(Status.READY);
        targetClusterType.set(handler.getTargetCluster());
    }

    public ServiceEventKey getServiceEventKey() {
        return serviceEventKey;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public ClusterType getTargetClusterType() {
        return targetClusterType.get();
    }

    public boolean setType(Type last, Type current) {
        return taskType.compareAndSet(last, current);
    }

    public boolean setStatus(Status last, Status current) {
        return taskStatus.compareAndSet(last, current);
    }

    public Type getTaskType() {
        return taskType.get();
    }

    public void retry() {
        grpcConnector.retryServiceUpdateTask(this);
    }

    @Override
    public void run() {
        if (getTaskType() == Type.FIRST) {
            LOG.info("[ServerConnector]start to run first task {}", this);
        } else {
            LOG.debug("[ServerConnector]start to run task {}", this);
        }
        ConnectionManager connectionManager = grpcConnector.getConnectionManager();
        ClusterType clusterType = targetClusterType.get();
        boolean clusterReady = connectionManager.checkReady(clusterType);
        if (!clusterReady) {
            //没有ready，就重试
            LOG.info("{} service is not ready", clusterType);
            grpcConnector.retryServiceUpdateTask(this);
            return;
        }
        if (grpcConnector.isDestroyed()) {
            LOG.info("{} grpc connection is destroyed", clusterType);
            grpcConnector.retryServiceUpdateTask(this);
            return;
        }
        AtomicReference<SpecStreamClient> streamClientAtomicReference = grpcConnector.getStreamClient(clusterType);
        SpecStreamClient specStreamClient = streamClientAtomicReference.get();
        boolean available = checkStreamClientAvailable(specStreamClient);
        if (!available) {
            LOG.debug("[ServerConnector]start to get connection for task {}", this);
            Connection connection = null;
            try {
                connection = connectionManager.getConnection(GrpcUtil.OP_KEY_DISCOVER, clusterType);
            } catch (PolarisException e) {
                LOG.error("[ServerConnector]fail to get connection to {}", clusterType, e);
            }
            if (null == connection) {
                LOG.error("[ServerConnector]get null connection for {}", this);
                grpcConnector.retryServiceUpdateTask(this);
                return;
            }
            specStreamClient = new SpecStreamClient(connection, grpcConnector.getConnectionIdleTimeoutMs(), this);
            streamClientAtomicReference.set(specStreamClient);
            LOG.info("[ServerConnector]success to create stream client for task {}", this);
        }
        msgSendTime.set(System.currentTimeMillis());
        totalRequests.addAndGet(1);
        specStreamClient.sendRequest(this);
    }

    private boolean checkStreamClientAvailable(SpecStreamClient streamClient) {
        if (null == streamClient) {
            return false;
        }
        return streamClient.checkAvailable(this);
    }

    /**
     * 加入调度队列
     */
    public void addUpdateTaskSet() {
        if (taskType.compareAndSet(Type.FIRST, Type.LONG_RUNNING)) {
            targetClusterType.set(ClusterType.SERVICE_DISCOVER_CLUSTER);
            grpcConnector.addLongRunningTask(this);
            LOG.debug("[ServerConnector]task for service {} has been scheduled updated", this);
        }
    }

    /**
     * 判断是否需要更新
     *
     * @return boolean
     */
    public boolean needUpdate() {
        if (taskType.get() != Type.LONG_RUNNING || taskStatus.get() != Status.READY) {
            return false;
        }
        long nowMs = System.currentTimeMillis();
        return nowMs - lastUpdateTime.get() >= refreshIntervalMs;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ServiceUpdateTask{" +
                "taskType=" + taskType.get() +
                ", taskStatus=" + taskStatus.get() +
                ", serviceEventKey=" + serviceEventKey +
                ", targetClusterType=" + targetClusterType.get() +
                '}';
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public boolean notifyServerEvent(ServerEvent serverEvent) {
        taskStatus.compareAndSet(Status.RUNNING, Status.READY);
        lastUpdateTime.set(System.currentTimeMillis());
        if (null == serverEvent.getError()) {
            successUpdates.addAndGet(1);
        }
        return eventHandler.onEventUpdate(serverEvent);
    }
}
