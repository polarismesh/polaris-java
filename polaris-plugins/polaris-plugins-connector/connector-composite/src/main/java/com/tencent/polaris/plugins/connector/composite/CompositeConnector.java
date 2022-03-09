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
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import com.tencent.polaris.plugins.connector.common.DestroyableServerConnector;
import com.tencent.polaris.plugins.connector.common.ServiceUpdateTask;
import com.tencent.polaris.plugins.connector.common.constant.ServiceUpdateTaskConstant.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(CompositeConnector.class);

    /**
     * Collection of server connector.
     */
    private List<DestroyableServerConnector> serverConnectors;
    /**
     * If server connector initialized.
     */
    private boolean initialized = false;
    /**
     * Thread pool for sending request to discovery server.
     */
    private ScheduledThreadPoolExecutor sendDiscoverExecutor;
    /**
     * Thread pool for updating service information.
     */
    private ScheduledThreadPoolExecutor updateServiceExecutor;

    @Override
    public String getName() {
        return DefaultPlugins.SERVER_CONNECTOR_COMPOSITE;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVER_CONNECTOR.getBaseType();
    }

    public List<DestroyableServerConnector> getServerConnectors() {
        return serverConnectors;
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
                serverConnector.init(ctx);
                serverConnectors.add(serverConnector);
            }
            sendDiscoverExecutor = new ScheduledThreadPoolExecutor(1,
                    new NamedThreadFactory(getName() + "-send-discovery"), new CallerRunsPolicy());
            sendDiscoverExecutor.setMaximumPoolSize(1);
            updateServiceExecutor = new ScheduledThreadPoolExecutor(1,
                    new NamedThreadFactory(getName() + "-update-service"));
            updateServiceExecutor.setMaximumPoolSize(1);
            initialized = true;
        }
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        if (initialized) {
            this.updateServiceExecutor.scheduleWithFixedDelay(new UpdateServiceTask(), TASK_RETRY_INTERVAL_MS,
                    TASK_RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void registerServiceHandler(ServiceEventHandler handler) throws PolarisException {
        checkDestroyed();
        ServiceUpdateTask serviceUpdateTask = new CompositeServiceUpdateTask(handler, this);
        submitServiceHandler(serviceUpdateTask, 0);
    }

    @Override
    public void deRegisterServiceHandler(ServiceEventKey eventKey) throws PolarisException {
        checkDestroyed();
        ServiceUpdateTask serviceUpdateTask = updateTaskSet.get(eventKey);
        if (null != serviceUpdateTask) {
            boolean result = serviceUpdateTask.setType(Type.LONG_RUNNING, Type.TERMINATED);
            LOG.info("[ServerConnector]success to deRegister updateServiceTask {}, result is {}", eventKey, result);
        }
    }

    @Override
    public CommonProviderResponse registerInstance(CommonProviderRequest req) throws PolarisException {
        checkDestroyed();
        CommonProviderResponse response = null;
        for (DestroyableServerConnector sc : serverConnectors) {
            CommonProviderResponse temp = sc.registerInstance(req);
            if (DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(sc.getName())) {
                response = temp;
            }
        }
        return response;
    }

    @Override
    public void deregisterInstance(CommonProviderRequest req) throws PolarisException {
        checkDestroyed();
        for (DestroyableServerConnector sc : serverConnectors) {
            sc.deregisterInstance(req);
        }
    }

    @Override
    public void heartbeat(CommonProviderRequest req) throws PolarisException {
        checkDestroyed();
        for (DestroyableServerConnector sc : serverConnectors) {
            sc.heartbeat(req);
        }
    }

    @Override
    public ReportClientResponse reportClient(ReportClientRequest req) throws PolarisException {
        checkDestroyed();
        ReportClientResponse response = null;
        for (DestroyableServerConnector sc : serverConnectors) {
            ReportClientResponse temp = sc.reportClient(req);
            if (DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(sc.getName())) {
                response = temp;
            }
        }
        return response;
    }

    @Override
    public void updateServers(ServiceEventKey svcEventKey) {
        for (DestroyableServerConnector sc : serverConnectors) {
            sc.updateServers(svcEventKey);
        }
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    protected void submitServiceHandler(ServiceUpdateTask updateTask, long delayMs) {
        LOG.debug("[ServerConnector]task for service {} has been scheduled discover", updateTask);
        sendDiscoverExecutor.schedule(updateTask, delayMs, TimeUnit.MILLISECONDS);
    }
}
