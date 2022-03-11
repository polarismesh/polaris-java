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

package com.tencent.polaris.plugins.connector.common;

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.plugin.server.EventHandler;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Status;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Type;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract service update task class.
 *
 * @author Haotian Zhang
 */
public abstract class ServiceUpdateTask implements Runnable, Comparable<ServiceUpdateTask> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceUpdateTask.class);

    protected final ServiceEventHandler serviceEventHandler;
    protected final DestroyableServerConnector serverConnector;
    protected final AtomicReference<ClusterType> targetClusterType = new AtomicReference<>();
    protected final AtomicReference<Type> taskType = new AtomicReference<>();
    protected final AtomicReference<Status> taskStatus = new AtomicReference<>();
    protected final ServiceEventKey serviceEventKey;
    protected final AtomicLong lastUpdateTime = new AtomicLong(0);
    protected final AtomicLong successUpdates = new AtomicLong(0);
    private final long refreshIntervalMs;
    private final EventHandler eventHandler;

    public ServiceUpdateTask(ServiceEventHandler handler, DestroyableServerConnector connector) {
        this.serviceEventHandler = handler;
        this.serverConnector = connector;
        this.serviceEventKey = handler.getServiceEventKey();
        this.refreshIntervalMs = handler.getRefreshIntervalMs() + (new Random()).nextInt(1000);
        this.eventHandler = handler.getEventHandler();
        taskType.set(Type.FIRST);
        taskStatus.set(Status.READY);
        targetClusterType.set(handler.getTargetCluster());
    }

    @Override
    public int compareTo(ServiceUpdateTask o) {
        return taskType.get().ordinal() - o.taskType.get().ordinal();
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
        serverConnector.retryServiceUpdateTask(this);
    }

    @Override
    public void run() {
        try {
            execute();
        } catch (Throwable throwable) {
            handle(throwable);
        }
    }

    /**
     * Business to be executed.
     *
     * @throws Throwable
     */
    protected abstract void execute() throws Throwable;

    /**
     * Throwable to be handled.
     *
     * @param throwable
     */
    protected abstract void handle(Throwable throwable);

    public abstract boolean notifyServerEvent(ServerEvent serverEvent);

    /**
     * 加入调度队列
     */
    public void addUpdateTaskSet() {
        if (taskType.compareAndSet(Type.FIRST, Type.LONG_RUNNING)) {
            targetClusterType.set(ClusterType.SERVICE_DISCOVER_CLUSTER);
            serverConnector.addLongRunningTask(this);
            LOG.info("[ServerConnector]task for service {} has been scheduled updated", this);
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
}
