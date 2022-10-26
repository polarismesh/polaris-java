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

package com.tencent.polaris.plugins.connector.nacos;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.listener.EventListener;
import com.alibaba.nacos.api.naming.listener.NamingEvent;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.common.utils.MD5Utils;
import com.google.common.collect.Lists;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.exception.ServerErrorResponseException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.server.CommonProviderRequest;
import com.tencent.polaris.api.plugin.server.CommonProviderResponse;
import com.tencent.polaris.api.plugin.server.ReportClientRequest;
import com.tencent.polaris.api.plugin.server.ReportClientResponse;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.plugin.server.ServiceEventHandler;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.ServicesByProto;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceInstancesResponse;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import java.lang.Exception;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.nacos.api.common.Constants.*;

/**
 * An implement of {@link ServerConnector} to connect to Nacos Server.
 *
 * @author Palmer Xu
 */
public class NacosConnector extends DestroyableServerConnector {

    /**
     * Logger Instance.
     */
    private static final Logger LOG = LoggerFactory.getLogger(NacosConnector.class);

    /**
     * Service Instance Name Format.
     */
    private static final String INSTANCE_NAME = "%s$%s@@DEFAULT_GROUP#%s#%d";

    /**
     * If server connector initialized .
     */
    private final AtomicBoolean initialized = new AtomicBoolean();

    /**
     * Connector id .
     */
    private String id;

    /**
     * Marking service registration as enabled or not .
     */
    private boolean isRegisterEnable = true;

    /**
     * Marking service discovery as enabled or not .
     */
    private boolean isDiscoveryEnable = true;

    /**
     * Nacos Config Properties .
     */
    private Properties nacosProperties = new Properties();

    /**
     * Nacos namespace & NamingService mappings .
     */
    private final Map<String, NamingService> namingServices = new ConcurrentHashMap<>();

    /**
     * Nacos namespace & NacosServiceMerger mappings .
     */
    private final Map<String, NacosServiceMerger> mergers = new ConcurrentHashMap<>();

    private final Object lock = new Object();

    private static final int NACOS_SERVICE_PAGESIZE = 10;

    @Override
    public String getName() {
        return DefaultPlugins.SERVER_CONNECTOR_NACOS;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVER_CONNECTOR.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        if (initialized.compareAndSet(false, true)) {
            List<ServerConnectorConfigImpl> serverConnectorConfigs = ctx.getConfig().getGlobal().getServerConnectors();
            if (CollectionUtils.isNotEmpty(serverConnectorConfigs)) {
                for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectorConfigs) {
                    if (DefaultPlugins.SERVER_CONNECTOR_NACOS.equals(serverConnectorConfig.getProtocol())) {
                        initActually(ctx, serverConnectorConfig);
                    }
                }
            }
        }
    }

    private void initActually(InitContext ctx, ServerConnectorConfig connectorConfig) {
        this.id = connectorConfig.getId();
        if (ctx.getConfig().getProvider().getRegisterConfigMap().containsKey(id)) {
            isRegisterEnable = ctx.getConfig().getProvider().getRegisterConfigMap().get(id).isEnable();
        }
        if (ctx.getConfig().getConsumer().getDiscoveryConfigMap().containsKey(id)) {
            isDiscoveryEnable = ctx.getConfig().getConsumer().getDiscoveryConfigMap().get(id).isEnable();
        }

        nacosProperties = this.decodeNacosConfigProperties(connectorConfig);
    }

    private Properties decodeNacosConfigProperties(ServerConnectorConfig config) {
        Properties properties = new Properties();
        // Nacos Address URI: nacos:nacos@127.0.0.1:8848
        String address = config.getAddresses().get(0);
        if (address.indexOf("@") > 0) {
            String[] parts = address.split("@");
            String[] auths = parts[0].split(":");
            if (auths.length == 2) {
                properties.put(PropertyKeyConst.USERNAME, auths[0]);
                properties.put(PropertyKeyConst.PASSWORD, auths[1]);
            }

            properties.put(PropertyKeyConst.SERVER_ADDR, parts[1]);
        } else {
            properties.put(PropertyKeyConst.USERNAME, "nacos");
            properties.put(PropertyKeyConst.PASSWORD, "nacos");
        }
        return properties;
    }

    private NamingService getOrCreateNamingService(String namespace) {
        NamingService namingService = namingServices.get(namespace);
        if (namingService != null) {
            return namingService;
        }

        synchronized (lock) {
            Properties properties = new Properties(nacosProperties);
            properties.setProperty(PropertyKeyConst.NAMESPACE, namespace);
            try {
                namingService = NacosFactory.createNamingService(properties);
            } catch (NacosException e) {
                LOG.error("[Connector][Nacos] fail to create naming service to {}, namespace {}",
                        properties.get(PropertyKeyConst.SERVER_ADDR), namespace, e);
                return null;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            namingServices.put(namespace, namingService);
            mergers.put(namespace, new NacosServiceMerger(namingService));
            return namingService;
        }
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        // do nothing
    }

    @Override
    public void registerServiceHandler(ServiceEventHandler handler) throws PolarisException {
        // do nothing
    }

    @Override
    public void deRegisterServiceHandler(ServiceEventKey eventKey) throws PolarisException {
        // do nothing
    }

    @Override
    public CommonProviderResponse registerInstance(CommonProviderRequest req,
            Map<String, String> customHeader) throws PolarisException {
        CommonProviderResponse response = new CommonProviderResponse();

        if (isRegisterEnable()) {
            NamingService namingService = getOrCreateNamingService(req.getNamespace());

            if (namingService == null) {
                LOG.error("[Nacos] fail to lookup namingService for service {}", req.getService());
                return null;
            }

            try {
                Instance instance = buildNacosInstance(req);
                namingService.registerInstance(req.getService(), DEFAULT_GROUP, instance);
                response.setInstanceID(instance.getInstanceId());
            } catch (NacosException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                    String.format("[Connector][Nacos] fail to register host %s:%d service %s", req.getHost(), req.getPort(),
                        req.getService()), e);
            }
        }
        return response;
    }

    @Override
    public void deregisterInstance(CommonProviderRequest req) throws PolarisException {

        try {
            NamingService service = getOrCreateNamingService(req.getNamespace());

            if (service == null) {
                LOG.error("[Nacos] fail to lookup namingService for service {}", req.getService());
                return;
            }

            Instance instance = buildNacosInstance(req);
            // register with nacos naming service
            service.deregisterInstance(req.getService(), DEFAULT_GROUP, instance);
        } catch (NacosException e) {
            throw new RetriableException(ErrorCode.NETWORK_ERROR,
                String.format("[Connector][Nacos] fail to deregister host %s:%d service %s", req.getHost(), req.getPort(),
                    req.getService()), e);
        }
    }

    @Override
    public void heartbeat(CommonProviderRequest req) throws PolarisException {
        // do nothing
    }

    @Override
    public ReportClientResponse reportClient(ReportClientRequest req) throws PolarisException {
        return null;
    }

    @Override
    public void updateServers(ServiceEventKey svcEventKey) {
        // do nothing
    }

    @Override
    public ServiceInstancesResponse syncGetServiceInstances(ServiceUpdateTask serviceUpdateTask) {
        List<DefaultInstance> instanceList = new ArrayList<>();
        try {

            String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
            NamingService namingService = getOrCreateNamingService(namespace);
            NacosServiceMerger merger = mergers.get(namespace);

            if (namingService == null || merger == null) {
                LOG.error("[Connector][Nacos] fail to lookup namingService for service {}", namespace);
                return null;
            }

            List<Instance> serviceList =
                namingService.getAllInstances(serviceUpdateTask.getServiceEventKey().getService(),
                    DEFAULT_GROUP, Lists.newArrayList(DEFAULT_CLUSTER_NAME));

            // subscribe service instance change .
            merger.addListener(serviceUpdateTask.getServiceEventKey().getService(), DEFAULT_GROUP, DEFAULT_CLUSTER_NAME, serviceList);

            if (serviceList == null || serviceList.isEmpty()) {
                return null;
            }

            NacosServiceMerger.ServiceKey serviceKey = new NacosServiceMerger.ServiceKey(serviceUpdateTask.getServiceEventKey().getService(),
                DEFAULT_GROUP, DEFAULT_CLUSTER_NAME);

            NacosServiceMerger.ServiceValue serviceValue = merger.findServiceInstances(serviceKey);

            for (Instance service : serviceList) {
                DefaultInstance instance = new DefaultInstance();
                instance.setId(service.getInstanceId());
                instance.setService(service.getServiceName());
                instance.setHost(service.getIp());
                instance.setPort(service.getPort());
                instance.setHealthy(service.isHealthy());
                instance.setMetadata(service.getMetadata());
                instance.setIsolated(service.isEnabled());
                instanceList.add(instance);
            }
            return new ServiceInstancesResponse(serviceValue.getRevision(), instanceList);
        } catch (Exception e) {
            throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
                String.format("[Connector][Nacos] Get service instances of %s sync failed.",
                    serviceUpdateTask.getServiceEventKey().getServiceKey()));
        }
    }

    @Override
    public Services syncGetServices(ServiceUpdateTask serviceUpdateTask) {
        Services services = new ServicesByProto(new ArrayList<>());
        try {

            String namespace = serviceUpdateTask.getServiceEventKey().getNamespace();
            NamingService namingService = getOrCreateNamingService(namespace);

            if (namingService == null) {
                LOG.error("[Connector][Nacos] fail to lookup namingService for service {}", namespace);
                return null;
            }

            int pageIndex = 1;
            ListView<String> listView = namingService.getServicesOfServer(pageIndex, NACOS_SERVICE_PAGESIZE, DEFAULT_GROUP);
            final Set<String> serviceNames = new LinkedHashSet<>(listView.getData());
            int count = listView.getCount();
            int pageNumbers = count / NACOS_SERVICE_PAGESIZE;
            int remainder = count % NACOS_SERVICE_PAGESIZE;
            if (remainder > 0) {
                pageNumbers += 1;
            }
            // If more than 1 page
            while (pageIndex < pageNumbers) {
                listView = namingService.getServicesOfServer(++pageIndex, NACOS_SERVICE_PAGESIZE, DEFAULT_GROUP);
                serviceNames.addAll(listView.getData());
            }

            serviceNames.forEach(name -> {
                ServiceInfo serviceInfo = new ServiceInfo();
                serviceInfo.setNamespace(namespace);
                serviceInfo.setService(name);
                services.getServices().add(serviceInfo);
            });

        } catch (NacosException e) {
            throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
                String.format("[Connector][Nacos] Get services of %s instances sync failed.",
                    serviceUpdateTask.getServiceEventKey().getServiceKey()));
        }
        return services;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean isRegisterEnable() {
        return this.isRegisterEnable;
    }

    @Override
    public boolean isDiscoveryEnable() {
        return this.isDiscoveryEnable;
    }

    @Override
    public boolean isInitialized()                  {
        return this.initialized.get();
    }

    @Override
    public void retryServiceUpdateTask(ServiceUpdateTask updateTask) {
        // do nothing
    }

    @Override
    protected void submitServiceHandler(ServiceUpdateTask updateTask, long delayMs) {
        // do nothing
    }

    @Override
    public void addLongRunningTask(ServiceUpdateTask serviceUpdateTask) {
        // do nothing
    }

    @Override
    protected void doDestroy() {
        if (initialized.compareAndSet(true, false)) {
            // unsubscribe service listener
            if (!mergers.isEmpty()) {
                mergers.forEach((s, nacosServiceMerger) -> {
                    try {
                        nacosServiceMerger.shutdown();
                    } catch (Exception ignore) {
                    }
                });
            }

            // shutdown naming service
            if (!namingServices.isEmpty()) {
                namingServices.forEach((s, namingService) -> {
                    try {
                        namingService.shutDown();
                    } catch (NacosException ignore) {
                    }
                });
            }
        }
    }

    private Instance buildNacosInstance(CommonProviderRequest req) {
        String instanceId = String.format(INSTANCE_NAME, req.getNamespace(), req.getService(), req.getHost(), req.getPort());
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instance.setClusterName(DEFAULT_CLUSTER_NAME);
        instance.setEnabled(true);
        instance.setEphemeral(true);
        instance.setPort(req.getPort());
        instance.setIp(req.getHost());
        instance.setHealthy(true);
        instance.setWeight(req.getWeight());
        instance.setServiceName(req.getService());

        Map<String, String> metadata = new HashMap<>(Optional.ofNullable(req.getMetadata()).orElse(Collections.emptyMap()));

        // 填充默认 protocol 以及 version 属性信息
        if (StringUtils.isNotEmpty(req.getProtocol())) {
            metadata.put("protocol", req.getProtocol());
        }
        if (StringUtils.isNotEmpty(req.getVersion())) {
            metadata.put("version", req.getVersion());
        }

        // 填充地域信息
        if (StringUtils.isNotEmpty(req.getRegion())) {
            metadata.put("region", req.getRegion());
        }
        if (StringUtils.isNotEmpty(req.getZone())) {
            metadata.put("zone", req.getZone());
        }
        if (StringUtils.isNotEmpty(req.getCampus())) {
            metadata.put("campus", req.getCampus());
        }

        instance.setMetadata(metadata);

        return instance;
    }

    private static class NacosServiceMerger {

        private final NamingService namingService;

        private NacosServiceMerger(NamingService service) {
            this.namingService = service;
        }

        private static final Map<ServiceKey, ServiceValue> SERVICE_INSTANCES = new HashMap<>(8);

        private static final Map<ServiceKey, EventListener> SERVICE_INSTANCES_LISTENERS = new HashMap<>(8);

        /**
         * Add Service Watcher Listener.
         * @param serviceName service name
         * @param group service group
         * @param clusters service cluster name
         */
        public void addListener(String serviceName, String group, String clusters, List<Instance> instances) throws Exception {
            ServiceKey serviceKey = new ServiceKey(serviceName, group, clusters);
            if (!SERVICE_INSTANCES.containsKey(serviceKey)) {
                // build cache
                SERVICE_INSTANCES.put(serviceKey, new ServiceValue(instances));
                // subscribe
                try {

                    // listener defined.
                    EventListener listener = event -> {
                        if (event instanceof NamingEvent) {
                            NamingEvent namingEvent = (NamingEvent) event;
                            // service key
                            ServiceKey tempKey = new ServiceKey(namingEvent.getServiceName(), namingEvent.getGroupName(), namingEvent.getClusters());
                            List<Instance> tempInstances = namingEvent.getInstances();

                            // rebuild instance cache
                            try {
                                SERVICE_INSTANCES.get(tempKey).rebuild(tempInstances);
                            } catch (Exception e) {
                                LOG.warn("[Connector][Nacos] service revision build failed, service name: {}, group: {}", serviceName, group, e);
                            }
                        }
                    };

                    namingService.subscribe(serviceName, group, Lists.newArrayList(clusters), listener);

                    SERVICE_INSTANCES_LISTENERS.put(serviceKey, listener);

                } catch (NacosException e) {
                    LOG.warn("[Connector][Nacos] service subscribe failed, service name: {}, group: {}", serviceName, group, e);
                }
            }
        }

        public void shutdown() {
            try {
                SERVICE_INSTANCES_LISTENERS.keySet().parallelStream().forEach(key -> {
                    try {
                        namingService.unsubscribe(key.serviceName, key.group, Lists.newArrayList(key.clusters), SERVICE_INSTANCES_LISTENERS.get(key));
                    } catch (NacosException ignore) {
                    }
                });
            } catch (Exception ignore) {
            }
        }

        public ServiceValue findServiceInstances(ServiceKey key) {
            return SERVICE_INSTANCES.get(key);
        }

        private static class ServiceKey {
            private final String serviceName;
            private final String group;
            private final String clusters;
            ServiceKey(String serviceName, String group, String clusters) {
                this.serviceName = serviceName;
                this.group = group;
                this.clusters = clusters;
            }

            public String getServiceName() {
                return serviceName;
            }

            public String getGroup() {
                return group;
            }

            public String getClusters() {
                return clusters;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                ServiceKey that = (ServiceKey) o;
                return Objects.equals(serviceName, that.serviceName) && Objects.equals(group, that.group) && Objects.equals(clusters, that.clusters);
            }

            @Override
            public int hashCode() {
                return Objects.hash(serviceName, group, clusters);
            }
        }

        private static class ServiceValue {
            private String revision;
            private List<Instance> instances;

            public ServiceValue(List<Instance> instances) throws Exception {
                this.instances = instances;
                this.revision = buildRevision(instances);
            }

            private String buildRevision(List<Instance> instances) throws Exception {
                StringBuilder revisionStr = new StringBuilder("NacosServiceInstances");
                for (Instance instance : instances) {
                    revisionStr.append("|").append(instance.toString());
                }
                return MD5Utils.md5Hex(revisionStr.toString().getBytes(StandardCharsets.UTF_8));
            }

            public void rebuild(List<Instance> instances) throws Exception {
                this.instances = instances;
                this.revision = buildRevision(instances);
            }

            public String getRevision() {
                return revision;
            }

            public List<Instance> getInstances() {
                return instances;
            }
        }
    }
}
