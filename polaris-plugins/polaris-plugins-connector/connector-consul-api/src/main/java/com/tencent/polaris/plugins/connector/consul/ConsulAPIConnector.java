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

import com.ecwid.consul.v1.ConsistencyMode;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.exception.PolarisException;
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
import com.tencent.polaris.api.pojo.Services;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    /**
     * If server connector initialized.
     */
    private boolean initialized = false;

    private ConsulClient consulClient;

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
        return null;
    }

    @Override
    public void deregisterInstance(CommonProviderRequest req) throws PolarisException {

    }

    @Override
    public void heartbeat(CommonProviderRequest req) throws PolarisException {

    }

    @Override
    public List<DefaultInstance> syncGetServiceInstances(ServiceUpdateTask serviceUpdateTask) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setQueryParams(new QueryParams(ConsistencyMode.DEFAULT))
                .build();
        Response<List<HealthService>> response = this.consulClient
                .getHealthServices(serviceUpdateTask.getServiceEventKey().getService(), request);
        if (response.getValue() == null || response.getValue().isEmpty()) {
            return Collections.emptyList();
        }
        List<DefaultInstance> instanceList = new ArrayList<>();
        for (HealthService service : response.getValue()) {
            DefaultInstance instance = new DefaultInstance();
            instance.setService(service.getService().getService());
            instance.setHost(service.getService().getAddress());
            instance.setPort(service.getService().getPort());
            instanceList.add(instance);
        }
        return instanceList;
    }

    @Override
    public Services syncGetServices(ServiceUpdateTask serviceUpdateTask) {
        return super.syncGetServices(serviceUpdateTask);
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
