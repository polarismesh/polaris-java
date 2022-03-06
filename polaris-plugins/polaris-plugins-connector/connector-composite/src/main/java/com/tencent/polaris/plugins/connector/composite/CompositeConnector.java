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
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import java.util.ArrayList;
import java.util.List;

/**
 * An implement of {@link ServerConnector} to connect to Multiple Naming Server.It provides methods to manage resources
 * relate to a service:
 * <ol>
 * <li>registerEventHandler/deRegisterEventHandler to subscribe instance/config for a service.
 * <li>registerInstance/deregisterInstance to register/deregister an instance.
 * <li>heartbeat to send heartbeat manually.
 * </ol>
 *
 * @author Haotian Zhang
 */
public class CompositeConnector extends DestroyableServerConnector {

    /**
     * Collection of server connector.
     */
    private List<DestroyableServerConnector> serverConnectors;

    /**
     * If server connector initialized.
     */
    private boolean initialized = false;

    @Override
    public String getName() {
        return DefaultPlugins.SERVER_CONNECTOR_COMPOSITE;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVER_CONNECTOR.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        if (getName().equals(ctx.getValueContext().getServerConnectorProtocol())) {
            if (CollectionUtils.isEmpty(serverConnectors)) {
                serverConnectors = new ArrayList<>();
            }
            List<ServerConnectorConfigImpl> serverConnectorConfigs = ctx.getConfig().getGlobal().getServerConnectors();
            for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectorConfigs) {
                DestroyableServerConnector serverConnector = (DestroyableServerConnector) ctx.getPlugins()
                        .getPlugin(PluginTypes.SERVER_CONNECTOR.getBaseType(), serverConnectorConfig.getProtocol());
                if (serverConnector.isInitialized()) {
                    serverConnectors.add(serverConnector);
                }
            }
            initialized = true;
        }
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {

    }

    @Override
    public void registerServiceHandler(ServiceEventHandler handler) throws PolarisException {

    }

    @Override
    public void deRegisterServiceHandler(ServiceEventKey eventKey) throws PolarisException {

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
    public ReportClientResponse reportClient(ReportClientRequest req) throws PolarisException {
        return null;
    }

    @Override
    public void updateServers(ServiceEventKey svcEventKey) {

    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void retryServiceUpdateTask(ServiceUpdateTask updateTask) {

    }

    @Override
    public void addLongRunningTask(ServiceUpdateTask serviceUpdateTask) {

    }
}
