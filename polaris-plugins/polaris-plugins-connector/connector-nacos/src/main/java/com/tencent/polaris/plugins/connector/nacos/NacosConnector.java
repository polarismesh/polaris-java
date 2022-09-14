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

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.alibaba.nacos.client.naming.NacosNamingService;
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
import com.tencent.polaris.client.pojo.ServicesByProto;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceInstancesResponse;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static com.alibaba.nacos.api.common.Constants.DEFAULT_CLUSTER_NAME;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static com.tencent.polaris.api.exception.ErrorCode.PLUGIN_ERROR;

/**
 * An implement of {@link ServerConnector} to connect to Nacos Server.
 *
 * @author Palmer Xu
 */
public class NacosConnector extends DestroyableServerConnector {

    private static final String INSTANCE_NAME = "%s$%s#%s#%d";

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
     * Marking service is already registered or not .
     */
    private boolean isRegistered = false;

    /**
     * Nacos Naming Service Instance .
     */
    private NamingService namingService;

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

        Properties properties = this.decodeNacosConfigProperties(connectorConfig);

        try {
            namingService = new NacosNamingService(properties);
        } catch (NacosException e) {
            throw new PolarisException(PLUGIN_ERROR, "Connector plugin nacos initialized failed , msg : " + e.getErrMsg());
        }
    }

    private Properties decodeNacosConfigProperties(ServerConnectorConfig config) {
        Properties properties = new Properties();
        // Nacos Address URI: nacos:nacos@127.0.0.1:8848/namespace
        String address = config.getAddresses().get(0);
        if (address.indexOf("@") > 0) {
            String[] parts = address.split("@");
            String[] auths = parts[0].split(":");
            if (auths.length == 2) {
                properties.put(PropertyKeyConst.USERNAME, auths[0]);
                properties.put(PropertyKeyConst.PASSWORD, auths[1]);
            }
            if (parts[1].indexOf("/") > 0) {
                String[] subparts = parts[1].split("/");
                if (subparts.length == 1) {
                    properties.put(PropertyKeyConst.SERVER_ADDR, subparts[0]);
                    properties.put(PropertyKeyConst.NAMESPACE, "default");
                } else if (subparts.length > 1) {
                    properties.put(PropertyKeyConst.SERVER_ADDR, subparts[0]);
                    properties.put(PropertyKeyConst.NAMESPACE, subparts[1]);
                }
            } else {
                properties.put(PropertyKeyConst.SERVER_ADDR, parts[1]);
                properties.put(PropertyKeyConst.NAMESPACE, "default");
            }
        } else {
            properties.put(PropertyKeyConst.USERNAME, "nacos");
            properties.put(PropertyKeyConst.PASSWORD, "nacos");
            properties.put(PropertyKeyConst.NAMESPACE, "default");
        }
        return properties;
    }

  public static void main(String[] args) {
    Pattern addressPattern =
        Pattern.compile("([a-zA-Z\\d]+(:)[a-zA-Z\\d~!@&%#_]+@)?(.*)(:)\\d+(/[a-zA-Z\\d-]*)?$");
      boolean matched = addressPattern.matcher("nacos:nacos@127.0.0.1:8848/namespace").find();
    System.out.println(matched);

      matched = addressPattern.matcher("nacos:nacos@127.0.0.1:8848").find();
      System.out.println(matched);

      matched = addressPattern.matcher("127.0.0.1:8848").find();
      System.out.println(matched);
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
    public CommonProviderResponse registerInstance(CommonProviderRequest req) throws PolarisException {
        CommonProviderResponse response = new CommonProviderResponse();
        if (isRegisterEnable() && !isRegistered) {
            try {
                String instanceId = String.format(INSTANCE_NAME, req.getNamespace(), req.getService(), req.getHost(), req.getPort());
                Instance instance = new Instance();
                instance.setInstanceId(instanceId);
                instance.setClusterName(DEFAULT_CLUSTER_NAME);
                instance.setEnabled(true);
                instance.setEphemeral(true);
                instance.setPort(req.getPort());
                instance.setIp(req.getHost());
                instance.setHealthy(true);
                instance.setServiceName(req.getService());
                instance.setMetadata(req.getMetadata());
                // register with nacos naming service
                namingService.registerInstance(req.getService(), DEFAULT_GROUP, instance);
                isRegistered = true;
                response.setInstanceID(instanceId);
            } catch (NacosException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                    String.format("fail to register host %s:%d service %s", req.getHost(), req.getPort(),
                        req.getService()), e);
            }
        }
        return response;
    }

    @Override
    public void deregisterInstance(CommonProviderRequest req) throws PolarisException {
        if (isRegistered) {
            try {
                String instanceId = String.format(INSTANCE_NAME, req.getNamespace(), req.getService(), req.getHost(), req.getPort());
                Instance instance = new Instance();
                instance.setInstanceId(instanceId);
                instance.setClusterName(DEFAULT_CLUSTER_NAME);
                instance.setEnabled(true);
                instance.setEphemeral(true);
                instance.setPort(req.getPort());
                instance.setIp(req.getHost());
                instance.setHealthy(true);
                instance.setServiceName(req.getService());
                instance.setMetadata(req.getMetadata());
                // register with nacos naming service
                namingService.deregisterInstance(req.getService(), DEFAULT_GROUP, instance);
                isRegistered = false;
            } catch (NacosException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                    String.format("fail to deregister host %s:%d service %s", req.getHost(), req.getPort(),
                        req.getService()), e);
            }
        }
    }

    @Override
    public void heartbeat(CommonProviderRequest req) throws PolarisException {
        this.registerInstance(req);
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
            List<Instance> serviceList =
                this.namingService.getAllInstances(serviceUpdateTask.getServiceEventKey().getService(),
                    DEFAULT_GROUP, Lists.newArrayList(DEFAULT_CLUSTER_NAME));

            if (serviceList == null || serviceList.isEmpty()) {
                return null;
            }
            for (Instance service : serviceList) {
                DefaultInstance instance = new DefaultInstance();
                instance.setId(service.getInstanceId());
                instance.setService(service.getServiceName());
                instance.setHost(service.getIp());
                instance.setPort(service.getPort());
                instance.setHealthy(service.isHealthy());
                instance.setMetadata(service.getMetadata());
                instance.setIsolated(service.isEphemeral());
                instanceList.add(instance);
            }
            return new ServiceInstancesResponse(String.valueOf(System.currentTimeMillis()), instanceList);
        } catch (NacosException e) {
            throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
                String.format("Get service instances of %s sync failed.",
                    serviceUpdateTask.getServiceEventKey().getServiceKey()));
        }
    }

    @Override
    public Services syncGetServices(ServiceUpdateTask serviceUpdateTask) {
        Services services = new ServicesByProto(new ArrayList<>());
        try {
            ListView<String> serviceList = this.namingService.getServicesOfServer(1, NACOS_SERVICE_PAGESIZE, DEFAULT_GROUP);

            for (String instance : serviceList.getData()) {
                ServiceInfo serviceInfo = new ServiceInfo();
                serviceInfo.setService(instance);
                services.getServices().add(serviceInfo);
            }
        } catch (NacosException e) {
            throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
                String.format("Get services of %s instances sync failed.",
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
    public boolean isInitialized() {
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
            if (namingService != null) {
                try {
                    this.namingService.shutDown();
                } catch (NacosException ignore) {
                }
            }
        }
    }
}
