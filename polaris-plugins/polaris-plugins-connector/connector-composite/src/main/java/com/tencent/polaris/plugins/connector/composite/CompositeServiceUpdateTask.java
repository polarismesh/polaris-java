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
import com.tencent.polaris.client.pojo.ServicesByProto;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceInstancesResponse;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Status;
import com.tencent.polaris.plugins.connector.composite.zero.InstanceListMeta;
import com.tencent.polaris.plugins.connector.consul.ConsulServiceUpdateTask;
import com.tencent.polaris.plugins.connector.grpc.GrpcServiceUpdateTask;
import com.tencent.polaris.plugins.connector.nacos.NacosServiceUpdateTask;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto.DiscoverResponse;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto.Instance;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto.Service;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_CONSUL;
import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_GRPC;
import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_NACOS;
import static com.tencent.polaris.plugins.connector.common.constant.ConnectorConstant.ORDER_LIST;
import static com.tencent.polaris.plugins.connector.common.constant.ConnectorConstant.SERVER_CONNECTOR_TYPE;

/**
 * Scheduled task for updating service information.
 *
 * @author Haotian Zhang
 */
public class CompositeServiceUpdateTask extends ServiceUpdateTask {

    private static final Logger LOG = LoggerFactory.getLogger(
            CompositeServiceUpdateTask.class);

    private final InstanceListMeta instanceListMeta = new InstanceListMeta();

    private String mainConnectorType = SERVER_CONNECTOR_GRPC;

    private boolean ifMainConnectorTypeSet = false;

    private final Map<String, ServiceUpdateTask> subServiceUpdateTaskMap = new ConcurrentHashMap<>();

    /**
     * 多注册多发现并发公平锁。
     */
    private final ReentrantLock fairLock = new ReentrantLock(true);

    public CompositeServiceUpdateTask(ServiceEventHandler handler, DestroyableServerConnector connector) {
        super(handler, connector);
        CompositeConnector compositeConnector = (CompositeConnector) connector;
        for (DestroyableServerConnector sc : compositeConnector.getServerConnectors()) {
            if (SERVER_CONNECTOR_GRPC.equals(sc.getName()) && sc.isDiscoveryEnable()) {
                subServiceUpdateTaskMap.put(SERVER_CONNECTOR_GRPC, new GrpcServiceUpdateTask(serviceEventHandler, sc));
                mainConnectorType = SERVER_CONNECTOR_GRPC;
                ifMainConnectorTypeSet = true;
            }
            if (SERVER_CONNECTOR_CONSUL.equals(sc.getName()) && sc.isDiscoveryEnable()) {
                subServiceUpdateTaskMap.put(SERVER_CONNECTOR_CONSUL, new ConsulServiceUpdateTask(serviceEventHandler, sc));
                if (!ifMainConnectorTypeSet) {
                    mainConnectorType = sc.getName();
                    ifMainConnectorTypeSet = true;
                }
            }
            if (SERVER_CONNECTOR_NACOS.equals(sc.getName()) && sc.isDiscoveryEnable()) {
                subServiceUpdateTaskMap.put(SERVER_CONNECTOR_NACOS, new NacosServiceUpdateTask(serviceEventHandler, sc));
                if (!ifMainConnectorTypeSet) {
                    mainConnectorType = sc.getName();
                    ifMainConnectorTypeSet = true;
                }
            }
        }
    }

    @Override
    public boolean needUpdate() {
        boolean compositeNeedUpdate = super.needUpdate();
        boolean subNeedUpdate = false;
        for (ServiceUpdateTask serviceUpdateTask : subServiceUpdateTaskMap.values()) {
            subNeedUpdate = subNeedUpdate || serviceUpdateTask.needUpdate();
        }
        return compositeNeedUpdate && subNeedUpdate;
    }

    @Override
    public void execute() {
        boolean isServiceUpdateTaskExecuted = false;
        int subTaskSize = subServiceUpdateTaskMap.size();
        for (Map.Entry<String, ServiceUpdateTask> entry : subServiceUpdateTaskMap.entrySet()) {
            // TODO: check multi connector
            if (subTaskSize == 1 || canExecute(entry.getKey(), entry.getValue())) {
                isServiceUpdateTaskExecuted = true;
                entry.getValue().setStatus(ServiceUpdateTaskConstant.Status.READY, ServiceUpdateTaskConstant.Status.RUNNING);
                entry.getValue().execute(this);
            }
        }
        if (ifMainConnectorTypeSet && isServiceUpdateTaskExecuted
                && (StringUtils.equals(mainConnectorType, SERVER_CONNECTOR_GRPC)
                || (serviceEventKey.getEventType().equals(EventType.INSTANCE)
                || serviceEventKey.getEventType().equals(EventType.SERVICE)
                || serviceEventKey.getEventType().equals(EventType.ROUTING)
                || serviceEventKey.getEventType().equals(EventType.NEARBY_ROUTE_RULE)
                || serviceEventKey.getEventType().equals(EventType.LOSSLESS)
                || serviceEventKey.getEventType().equals(EventType.CIRCUIT_BREAKING)
                || serviceEventKey.getEventType().equals(EventType.RATE_LIMITING)
                || serviceEventKey.getEventType().equals(EventType.LANE_RULE)
                || serviceEventKey.getEventType().equals(EventType.BLOCK_ALLOW_RULE)
                || serviceEventKey.getEventType().equals(EventType.TRAFFIC_MIRRORING)
                || serviceEventKey.getEventType().equals(EventType.FAULT_INJECTION)))) {
            return;
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
        fairLock.lock();
        try {
            taskStatus.compareAndSet(Status.RUNNING, Status.READY);
            long currentTimeStamp = System.currentTimeMillis();
            lastUpdateTime.set(currentTimeStamp);
            LOG.debug("[CompositeServerConnector]task for service {} has been notified", this);
            String serverEventConnectorType = serverEvent.getConnectorType();
            ServiceUpdateTask subTask = subServiceUpdateTaskMap.get(serverEventConnectorType);
            if (subTask != null) {
                subTask.setStatus(Status.RUNNING, Status.READY);
                subTask.setLastUpdateTime(currentTimeStamp);
                LOG.debug("[CompositeServerConnector]subtask {} for service {} has been notified", serverEventConnectorType, this);
            }
            boolean shouldTest = false;
            if (null == serverEvent.getError()) {
                try {
                    if (serverEvent.getValue() instanceof DiscoverResponse) {
                        DiscoverResponse discoverResponse = (DiscoverResponse) serverEvent.getValue();
                        DiscoverResponse.Builder newDiscoverResponseBuilder = DiscoverResponse.newBuilder()
                                .mergeFrom(discoverResponse);
                        CompositeConnector connector = (CompositeConnector) serverConnector;

                        if (EventType.INSTANCE.equals(serviceEventKey.getEventType())) {
                            // load current instance map split by connector type.
                            CompositeRevision compositeRevision = new CompositeRevision();
                            Object value = getEventHandler().getValue();
                            Map<String, List<Instance>> instancesMap = new HashMap<>();
                            boolean isDiscoveryChanged = false;
                            if (taskType.get() == ServiceUpdateTaskConstant.Type.LONG_RUNNING && value instanceof ServiceInstancesByProto) {
                                ServiceInstancesByProto cacheValue = (ServiceInstancesByProto) value;
                                compositeRevision = CompositeRevision.of(cacheValue.getRevision());
                                List<Instance> oldInstancesList = cacheValue.getOriginInstancesList();
                                for (Instance oldInstance : oldInstancesList) {
                                    String serverConnectorType = oldInstance.getMetadataOrDefault(SERVER_CONNECTOR_TYPE, SERVER_CONNECTOR_GRPC);
                                    DestroyableServerConnector serverConnector = connector.getServerConnectorByType(serverConnectorType);
                                    if (serverConnector != null && serverConnector.isDiscoveryEnable()) {
                                        if (!instancesMap.containsKey(serverConnectorType)) {
                                            instancesMap.put(serverConnectorType, new ArrayList<>());
                                        }
                                        instancesMap.get(serverConnectorType).add(oldInstance);
                                    } else if (serverConnector != null && !serverConnector.isDiscoveryEnable()) {
                                        compositeRevision.removeRevision(serverConnectorType);
                                        isDiscoveryChanged = true;
                                        LOG.info("server connector {} is not enabled for discovery instance {}:{}",
                                                serverConnectorType, oldInstance.getHost().getValue(), oldInstance.getPort().getValue());
                                    }
                                }
                            }

                            // 按照事件来源更新对应的revision
                            compositeRevision.setRevision(serverEventConnectorType, discoverResponse.getService().getRevision().getValue());
                            List<Instance> serverEventInstancesList = instancesMap.computeIfAbsent(serverEventConnectorType, key -> new ArrayList<>());
                            if (LOG.isDebugEnabled()) {
                                String oldInstancesMapStr = convertToString(instancesMap);
                                LOG.debug("old instances map value: {}", oldInstancesMapStr);
                            }
                            if (newDiscoverResponseBuilder.getCode().getValue() != ServerCodes.DATA_NO_CHANGE) {
                                // 按照事件来源更新对应的列表
                                serverEventInstancesList.clear();
                                serverEventInstancesList.addAll(discoverResponse.getInstancesList());
                            } else if (newDiscoverResponseBuilder.getCode().getValue() == ServerCodes.DATA_NO_CHANGE && isDiscoveryChanged) {
                                newDiscoverResponseBuilder.setCode(UInt32Value.of(ServerCodes.EXECUTE_SUCCESS));
                            }
                            if (LOG.isDebugEnabled()) {
                                String newInstancesMapStr = convertToString(instancesMap);
                                LOG.debug("new instances map value: {}", newInstancesMapStr);
                            }
                            // 由于合并多个发现结果会修改版本号，所以将 polaris 的版本号保存一份
                            if (StringUtils.equals(serverEvent.getConnectorType(), SERVER_CONNECTOR_GRPC)) {
                                serverEvent.setPolarisRevision(discoverResponse.getService().getRevision().getValue());
                            }

                            // Get instance information list except polaris.
                            for (DestroyableServerConnector sc : connector.getServerConnectors()) {
                                if (!SERVER_CONNECTOR_GRPC.equals(sc.getName()) && sc.isDiscoveryEnable()) {
                                    ServiceInstancesResponse serviceInstancesResponse = sc.syncGetServiceInstances(this);
                                    if (serviceInstancesResponse != null) {
                                        compositeRevision.setRevision(sc.getName(), serviceInstancesResponse.getRevision());
                                        List<DefaultInstance> tempServiceInstanceList = serviceInstancesResponse.getServiceInstanceList();
                                        if (CollectionUtils.isNotEmpty(tempServiceInstanceList)) {
                                            // 将 NO_CHANGE 响应转为 SUCCESS 响应，用于多个发现结果的合并
                                            newDiscoverResponseBuilder
                                                    .setCode(UInt32Value.newBuilder().setValue(ServerCodes.EXECUTE_SUCCESS).build());
                                        }
                                        List<Instance> tempServerEventInstancesList = instancesMap.computeIfAbsent(sc.getName(), key -> new ArrayList<>());
                                        tempServerEventInstancesList.clear();
                                        for (DefaultInstance e : tempServiceInstanceList) {
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
                                            tempServerEventInstancesList.add(instanceBuilder.build());
                                        }
                                    }
                                }
                            }

                            // Merge instance information list if needed.
                            newDiscoverResponseBuilder.clearInstances();
                            List<Instance> finalInstanceList = new ArrayList<>();
                            for (String type : ORDER_LIST) {
                                List<Instance> instances = instancesMap.get(type);
                                if (CollectionUtils.isNotEmpty(instances)) {
                                    for (Instance newInstance : instances) {
                                        boolean needAdd = true;
                                        for (Instance existInstance : finalInstanceList) {
                                            if (StringUtils.equals(newInstance.getHost().getValue(), existInstance.getHost().getValue()) &&
                                                    Objects.equals(newInstance.getPort().getValue(), existInstance.getPort().getValue())) {
                                                needAdd = false;
                                                break;
                                            }
                                        }
                                        if (needAdd) {
                                            finalInstanceList.add(newInstance);
                                        }
                                    }
                                }
                            }
                            if (LOG.isDebugEnabled()) {
                                String finalInstanceListStr = convertToString(finalInstanceList);
                                LOG.debug("final instance list value: {}", finalInstanceListStr);
                            }
                            Service.Builder newServiceBuilder = Service.newBuilder()
                                    .mergeFrom(newDiscoverResponseBuilder.getService());
                            if (newDiscoverResponseBuilder.getService() != null) {
                                if (StringUtils.isBlank(newDiscoverResponseBuilder.getService().getNamespace().getValue())) {
                                    newServiceBuilder.setNamespace(StringValue.of(serviceEventKey.getNamespace()));
                                }
                                if (StringUtils.isBlank(newDiscoverResponseBuilder.getService().getName().getValue())) {
                                    newServiceBuilder.setName(StringValue.of(serviceEventKey.getService()));
                                }
                            }
                            newServiceBuilder.setRevision(StringValue.of(compositeRevision.getCompositeRevisionString()));
                            newDiscoverResponseBuilder.setService(newServiceBuilder.build());
                            newDiscoverResponseBuilder.addAllInstances(finalInstanceList);

                            // zero instance protect.
                            if (!newDiscoverResponseBuilder.getInstancesList().isEmpty()) {
                                serverEvent.setError(null);
                            } else if (newDiscoverResponseBuilder.getCode().getValue() != ServerCodes.DATA_NO_CHANGE && connector.isZeroProtectionEnabled()) {
                                if (value instanceof ServiceInstancesByProto) {
                                    ServiceInstancesByProto cacheValue = (ServiceInstancesByProto) value;
                                    newDiscoverResponseBuilder.setCode(UInt32Value.of(ServerCodes.DATA_NO_CHANGE));
                                    newServiceBuilder = Service.newBuilder()
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
                            // load current instance map split by connector type.
                            CompositeRevision compositeRevision = new CompositeRevision();
                            Object value = getEventHandler().getValue();
                            Map<String, List<Service>> servicesMap = new HashMap<>();
                            boolean isDiscoveryChanged = false;
                            if (taskType.get() == ServiceUpdateTaskConstant.Type.LONG_RUNNING && value instanceof ServicesByProto) {
                                ServicesByProto cacheValue = (ServicesByProto) value;
                                compositeRevision = CompositeRevision.of(cacheValue.getRevision());
                                List<Service> oldServiceList = cacheValue.getOriginServicesList();
                                for (Service oldService : oldServiceList) {
                                    String serverConnectorType = oldService.getMetadataOrDefault(SERVER_CONNECTOR_TYPE, SERVER_CONNECTOR_GRPC);
                                    DestroyableServerConnector serverConnector = connector.getServerConnectorByType(serverConnectorType);
                                    if (serverConnector != null && serverConnector.isDiscoveryEnable()) {
                                        if (!servicesMap.containsKey(serverConnectorType)) {
                                            servicesMap.put(serverConnectorType, new ArrayList<>());
                                        }
                                        servicesMap.get(serverConnectorType).add(oldService);
                                    } else if (serverConnector != null && !serverConnector.isDiscoveryEnable()) {
                                        compositeRevision.removeRevision(serverConnectorType);
                                        isDiscoveryChanged = true;
                                        LOG.info("server connector {} is not enabled for discovery service {}", serverConnectorType, oldService);
                                    }
                                }
                            }

                            // 按照事件来源更新对应的revision
                            compositeRevision.setRevision(serverEventConnectorType, discoverResponse.getService().getRevision().getValue());
                            // 按照事件来源更新对应的列表
                            List<Service> serverEventServicesList = servicesMap.computeIfAbsent(serverEventConnectorType, key -> new ArrayList<>());
                            if (newDiscoverResponseBuilder.getCode().getValue() != ServerCodes.DATA_NO_CHANGE) {
                                serverEventServicesList.clear();
                                serverEventServicesList.addAll(discoverResponse.getServicesList());
                            } else if (newDiscoverResponseBuilder.getCode().getValue() == ServerCodes.DATA_NO_CHANGE && isDiscoveryChanged) {
                                newDiscoverResponseBuilder.setCode(UInt32Value.of(ServerCodes.EXECUTE_SUCCESS));
                            }
                            // 由于合并多个发现结果会修改版本号，所以将 polaris 的版本号保存一份
                            if (StringUtils.equals(serverEvent.getConnectorType(), SERVER_CONNECTOR_GRPC)) {
                                serverEvent.setPolarisRevision(discoverResponse.getService().getRevision().getValue());
                            }

                            // Get service information list except polaris.
                            for (DestroyableServerConnector sc : connector.getServerConnectors()) {
                                if (!SERVER_CONNECTOR_GRPC.equals(sc.getName()) && sc.isDiscoveryEnable()) {
                                    Services services = sc.syncGetServices(this);
                                    if (services != null) {
                                        compositeRevision.setRevision(sc.getName(), services.getRevision());
                                        List<ServiceInfo> tempServiceList = services.getServices();
                                        List<Service> tempServerEventServicesList = servicesMap.computeIfAbsent(sc.getName(), key -> new ArrayList<>());
                                        tempServerEventServicesList.clear();
                                        for (ServiceInfo serviceInfo : tempServiceList) {
                                            Service service = Service.newBuilder()
                                                    .setNamespace(StringValue.of(serviceEventKey.getNamespace()))
                                                    .setName(StringValue.of(serviceInfo.getService()))
                                                    .build();
                                            tempServerEventServicesList.add(service);
                                        }
                                    }
                                }
                            }
                            // Merge service information list if needed.
                            newDiscoverResponseBuilder.clearServices();
                            List<Service> finalServiceList = new ArrayList<>(servicesMap.get(serverEventConnectorType));
                            for (String type : ORDER_LIST) {
                                if (!StringUtils.equals(type, serverEventConnectorType)) {
                                    List<Service> services = servicesMap.get(type);
                                    if (CollectionUtils.isNotEmpty(services)) {
                                        for (Service newService : services) {
                                            boolean needAdd = true;
                                            for (Service existService : finalServiceList) {
                                                if (StringUtils.equals(newService.getName().getValue(), existService.getName().getValue())) {
                                                    needAdd = false;
                                                    break;
                                                }
                                            }
                                            if (needAdd) {
                                                finalServiceList.add(newService);
                                            }
                                        }
                                    }
                                    newDiscoverResponseBuilder.addAllServices(finalServiceList);
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
            }
            if (null == serverEvent.getError()) {
                successUpdates.addAndGet(1);
            }
            boolean svcDeleted = getEventHandler().onEventUpdate(serverEvent);
            if (!svcDeleted && subTask != null) {
                subTask.setType(ServiceUpdateTaskConstant.Type.FIRST, ServiceUpdateTaskConstant.Type.LONG_RUNNING);
            }
            return svcDeleted;
        } finally {
            fairLock.unlock();
        }
    }

    public boolean setStatus(Status last, Status current, boolean isSpread) {
        if (isSpread) {
            for (Map.Entry<String, ServiceUpdateTask> entry : subServiceUpdateTaskMap.entrySet()) {
                entry.getValue().setStatus(last, current);
            }
        }
        return taskStatus.compareAndSet(last, current);
    }

    private boolean canExecute(String connectorType, ServiceUpdateTask serviceUpdateTask) {
        boolean canConnectorExecute = StringUtils.equalsIgnoreCase(mainConnectorType, connectorType)
                || serviceEventKey.getEventType().equals(EventType.INSTANCE)
                || serviceEventKey.getEventType().equals(EventType.SERVICE);
        boolean canTaskExecute = (serviceUpdateTask.getTaskType() == ServiceUpdateTaskConstant.Type.FIRST
                && serviceUpdateTask.getTaskStatus() == Status.READY) || serviceUpdateTask.needUpdate();
        return canConnectorExecute && canTaskExecute;
    }

    public boolean notifyServerEventWithRevisionChecking(ServerEvent serverEvent, String revision) {
        if (serverEvent.getValue() instanceof DiscoverResponse) {
            if (StringUtils.equals(revision, instanceListMeta.getLastRevision())) {
                return notifyServerEvent(serverEvent);
            }
        }
        return false;
    }

    private String convertToString(List<Instance> list) {
        StringBuilder stringBuilder = new StringBuilder("----------------------\n");
        for (Instance instance : list) {
            stringBuilder.append(instance.toString()).append("\n");
        }
        stringBuilder.append("----------------------\n");
        return stringBuilder.toString();
    }

    private String convertToString(Map<String, List<Instance>> map) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, List<Instance>> entry : map.entrySet()) {
            List<Instance> instances = entry.getValue();
            StringBuilder line = new StringBuilder("-----------" + entry.getKey() + "-----------\n");
            for (Instance instance : instances) {
                line.append(instance.toString()).append("\n");
            }
            stringBuilder.append(line).append("\n");
        }
        stringBuilder.append("----------------------\n");
        return stringBuilder.toString();
    }
}
