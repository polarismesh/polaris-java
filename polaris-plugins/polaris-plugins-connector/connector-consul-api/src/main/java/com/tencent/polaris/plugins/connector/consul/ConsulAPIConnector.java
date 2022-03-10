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

package com.tencent.polaris.plugins.connector.consul;

import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.INSTANCE_ID_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.IP_ADDRESS_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.PREFER_IP_ADDRESS_KEY;
import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.SERVICE_NAME_KEY;

import com.ecwid.consul.ConsulException;
import com.ecwid.consul.v1.ConsistencyMode;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.agent.model.NewService.Check;
import com.ecwid.consul.v1.catalog.CatalogServicesRequest;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
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
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.ServicesByProto;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implement of {@link ServerConnector} to connect to Consul Server.It provides methods to manage resources
 * relate to a service:
 * <ol>
 * <li>registerEventHandler/deRegisterEventHandler to subscribe instance/config for a service.
 * <li>registerInstance/deregisterInstance to register/deregister an instance.
 * <li>heartbeat to send heartbeat manually.
 * </ol>
 *
 * @author Haotian Zhang
 */
public class ConsulAPIConnector extends DestroyableServerConnector {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulAPIConnector.class);

    /**
     * If server connector initialized.
     */
    private boolean initialized = false;

    /**
     *
     */
    private boolean ieRegistered = false;

    private ConsulClient consulClient;

    private ConsulContext consulContext;

    @Override
    public String getName() {
        return DefaultPlugins.SERVER_CONNECTOR_CONSUL;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVER_CONNECTOR.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        if (!initialized) {
            List<ServerConnectorConfigImpl> serverConnectorConfigs = ctx.getConfig().getGlobal().getServerConnectors();
            if (CollectionUtils.isNotEmpty(serverConnectorConfigs)) {
                for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectorConfigs) {
                    if (DefaultPlugins.SERVER_CONNECTOR_CONSUL.equals(serverConnectorConfig.getProtocol())) {
                        initActually(ctx, serverConnectorConfig);
                    }
                }
            }
        }
    }

    private void initActually(InitContext ctx, ServerConnectorConfig connectorConfig) {
        String address = connectorConfig.getAddresses().get(0);
        String[] addressSplit = address.split(":");
        String agentHost = addressSplit[0];
        int agentPort = Integer.parseInt(addressSplit[1]);
        consulClient = new ConsulClient(agentHost, agentPort);

        // Init context.
        consulContext = new ConsulContext();
        Map<String, String> metadata = connectorConfig.getMetadata();
        if (metadata.containsKey(SERVICE_NAME_KEY) && StringUtils.isNotBlank(metadata.get(SERVICE_NAME_KEY))) {
            consulContext.setServiceName(metadata.get(SERVICE_NAME_KEY));
        }
        if (metadata.containsKey(INSTANCE_ID_KEY) && StringUtils.isNotBlank(metadata.get(INSTANCE_ID_KEY))) {
            consulContext.setInstanceId(metadata.get(INSTANCE_ID_KEY));
        }
        if (metadata.containsKey(IP_ADDRESS_KEY) && StringUtils.isNotBlank(metadata.get(IP_ADDRESS_KEY))) {
            consulContext.setIpAddress(metadata.get(IP_ADDRESS_KEY));
        }
        if (metadata.containsKey(PREFER_IP_ADDRESS_KEY) && StringUtils.isNotBlank(
                metadata.get(PREFER_IP_ADDRESS_KEY))) {
            consulContext.setPreferIpAddress(Boolean.parseBoolean(metadata.get(PREFER_IP_ADDRESS_KEY)));
        }
        initialized = true;
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
        if (!ieRegistered) {
            ServiceKey serviceKey = new ServiceKey(req.getNamespace(), req.getService());
            try {
                LOG.info("Registering service to Consul");
                NewService service = buildRegisterInstanceRequest(req);
                this.consulClient.agentServiceRegister(service);
                CommonProviderResponse resp = new CommonProviderResponse();
                consulContext.setInstanceId(service.getId());
                resp.setInstanceID(service.getId());
                resp.setExists(true);
                LOG.info("Registered service to Consul: " + service);
                ieRegistered = true;
                return resp;
            } catch (ConsulException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                        String.format("fail to register host %s:%d service %s", req.getHost(), req.getPort(),
                                serviceKey), e);
            }
        }
        return null;
    }

    private NewService buildRegisterInstanceRequest(CommonProviderRequest req) {
        NewService service = new NewService();
        String appName = req.getService();
        // Generate ip address
        if (consulContext.isPreferIpAddress()) {
            service.setAddress(consulContext.getIpAddress());
        } else {
            service.setAddress(req.getHost());
        }
        // Generate instance id
        if (StringUtils.isBlank(req.getInstanceID())) {
            if (StringUtils.isBlank(consulContext.getInstanceId())) {
                consulContext.setInstanceId(
                        appName + "-" + req.getHost().replace(":", "-") + "-" + req.getPort());
            }
            service.setId(consulContext.getInstanceId());
        } else {
            service.setId(req.getInstanceID());
        }

        service.setPort(req.getPort());
        if (StringUtils.isBlank(consulContext.getServiceName())) {
            consulContext.setServiceName(appName);
        }
        service.setName(consulContext.getServiceName());
        service.setMeta(req.getMetadata());
        if (null != req.getTtl()) {
            Check check = new Check();
            check.setTtl(req.getTtl() * 1.5 + "s");
            service.setCheck(check);
        }
        return service;
    }

    @Override
    public void deregisterInstance(CommonProviderRequest req) throws PolarisException {
        if (ieRegistered) {
            ServiceKey serviceKey = new ServiceKey(req.getNamespace(), req.getService());
            try {
                LOG.info("Unregistering service to Consul: " + consulContext.getInstanceId());
                this.consulClient.agentServiceDeregister(consulContext.getInstanceId());
                LOG.info("Unregistered service to Consul: " + consulContext.getInstanceId());
                ieRegistered = false;
            } catch (ConsulException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                        String.format("fail to deregister host %s:%d service %s", req.getHost(), req.getPort(),
                                serviceKey), e);
            }
        }
    }

    @Override
    public void heartbeat(CommonProviderRequest req) throws PolarisException {
        if (ieRegistered) {
            ServiceKey serviceKey = new ServiceKey(req.getNamespace(), req.getService());
            try {
                this.consulClient.agentCheckPass("service:" + consulContext.getInstanceId());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Heartbeat service to Consul: " + consulContext.getInstanceId());
                }
            } catch (ConsulException e) {
                throw new RetriableException(ErrorCode.NETWORK_ERROR,
                        String.format("fail to heartbeat id %s, host %s:%d service %s",
                                req.getInstanceID(), req.getHost(), req.getPort(), serviceKey), e);
            }
        }
    }

    @Override
    public List<DefaultInstance> syncGetServiceInstances(ServiceUpdateTask serviceUpdateTask) {
        List<DefaultInstance> instanceList = new ArrayList<>();
        try {
            HealthServicesRequest request = HealthServicesRequest.newBuilder()
                    .setQueryParams(new QueryParams(ConsistencyMode.DEFAULT))
                    .build();
            Response<List<HealthService>> response = this.consulClient
                    .getHealthServices(serviceUpdateTask.getServiceEventKey().getService(), request);
            if (response.getValue() == null || response.getValue().isEmpty()) {
                return Collections.emptyList();
            }
            for (HealthService service : response.getValue()) {
                DefaultInstance instance = new DefaultInstance();
                instance.setId(service.getService().getId());
                instance.setService(service.getService().getService());
                instance.setHost(service.getService().getAddress());
                instance.setPort(service.getService().getPort());
                instanceList.add(instance);
            }
        } catch (ConsulException e) {
            throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
                    String.format("Get service instances of %s sync failed.",
                            serviceUpdateTask.getServiceEventKey().getServiceKey()));
        }
        return instanceList;
    }

    @Override
    public Services syncGetServices(ServiceUpdateTask serviceUpdateTask) {
        Services services = new ServicesByProto();
        try {
            CatalogServicesRequest request = CatalogServicesRequest.newBuilder()
                    .setQueryParams(QueryParams.DEFAULT).build();
            ArrayList<String> serviceList = new ArrayList<>(
                    this.consulClient.getCatalogServices(request).getValue().keySet());
            for (String s : serviceList) {
                ServiceInfo serviceInfo = new ServiceInfo();
                serviceInfo.setService(s);
                services.getServices().add(serviceInfo);
            }
        } catch (ConsulException e) {
            throw ServerErrorResponseException.build(ErrorCode.SERVER_USER_ERROR.ordinal(),
                    String.format("Get services of %s instances sync failed.",
                            serviceUpdateTask.getServiceEventKey().getServiceKey()));
        }
        return services;
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
    public boolean isInitialized() {
        return initialized;
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
}
