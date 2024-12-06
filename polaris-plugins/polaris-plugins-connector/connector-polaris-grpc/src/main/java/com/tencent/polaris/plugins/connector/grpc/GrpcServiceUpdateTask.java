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
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Status;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Type;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class GrpcServiceUpdateTask extends ServiceUpdateTask {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceUpdateTask.class);

    private final AtomicLong msgSendTime = new AtomicLong(0);
    private final AtomicLong totalRequests = new AtomicLong(0);

    private ServerConnectorConfigImpl connectorConfig;

    public GrpcServiceUpdateTask(ServiceEventHandler handler,
                                 DestroyableServerConnector connector) {
        super(handler, connector);
        this.connectorConfig = ((GrpcConnector) connector).getConnectorConfig();
    }

    @Override
    public void execute() {
        execute(this);
    }

    @Override
    public void execute(ServiceUpdateTask serviceUpdateTask) {
        try {
            if (serviceUpdateTask.getTaskType() == Type.FIRST) {
                LOG.info("[ServerConnector]start to run first task {}", serviceUpdateTask);
            } else {
                LOG.debug("[ServerConnector]start to run task {}", serviceUpdateTask);
            }
            GrpcConnector grpcConnector = (GrpcConnector) serverConnector;
            ConnectionManager connectionManager = grpcConnector.getConnectionManager();
            ClusterType clusterType = targetClusterType.get();
            boolean clusterReady = connectionManager.checkReady(clusterType);
            if (!clusterReady) {
                //没有ready，就重试
                LOG.info("{} service is not ready", clusterType);
                grpcConnector.retryServiceUpdateTask(serviceUpdateTask);
                return;
            }
            if (grpcConnector.isDestroyed()) {
                LOG.info("{} grpc connection is destroyed", clusterType);
                grpcConnector.retryServiceUpdateTask(serviceUpdateTask);
                return;
            }
            AtomicReference<SpecStreamClient> streamClientAtomicReference = grpcConnector.getStreamClient(clusterType);
            SpecStreamClient specStreamClient = streamClientAtomicReference.get();
            boolean available = checkStreamClientAvailable(specStreamClient, serviceUpdateTask);
            if (!available) {
                if (null != specStreamClient) {
                    specStreamClient.closeStream(true);
                }
                LOG.debug("[ServerConnector]start to get connection for task {}", serviceUpdateTask);
                Connection connection = null;
                try {
                    connection = connectionManager.getConnection(GrpcUtil.OP_KEY_DISCOVER, clusterType);
                } catch (PolarisException e) {
                    LOG.error("[ServerConnector]fail to get connection to {}", clusterType, e);
                }
                if (null == connection) {
                    LOG.error("[ServerConnector]get null connection for {}", serviceUpdateTask);
                    grpcConnector.retryServiceUpdateTask(serviceUpdateTask);
                    return;
                }
                specStreamClient = new SpecStreamClient(connectorConfig, connection, grpcConnector.getConnectionIdleTimeoutMs(),
                        serviceUpdateTask);
                streamClientAtomicReference.set(specStreamClient);
                LOG.info("[ServerConnector]success to create stream client for task {}", serviceUpdateTask);
            }
            msgSendTime.set(System.currentTimeMillis());
            totalRequests.addAndGet(1);
            specStreamClient.sendRequest(serviceUpdateTask);
        } catch (Exception e) {
            LOG.error("[ServerConnector]fail to run discover task {}", this, e);
        }
    }

    @Override
    protected void handle(Throwable throwable) {
        LOG.error("Grpc service task execute error.", throwable);
    }

    private boolean checkStreamClientAvailable(SpecStreamClient streamClient, ServiceUpdateTask serviceUpdateTask) {
        if (null == streamClient) {
            return false;
        }
        return streamClient.checkAvailable(serviceUpdateTask);
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "GrpcServiceUpdateTask{" +
                "taskType=" + taskType.get() +
                ", taskStatus=" + taskStatus.get() +
                ", serviceEventKey=" + serviceEventKey +
                ", targetClusterType=" + targetClusterType.get() +
                '}';
    }

    public boolean notifyServerEvent(ServerEvent serverEvent) {
        taskStatus.compareAndSet(Status.RUNNING, Status.READY);
        lastUpdateTime.set(System.currentTimeMillis());
        if (null == serverEvent.getError()) {
            successUpdates.addAndGet(1);
        }
        return getEventHandler().onEventUpdate(serverEvent);
    }
}
