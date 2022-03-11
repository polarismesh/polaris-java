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

import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse;
import com.tencent.polaris.client.pb.ServiceProto.Instance;
import com.tencent.polaris.client.pb.ServiceProto.Service;
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
        try {
            if (serverEvent.getValue() instanceof DiscoverResponse) {
                DiscoverResponse discoverResponse = (DiscoverResponse) serverEvent.getValue();
                DiscoverResponse.Builder newDiscoverResponseBuilder = DiscoverResponse.newBuilder()
                        .mergeFrom(discoverResponse);
                CompositeConnector connector = (CompositeConnector) serverConnector;
                if (EventType.INSTANCE.equals(serviceEventKey.getEventType())) {
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
                    List<Instance> polarisInstanceList = discoverResponse.getInstancesList();
                    for (DefaultInstance i : extendInstanceList) {
                        boolean needAdd = true;
                        for (Instance j : polarisInstanceList) {
                            if (i.getHost().equals(j.getHost().getValue()) && i.getPort() == j.getPort()
                                    .getValue()) {
                                needAdd = false;
                                break;
                            }
                        }
                        if (needAdd) {
                            Instance.Builder instanceBuilder = Instance.newBuilder()
                                    .setNamespace(StringValue.of(serviceEventKey.getNamespace()))
                                    .setService(StringValue.of(i.getService()))
                                    .setHost(StringValue.of(i.getHost()))
                                    .setPort(UInt32Value.of(i.getPort()))
                                    .setHealthy(BoolValue.of(true));
                            if (StringUtils.isNotBlank(i.getId())) {
                                instanceBuilder.setId(StringValue.of(i.getId()));
                            }
                            newDiscoverResponseBuilder.addInstances(instanceBuilder.build());
                        }
                    }
                    if (!newDiscoverResponseBuilder.getInstancesList().isEmpty()) {
                        serverEvent.setError(null);
                    }
                } else if (EventType.SERVICE.equals(serviceEventKey.getEventType())) {
                    // Get instance information list except polaris.
                    List<ServiceInfo> extendServiceList = new ArrayList<>();
                    for (DestroyableServerConnector sc : connector.getServerConnectors()) {
                        if (!DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(sc.getName())) {
                            Services services = sc.syncGetServices(this);
                            if (extendServiceList.isEmpty()) {
                                extendServiceList.addAll(services.getServices());
                            } else {
                                // TODO 多数据源合并去重
                            }
                        }
                    }
                    // Merge service information list
                    List<Service> polarisServiceList = discoverResponse.getServicesList();
                    for (ServiceInfo i : extendServiceList) {
                        boolean needAdd = true;
                        for (Service j : polarisServiceList) {
                            if (i.getService().equals(j.getName().getValue())) {
                                needAdd = false;
                                break;
                            }
                        }
                        if (needAdd) {
                            Service service = Service.newBuilder()
                                    .setNamespace(StringValue.of(serviceEventKey.getNamespace()))
                                    .setName(StringValue.of(i.getService()))
                                    .build();
                            newDiscoverResponseBuilder.addServices(service);
                        }
                    }
                    if (!newDiscoverResponseBuilder.getServicesList().isEmpty()) {
                        serverEvent.setError(null);
                    }
                }
                serverEvent.setValue(newDiscoverResponseBuilder.build());
            }
        } catch (PolarisException e) {
            LOG.error("Merge other server response failed.", e);
            serverEvent.setError(e);
        } catch (Throwable throwable) {
            LOG.error("Merge other server response failed.", throwable);
            serverEvent.setError(new PolarisException(ErrorCode.INTERNAL_ERROR));
        }
        if (null == serverEvent.getError()) {
            successUpdates.addAndGet(1);
        }
        return getEventHandler().onEventUpdate(serverEvent);
    }
}
