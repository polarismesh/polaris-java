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

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.server.*;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static com.tencent.polaris.api.config.plugin.DefaultPlugins.SERVER_CONNECTOR_NACOS;
import static com.tencent.polaris.plugins.connector.common.constant.NacosConstant.MetadataMapKey.*;

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
    private static final String INSTANCE_NAME = "%s$%s@@%s#%s#%d";

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
     * Nacos Context.
     */
    private NacosContext nacosContext;

    /**
     * Nacos namespace & NamingService mappings .
     */
    private final Map<String, NamingService> namingServices = new ConcurrentHashMap<>();

    /**
     * Nacos namespace & NacosServiceMerger mappings .
     */
    private final Map<String, NacosService> nacosServices = new ConcurrentHashMap<>();

    private final Object lock = new Object();


    @Override
    public String getName() {
        return SERVER_CONNECTOR_NACOS;
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
                    if (SERVER_CONNECTOR_NACOS.equals(serverConnectorConfig.getProtocol())) {
                        initActually(ctx, serverConnectorConfig);
                    }
                }
            }
        }
    }

    private void initActually(InitContext ctx, ServerConnectorConfig connectorConfig) {
        id = connectorConfig.getId();
        if (ctx.getConfig().getProvider().getRegisterConfigMap().containsKey(id)) {
            isRegisterEnable = ctx.getConfig().getProvider().getRegisterConfigMap().get(id).isEnable();
        }
        if (ctx.getConfig().getConsumer().getDiscoveryConfigMap().containsKey(id)) {
            isDiscoveryEnable = ctx.getConfig().getConsumer().getDiscoveryConfigMap().get(id).isEnable();
        }

        nacosProperties = this.decodeNacosConfigProperties(connectorConfig);
        nacosContext = new NacosContext();
        Map<String, String> metadata = connectorConfig.getMetadata();
        if (metadata.containsKey(NACOS_GROUP_KEY) && StringUtils.isNotEmpty(metadata.get(NACOS_GROUP_KEY))) {
            nacosContext.setGroupName(metadata.get(NACOS_GROUP_KEY));
        }
        if (metadata.containsKey(NACOS_CLUSTER_KEY) && StringUtils.isNotEmpty(metadata.get(NACOS_CLUSTER_KEY))) {
            nacosContext.setClusterName(metadata.get(NACOS_CLUSTER_KEY));
        }

        if (metadata.containsKey(NACOS_SERVICE_KEY) && StringUtils.isNotEmpty(metadata.get(NACOS_SERVICE_KEY))) {
            nacosContext.setServiceName(metadata.get(NACOS_SERVICE_KEY));
        }
        if (metadata.containsKey(NACOS_EPHEMERAL_KEY)) {
            nacosContext.setEphemeral(Boolean.parseBoolean(metadata.get(NACOS_EPHEMERAL_KEY)));
        }
        if (metadata.containsKey(PropertyKeyConst.NAMESPACE) && StringUtils.isNotEmpty(
                metadata.get(PropertyKeyConst.NAMESPACE))) {
            nacosContext.setNamespace(metadata.get(PropertyKeyConst.NAMESPACE));
        }
        getOrCreateNamingService(nacosContext.getNamespace());

    }

    private Properties decodeNacosConfigProperties(ServerConnectorConfig config) {
        Properties properties = new Properties();
        Map<String, String> metadata = Optional.ofNullable(config.getMetadata()).orElse(new HashMap<>());
        if (Objects.nonNull(metadata.get(PropertyKeyConst.USERNAME))) {
            properties.put(PropertyKeyConst.USERNAME, metadata.get(PropertyKeyConst.USERNAME));
        }
        if (Objects.nonNull(metadata.get(PropertyKeyConst.PASSWORD))) {
            properties.put(PropertyKeyConst.PASSWORD, metadata.get(PropertyKeyConst.PASSWORD));
        }
        if (Objects.nonNull(metadata.get(PropertyKeyConst.CONTEXT_PATH))) {
            properties.put(PropertyKeyConst.CONTEXT_PATH, metadata.get(PropertyKeyConst.CONTEXT_PATH));
        }
        if (StringUtils.isNotEmpty(metadata.get(PropertyKeyConst.NAMESPACE))) {
            properties.put(PropertyKeyConst.NAMESPACE, metadata.get(PropertyKeyConst.NAMESPACE));
        }
        properties.put(PropertyKeyConst.SERVER_ADDR, String.join(",", config.getAddresses()));
        return properties;
    }

    public NamingService getOrCreateNamingService(String namespace) {
        // nacos中一个命名空间仅对应一个sdk实例
        NamingService namingService = namingServices.get(namespace);
        if (namingService != null) {
            return namingService;
        }
        synchronized (lock) {
            Properties properties = new Properties(nacosProperties);
            // polaris 默认namespace 为default，nacos中映射为public
            if (StringUtils.isEmpty(nacosProperties.getProperty(PropertyKeyConst.NAMESPACE))
                    && !StringUtils.equals(namespace, "default")) {
                properties.setProperty(PropertyKeyConst.NAMESPACE, namespace);
            }

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
            return namingService;
        }
    }

    public NacosService getNacosService(String namespace) {
        NacosService nacosService = nacosServices.get(namespace);
        if (nacosService != null) {
            return nacosService;
        }
        // nacos sdk封装的服务，用于给polaris-java调用
        NamingService namingService = getOrCreateNamingService(namespace);
        synchronized (lock) {
            nacosService = new NacosService(namingService, nacosContext);
            nacosServices.put(namespace, nacosService);
        }
        return nacosService;
    }

    public NacosContext getNacosContext() {
        return nacosContext;
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
                Instance instance = buildRegisterNacosInstance(req);
                namingService.registerInstance(instance.getServiceName(),
                        nacosContext.getGroupName(), instance);
                response.setInstanceID(instance.getInstanceId());
            } catch (NacosException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                        String.format("[Connector][Nacos] fail to register host %s:%d service %s", req.getHost(),
                                req.getPort(),
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
            // 优先设置成nacos的service name，如没有再设置成req的service name
            String serviceName = req.getService();
            if (StringUtils.isNotEmpty(nacosContext.getServiceName())) {
                serviceName = nacosContext.getServiceName();
            }
            Instance instance = buildDeregisterNacosInstance(req, nacosContext.getGroupName());

            // deregister with nacos naming service
            service.deregisterInstance(serviceName, nacosContext.getGroupName(),
                    instance);
            LOG.info("[connector][Nacos] deregister service {} success, groupName: {}, clusterName: {}, instance: {}",
                    serviceName, nacosContext.getGroupName(), nacosContext.getClusterName(), instance);
        } catch (NacosException e) {
            throw new RetriableException(ErrorCode.NETWORK_ERROR,
                    String.format("[Connector][Nacos] fail to deregister host %s:%d service %s", req.getHost(),
                            req.getPort(),
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
    public ReportServiceContractResponse reportServiceContract(ReportServiceContractRequest req)
            throws PolarisException {
        return null;
    }

    @Override
    public void updateServers(ServiceEventKey svcEventKey) {
        // do nothing
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
    public boolean isReportServiceContractEnable() {
        return false;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized.get();
    }

    @Override
    public void retryServiceUpdateTask(ServiceUpdateTask updateTask) {
        // do nothing
    }

    @Override
    protected void submitServiceHandler(ServiceUpdateTask updateTask, long delayMs) {
        //
    }

    @Override
    public void addLongRunningTask(ServiceUpdateTask serviceUpdateTask) {
        // do nothing
    }

    @Override
    protected void doDestroy() {
        if (initialized.compareAndSet(true, false)) {
            // 先unsubscribe listener
            if (CollectionUtils.isNotEmpty(nacosServices)) {
                nacosServices.forEach((s, nacosService) -> {
                    nacosService.destroy();
                });
            }
            if (CollectionUtils.isNotEmpty(namingServices)) {
                namingServices.forEach((s, namingService) -> {
                    try {
                        namingService.shutDown();
                    } catch (NacosException ignore) {
                    }
                });
            }

        }
    }

    private Instance buildRegisterNacosInstance(CommonProviderRequest req) {
        // nacos上注册和polaris不同的服务名时优先用nacosContext中的serviceName
        String serviceName = req.getService();
        if (StringUtils.isNotEmpty(nacosContext.getServiceName())) {
            serviceName = nacosContext.getServiceName();
        }
        String nameSpace = req.getNamespace();

        String instanceId = String.format(INSTANCE_NAME, nameSpace, nacosContext.getGroupName(),
                serviceName, req.getHost(), req.getPort());
        Instance instance = new Instance();
        instance.setServiceName(serviceName);
        instance.setClusterName(nacosContext.getClusterName());
        instance.setInstanceId(instanceId);
        instance.setEnabled(true);
        instance.setEphemeral(nacosContext.isEphemeral());
        instance.setPort(req.getPort());
        instance.setIp(req.getHost());
        instance.setHealthy(true);
        if (Objects.nonNull(req.getWeight())) {
            instance.setWeight(req.getWeight());
        }
        Map<String, String> metadata = new HashMap<>(Optional.ofNullable(req.getMetadata())
                .orElse(Collections.emptyMap()));

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

    private Instance buildDeregisterNacosInstance(CommonProviderRequest req, String group) {
        String serviceName = req.getService();
        if (StringUtils.isNotEmpty(nacosContext.getServiceName())) {
            serviceName = nacosContext.getServiceName();
        }
        String instanceId = String.format(INSTANCE_NAME, req.getNamespace(), group,
                serviceName, req.getHost(), req.getPort());
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instance.setEnabled(true);
        instance.setEphemeral(nacosContext.isEphemeral());
        instance.setPort(req.getPort());
        instance.setIp(req.getHost());
        instance.setHealthy(true);
        // clusterName 由nacosContext管理
        instance.setClusterName(nacosContext.getClusterName());
        return instance;
    }

    protected static String analyzeNacosService(String service) {
        String[] detail = service.split("__");
        if (detail.length == 1) {
            return service;
        }
        return service.replaceFirst(detail[0] + "__", "");
    }

    protected static String analyzeNacosGroup(String service) {
        String[] detail = service.split("__");
        if (detail.length == 1 || Objects.equals(detail[0], "")) {
            return DEFAULT_GROUP;
        }
        return detail[0];
    }
}
