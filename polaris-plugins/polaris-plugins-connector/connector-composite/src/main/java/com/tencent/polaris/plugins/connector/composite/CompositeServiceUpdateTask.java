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

package com.tencent.polaris.plugins.connector.composite;

import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse;
import com.tencent.polaris.client.pb.ServiceProto.Instance;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Status;
import com.tencent.polaris.plugins.connector.grpc.GrpcServiceUpdateTask;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduled task for updating service information.
 *
 * @author Haotian Zhang
 */
public class CompositeServiceUpdateTask extends ServiceUpdateTask {

    private static final Logger LOG = LoggerFactory.getLogger(
            CompositeServiceUpdateTask.class);

    public CompositeServiceUpdateTask(ServiceEventHandler handler, DestroyableServerConnector connector) {
        super(handler, connector);
    }

    @Override
    protected void execute() {
        CompositeConnector connector = (CompositeConnector) serverConnector;
        for (DestroyableServerConnector sc : connector.getServerConnectors()) {
            if (DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(sc.getName())) {
                GrpcServiceUpdateTask grpcServiceUpdateTask = new GrpcServiceUpdateTask(serviceEventHandler, sc);
                grpcServiceUpdateTask.execute(this);
            }
        }
    }

    @Override
    protected void handle(Throwable throwable) {
        LOG.error("Composite service task execute error.", throwable);
    }

    @Override
    public boolean notifyServerEvent(ServerEvent serverEvent) {
        taskStatus.compareAndSet(Status.RUNNING, Status.READY);
        lastUpdateTime.set(System.currentTimeMillis());
        if (null == serverEvent.getError()) {
            successUpdates.addAndGet(1);
        }
        if (serverEvent.getValue() instanceof DiscoverResponse) {
            DiscoverResponse discoverResponse = (DiscoverResponse) serverEvent.getValue();
            CompositeConnector connector = (CompositeConnector) serverConnector;
            if (EventType.INSTANCE.equals(serviceEventKey.getEventType())) {
                List<Instance> polarisInstanceList = discoverResponse.getInstancesList();
                // Get instance information list except polaris.
                List<DefaultInstance> extendInstanceList = new ArrayList<>();
                for (DestroyableServerConnector sc : connector.getServerConnectors()) {
                    if (!DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(sc.getName())) {
                        List<DefaultInstance> instanceList = sc.syncGetServiceInstances(this);
                        if (extendInstanceList.isEmpty()) {
                            extendInstanceList.addAll(instanceList);
                        } else {
                            // TODO 多数据源合并去重
                        }
                    }
                }
                // Merge instance information list
                for (DefaultInstance i : extendInstanceList) {
                    for (Instance j : polarisInstanceList) {
                        if (i.getHost().equals(j.getHost().getValue()) && i.getPort() == j.getPort().getValue()) {
                            break;
                        }
                    }
                    polarisInstanceList.add(Instance.newBuilder()
                            .setService(StringValue.of(i.getService()))
                            .setHost(StringValue.of(i.getHost()))
                            .setPort(UInt32Value.of(i.getPort()))
                            .build());
                }
            } else if (EventType.SERVICE.equals(serviceEventKey.getEventType())) {
                // TODO service information update
//                Services polarisServices = discoverResponse.getServicesList();
//                for (DestroyableServerConnector sc : connector.getServerConnectors()) {
//                    if (!DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(sc.getName())) {
//
//                    }
//                }
            }
        }
        return getEventHandler().onEventUpdate(serverEvent);
    }
}
