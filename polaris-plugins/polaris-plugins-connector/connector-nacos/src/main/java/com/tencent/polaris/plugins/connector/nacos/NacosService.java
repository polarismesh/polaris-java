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

package com.tencent.polaris.plugins.connector.nacos;

import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_NACOS;
import static com.tencent.polaris.plugins.connector.common.constant.ConnectorConstant.SERVER_CONNECTOR_TYPE;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.Event;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.common.utils.MD5Utils;
import com.google.protobuf.BoolValue;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.server.ServerEvent;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.specification.api.v1.model.ModelProto.Location;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;

public class NacosService extends Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(NacosService.class);

    private final NamingService namingService;

    private final NacosContext nacosContext;

    protected final ExecutorService refreshExecutor;

    private static final int NACOS_SERVICE_PAGESIZE = 10;

    private final Map<String, EventListener> eventListeners;


    public NacosService(NamingService namingService, NacosContext nacosContext) {
        this.namingService = namingService;
        this.nacosContext = nacosContext;
        NamedThreadFactory threadFactory = new NamedThreadFactory("nacos-service");
        this.refreshExecutor = Executors.newFixedThreadPool(8, threadFactory);
        this.eventListeners = new ConcurrentHashMap<>();
    }

    public NamingService getNamingService() {
        return namingService;
    }

    private static class NacosEventListener implements EventListener {

        private final ServiceUpdateTask serviceUpdateTask;
        private final NacosContext nacosContext;

        public NacosEventListener(ServiceUpdateTask serviceUpdateTask, NacosContext nacosContext) {
            this.serviceUpdateTask = serviceUpdateTask;
            this.nacosContext = nacosContext;
        }

        @Override
        public void onEvent(Event event) {
            if (event instanceof NamingEvent) {
                String serviceId = serviceUpdateTask.getServiceEventKey().getService();
                NamingEvent namingEvent = (NamingEvent) event;
                List<Instance> nacosInstances = namingEvent.getInstances();
                List<ServiceProto.Instance> polarisInstanceList = new ArrayList<>();
                // transform nacos instance to polaris instance
                for (Instance nacosInstance : nacosInstances) {
                    ServiceProto.Instance.Builder instanceBuilder = ServiceProto.Instance.newBuilder()
                            .setService(StringValue.of(nacosInstance.getServiceName()))
                            .setHost(StringValue.of(nacosInstance.getIp()))
                            .setPort(UInt32Value.of(nacosInstance.getPort()))
                            .setHealthy(BoolValue.of(nacosInstance.isHealthy()))
                            .setIsolate(BoolValue.of(!nacosInstance.isEnabled()))
                            // The default weight in Nacos is (double)1, while in Polaris it is (int)100
                            // need to multiply by 100
                            .setWeight(UInt32Value.of((int) (100 * nacosInstance.getWeight())));
                    if (StringUtils.isNotBlank(nacosInstance.getInstanceId())) {
                        instanceBuilder.setId(StringValue.of(nacosInstance.getInstanceId()));
                    } else {
                        String id =
                                serviceId + "-" + nacosInstance.getIp().replace(".", "-") + "-"
                                        + nacosInstance.getPort();
                        instanceBuilder.setId(StringValue.of(id));
                        LOG.info("Instance with name {} host {} port {} doesn't have id.", serviceId
                                , nacosInstance.getIp(), nacosInstance.getPort());
                    }
                    // set metadata
                    Map<String, String> metadata = nacosInstance.getMetadata();
                    if (CollectionUtils.isNotEmpty(metadata)) {
                        instanceBuilder.putAllMetadata(metadata);
                    }
                    instanceBuilder.putMetadata("nacos.cluster", nacosInstance.getClusterName())
                            .putMetadata("nacos.group", nacosContext.getGroupName())
                            .putMetadata("nacos.ephemeral", String.valueOf(nacosInstance.isEphemeral()));

                    String protocol = metadata.getOrDefault("protocol", "");
                    String version = metadata.getOrDefault("version", "");
                    if (StringUtils.isNotEmpty(protocol)) {
                        instanceBuilder.setProtocol(StringValue.of(protocol));
                    }
                    if (StringUtils.isNotEmpty(version)) {
                        instanceBuilder.setVersion(StringValue.of(version));
                    }

                    String region = metadata.getOrDefault("region", "");
                    String zone = metadata.getOrDefault("zone", "");
                    String campus = metadata.getOrDefault("campus", "");
                    instanceBuilder.putMetadata(SERVER_CONNECTOR_TYPE, SERVER_CONNECTOR_NACOS);
                    Location.Builder locationBuilder = Location.newBuilder();
                    if (StringUtils.isNotEmpty(region)) {
                        locationBuilder.setRegion(StringValue.of(region));
                    }
                    if (StringUtils.isNotEmpty(zone)) {
                        locationBuilder.setZone(StringValue.of(zone));
                    }
                    if (StringUtils.isNotEmpty(campus)) {
                        locationBuilder.setCampus(StringValue.of(campus));
                    }

                    instanceBuilder.setLocation(locationBuilder.build());
                    instanceBuilder.setNamespace(
                            StringValue.of(serviceUpdateTask.getServiceEventKey().getNamespace()));

                    polarisInstanceList.add(instanceBuilder.build());
                }
                ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
                newServiceBuilder.setNamespace(
                        StringValue.of(serviceUpdateTask.getServiceEventKey().getNamespace()));
                newServiceBuilder.setName(StringValue.of(serviceUpdateTask.getServiceEventKey().getService()));
                newServiceBuilder.setRevision(StringValue.of(buildInstanceListRevision(nacosInstances)));
                ServiceProto.Service service = newServiceBuilder.build();
                ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
                newDiscoverResponseBuilder.setService(service);
                newDiscoverResponseBuilder.addAllInstances(polarisInstanceList);
                int code = ServerCodes.EXECUTE_SUCCESS;
                newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
                // notify to polaris-java
                ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(),
                        newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_NACOS);
                boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);
                if (!svcDeleted) {
                    serviceUpdateTask.addUpdateTaskSet();
                }
            }
        }
    }

    public void asyncGetInstances(ServiceUpdateTask serviceUpdateTask) {
        EventListener serviceListener = new NacosEventListener(serviceUpdateTask, nacosContext);
        try {
            namingService.subscribe(serviceUpdateTask.getServiceEventKey().getService(), nacosContext.getGroupName(),
                    serviceListener);
            eventListeners.put(serviceUpdateTask.getServiceEventKey().getService(), serviceListener);
            LOG.debug("[NacosConnector] Subscribe instances of {} success. ",
                    serviceUpdateTask.getServiceEventKey().getService());
        } catch (NacosException nacosException) {
            String errorMsg = String.format("subscribe nacos service instances of %s failed.",
                    serviceUpdateTask.getServiceEventKey().getService());

            LOG.error(errorMsg, nacosException);
            try {
                Thread.sleep(nacosContext.getNacosErrorSleep());
            } catch (Exception e1) {
                LOG.error("error in sleep, msg: " + e1.getMessage());
            }
            PolarisException error = new PolarisException(ErrorCode.SERVER_ERROR, errorMsg, nacosException);
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error,
                    SERVER_CONNECTOR_NACOS);
            serviceUpdateTask.notifyServerEvent(serverEvent);
            serviceUpdateTask.retry();
        }
    }

    public void asyncGetService(ServiceUpdateTask serviceUpdateTask) {
        refreshExecutor.submit(() -> {
            try {
                syncGetService(serviceUpdateTask);
            } catch (Throwable e) {
                LOG.error("nacos client failed to get service {}. ", serviceUpdateTask.getServiceEventKey().getService(),
                        e);
            }
        });
    }

    private void syncGetService(ServiceUpdateTask serviceUpdateTask) {
        try {
            String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
            if (namingService == null) {
                LOG.error("nacos client fail to lookup namingService for service {}", namespace);
                return;
            }
            int pageIndex = 1;
            ListView<String> listView = namingService.getServicesOfServer(pageIndex, NACOS_SERVICE_PAGESIZE,
                    nacosContext.getGroupName());
            final Set<String> serviceNames = new LinkedHashSet<>(listView.getData());
            int count = listView.getCount();
            int pageNumbers = count / NACOS_SERVICE_PAGESIZE;
            int remainder = count % NACOS_SERVICE_PAGESIZE;
            if (remainder > 0) {
                pageNumbers += 1;
            }
            // If more than 1 page
            while (pageIndex < pageNumbers) {
                listView = namingService.getServicesOfServer(++pageIndex, NACOS_SERVICE_PAGESIZE,
                        nacosContext.getGroupName());
                serviceNames.addAll(listView.getData());
            }

            List<ServiceProto.Service> newServiceList = new ArrayList<>();
            serviceNames.forEach(name -> {
                ServiceProto.Service service = ServiceProto.Service.newBuilder()
                        .setNamespace(StringValue.of(namespace))
                        .setName(StringValue.of(name))
                        .putMetadata(SERVER_CONNECTOR_TYPE, SERVER_CONNECTOR_NACOS)
                        .build();
                newServiceList.add(service);
            });

            ServiceProto.Service.Builder newServiceBuilder = ServiceProto.Service.newBuilder();
            newServiceBuilder.setNamespace(StringValue.of(namespace));
            newServiceBuilder.setName(StringValue.of(serviceUpdateTask.getServiceEventKey().getService()));
            newServiceBuilder.setRevision(StringValue.of(buildServiceListRevision(serviceNames)));
            ServiceProto.Service newService = newServiceBuilder.build();
            ResponseProto.DiscoverResponse.Builder newDiscoverResponseBuilder = ResponseProto.DiscoverResponse.newBuilder();
            newDiscoverResponseBuilder.setService(newService);
            newDiscoverResponseBuilder.addAllServices(newServiceList);

            int code = ServerCodes.EXECUTE_SUCCESS;
            newDiscoverResponseBuilder.setCode(UInt32Value.of(code));
            LOG.debug("nacos client get service {} success. ",
                    serviceUpdateTask.getServiceEventKey().getService());
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(),
                    newDiscoverResponseBuilder.build(), null, SERVER_CONNECTOR_NACOS);
            boolean svcDeleted = serviceUpdateTask.notifyServerEvent(serverEvent);

            if (!svcDeleted) {
                serviceUpdateTask.addUpdateTaskSet();
            }
        } catch (Throwable throwable) {
            LOG.error("nacos client get service of {} failed. ", serviceUpdateTask.getServiceEventKey().getService(),
                    throwable);
            try {
                Thread.sleep(nacosContext.getNacosErrorSleep());
            } catch (Exception e1) {
                LOG.error("nacos connector error in sleep, msg: " + e1.getMessage());
            }
            PolarisException error = ServerErrorResponseException.build(ErrorCode.NETWORK_ERROR.getCode(),
                    String.format("nacos client sync get service %s failed.",
                            serviceUpdateTask.getServiceEventKey().getServiceKey()));
            ServerEvent serverEvent = new ServerEvent(serviceUpdateTask.getServiceEventKey(), null, error,
                    SERVER_CONNECTOR_NACOS);
            serviceUpdateTask.notifyServerEvent(serverEvent);
        }
    }

    private static String buildInstanceListRevision(List<Instance> instances) {
        StringBuilder revisionStr = new StringBuilder("NacosServiceInstances");
        for (Instance instance : instances) {
            revisionStr.append("|").append(instance.toString());
        }
        try {
            return MD5Utils.md5Hex(revisionStr.toString().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            LOG.error("nacos client build revision failed.", e);
            return "";
        }
    }
    private static String buildServiceListRevision(Set<String> serviceSet) {
        StringBuilder revisionStr = new StringBuilder("NacosServiceInstances");
        for (String serviceName : serviceSet) {
            revisionStr.append("|").append(serviceName);
        }
        try {
            return MD5Utils.md5Hex(revisionStr.toString().getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            LOG.error("build nacos revision failed", e);
            return "";
        }
    }
    @Override
    protected void doDestroy() {
        for (Map.Entry<String, EventListener> entry : eventListeners.entrySet()) {
            try {
                namingService.unsubscribe(entry.getKey(), nacosContext.getGroupName(), entry.getValue());
            } catch (NacosException e) {
                LOG.error("nacos client  unsubscribe service {} in group {} failed. ", entry.getKey(),
                        nacosContext.getGroupName());
            }
        }
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{refreshExecutor});
        try {
            namingService.shutDown();
        } catch (NacosException e) {
            LOG.error("nacos client shutdown namingService failed. ", e);
        }
    }



}