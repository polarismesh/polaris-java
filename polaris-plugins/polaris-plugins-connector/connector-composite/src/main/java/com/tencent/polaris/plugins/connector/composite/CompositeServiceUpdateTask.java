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
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceInstancesResponse;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Status;
import com.tencent.polaris.plugins.connector.composite.zero.InstanceListMeta;
import com.tencent.polaris.plugins.connector.grpc.GrpcServiceUpdateTask;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto.DiscoverResponse;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto.Instance;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto.Service;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_GRPC;

/**
 * Scheduled task for updating service information.
 *
 * @author Haotian Zhang
 */
public class CompositeServiceUpdateTask extends ServiceUpdateTask {

    private static final Logger LOG = LoggerFactory.getLogger(
            CompositeServiceUpdateTask.class);

    private final InstanceListMeta instanceListMeta = new InstanceListMeta();

    public CompositeServiceUpdateTask(ServiceEventHandler handler, DestroyableServerConnector connector) {
        super(handler, connector);
    }

    @Override
    protected void execute() {
        CompositeConnector connector = (CompositeConnector) serverConnector;
        for (DestroyableServerConnector sc : connector.getServerConnectors()) {
            if (SERVER_CONNECTOR_GRPC.equals(sc.getName()) && sc.isDiscoveryEnable()) {
                GrpcServiceUpdateTask grpcServiceUpdateTask = new GrpcServiceUpdateTask(serviceEventHandler, sc);
                grpcServiceUpdateTask.execute(this);
                return;
            }
        }
        boolean svcDeleted = this.notifyServerEvent(
                new ServerEvent(serviceEventKey, DiscoverResponse.newBuilder().build(), null));
        if (!svcDeleted) {
            this.addUpdateTaskSet();
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
        boolean shouldTest = false;
        try {
            if (serverEvent.getValue() instanceof DiscoverResponse) {
                DiscoverResponse discoverResponse = (DiscoverResponse) serverEvent.getValue();
                DiscoverResponse.Builder newDiscoverResponseBuilder = DiscoverResponse.newBuilder()
                        .mergeFrom(discoverResponse);
                CompositeConnector connector = (CompositeConnector) serverConnector;

                if (EventType.INSTANCE.equals(serviceEventKey.getEventType())) {
                    // Get instance information list except polaris.
                    List<DefaultInstance> extendInstanceList = new ArrayList<>();
                    CompositeRevision compositeRevision = new CompositeRevision();
                    compositeRevision.setRevision(SERVER_CONNECTOR_GRPC,
                            discoverResponse.getService().getRevision().getValue());
                    for (DestroyableServerConnector sc : connector.getServerConnectors()) {
                        if (!SERVER_CONNECTOR_GRPC.equals(sc.getName()) && sc.isDiscoveryEnable()) {
                            ServiceInstancesResponse serviceInstancesResponse = sc.syncGetServiceInstances(this);
                            if (serviceInstancesResponse != null) {
                                compositeRevision.setRevision(sc.getName(), serviceInstancesResponse.getRevision());
                                extendInstanceList.addAll(serviceInstancesResponse.getServiceInstanceList());
                            }
                        }
                    }

                    // Merge instance information list if needed.
                    if (CollectionUtils.isNotEmpty(extendInstanceList)) {
                        // 由于合并多个发现结果会修改版本号，所以将 polaris 的版本号保存一份
                        serverEvent.setPolarisRevision(discoverResponse.getService().getRevision().getValue());
                        if (discoverResponse.getCode().getValue() == ServerCodes.DATA_NO_CHANGE) {
                            // 将 NO_CHANGE 响应转为 SUCCESS 响应，用于多个发现结果的合并
                            newDiscoverResponseBuilder
                                    .setCode(UInt32Value.newBuilder().setValue(ServerCodes.EXECUTE_SUCCESS).build());
                            Object value = getEventHandler().getValue();
                            if (value instanceof ServiceInstancesByProto) {
                                // Add local cache when DATA_NO_CHANGE
                                ServiceInstancesByProto cacheValue = (ServiceInstancesByProto) value;
                                newDiscoverResponseBuilder.clearInstances();
                                newDiscoverResponseBuilder.addAllInstances(cacheValue.getOriginInstancesList());
                            }
                        }
                        List<Instance> polarisInstanceList = newDiscoverResponseBuilder.getInstancesList();
                        List<Instance.Builder> finalInstanceBuilderList = new ArrayList<>();
                        for (Instance i : polarisInstanceList) {
                            finalInstanceBuilderList.add(Instance.newBuilder().mergeFrom(i));
                        }
                        for (DefaultInstance e : extendInstanceList) {
                            boolean needAdd = true;
                            // 看看北极星的实例列表是否存在
                            for (Instance.Builder f : finalInstanceBuilderList) {
                                if (StringUtils.equals(e.getHost(), f.getHost().getValue()) && e.getPort() == f.getPort()
                                        .getValue()) {
                                    // 北极星服务实例状态和拓展服务实例状态，有一个可用即可用
                                    f.setHealthy(BoolValue.of(e.isHealthy() || f.getHealthy().getValue()));
                                    f.setIsolate(BoolValue.of(e.isIsolated() && f.getIsolate().getValue()));
                                    needAdd = false;
                                    break;
                                }
                            }
                            if (needAdd) {
                                Instance.Builder instanceBuilder = Instance.newBuilder()
                                        .setNamespace(StringValue.of(serviceEventKey.getNamespace()))
                                        .setService(StringValue.of(e.getService()))
                                        .setHost(StringValue.of(e.getHost()))
                                        .setPort(UInt32Value.of(e.getPort()))
                                        .setHealthy(BoolValue.of(e.isHealthy()))
                                        .setIsolate(BoolValue.of(e.isIsolated()));
                                // set Id
                                if (StringUtils.isNotBlank(e.getId())) {
                                    instanceBuilder.setId(StringValue.of(e.getId()));
                                } else {
                                    String id =
                                            e.getService() + "-" + e.getHost().replace(".", "-") + "-" + e.getPort();
                                    instanceBuilder.setId(StringValue.of(id));
                                    LOG.info("Instance with name {} host {} port {} doesn't have id.", e.getService()
                                            , e.getHost(), e.getPort());
                                }
                                // set location
                                ModelProto.Location.Builder locationBuilder = ModelProto.Location.newBuilder();
                                if (StringUtils.isNotBlank(e.getRegion())) {
                                    locationBuilder.setRegion(StringValue.of(e.getRegion()));
                                }
                                if (StringUtils.isNotBlank(e.getZone())) {
                                    locationBuilder.setZone(StringValue.of(e.getZone()));
                                }
                                if (StringUtils.isNotBlank(e.getCampus())) {
                                    locationBuilder.setCampus(StringValue.of(e.getCampus()));
                                }
                                instanceBuilder.setLocation(locationBuilder.build());
                                // set metadata
                                if (CollectionUtils.isNotEmpty(e.getMetadata())) {
                                    instanceBuilder.putAllMetadata(e.getMetadata());
                                }
                                // set Protocol
                                if (StringUtils.isNotBlank(e.getProtocol())) {
                                    instanceBuilder.setProtocol(StringValue.of(e.getProtocol()));
                                }
                                // set Version
                                if (StringUtils.isNotBlank(e.getVersion())) {
                                    instanceBuilder.setVersion(StringValue.of(e.getVersion()));
                                }
                                finalInstanceBuilderList.add(instanceBuilder);
                            }
                        }
                        List<Instance> finalInstanceList = new ArrayList<>();
                        for (Instance.Builder i : finalInstanceBuilderList) {
                            finalInstanceList.add(i.build());
                        }
                        Service.Builder newServiceBuilder = Service.newBuilder()
                                .mergeFrom(newDiscoverResponseBuilder.getService());
                        newServiceBuilder.setRevision(StringValue.of(compositeRevision.getCompositeRevisionString()));
                        newDiscoverResponseBuilder.setService(newServiceBuilder.build());
                        newDiscoverResponseBuilder.clearInstances();
                        newDiscoverResponseBuilder.addAllInstances(finalInstanceList);
                    }

                    // zero instance protect.
                    if (!newDiscoverResponseBuilder.getInstancesList().isEmpty()) {
                        serverEvent.setError(null);
                    } else if (newDiscoverResponseBuilder.getCode().getValue() != ServerCodes.DATA_NO_CHANGE && connector.isZeroProtectionEnabled()) {
                        Object value = getEventHandler().getValue();
                        if (value instanceof ServiceInstancesByProto) {
                            ServiceInstancesByProto cacheValue = (ServiceInstancesByProto) value;
                            newDiscoverResponseBuilder.setCode(UInt32Value.of(ServerCodes.DATA_NO_CHANGE));
                            Service.Builder newServiceBuilder = Service.newBuilder()
                                    .mergeFrom(newDiscoverResponseBuilder.getService());
                            newServiceBuilder.setRevision(StringValue.of(cacheValue.getRevision()));
                            newDiscoverResponseBuilder.setService(newServiceBuilder.build());
                            newDiscoverResponseBuilder.clearInstances();
                            newDiscoverResponseBuilder.addAllInstances(cacheValue.getOriginInstancesList());
                            if (CollectionUtils.isNotEmpty(cacheValue.getOriginInstancesList())) {
                                shouldTest = true;
                            }
                            serverEvent.setError(null);
                        }
                    }
                    instanceListMeta.setLastRevision(newDiscoverResponseBuilder.getService().getRevision().getValue());
                } else if (EventType.SERVICE.equals(serviceEventKey.getEventType())) {
                    // Get service information list except polaris.
                    List<ServiceInfo> extendServiceList = new ArrayList<>();
                    for (DestroyableServerConnector sc : connector.getServerConnectors()) {
                        if (!SERVER_CONNECTOR_GRPC.equals(sc.getName()) && sc.isDiscoveryEnable()) {
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
                DiscoverResponse response = newDiscoverResponseBuilder.build();
                if (EventType.INSTANCE.equals(serviceEventKey.getEventType()) && shouldTest) {
                    connector.submitTestConnectivityTask(this, response);
                }
                serverEvent.setValue(response);
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

    public boolean notifyServerEventWithRevisionChecking(ServerEvent serverEvent, String revision) {
        if (serverEvent.getValue() instanceof DiscoverResponse) {
            if (StringUtils.equals(revision, instanceListMeta.getLastRevision())) {
                return notifyServerEvent(serverEvent);
            }
        }
        return false;
    }
}
