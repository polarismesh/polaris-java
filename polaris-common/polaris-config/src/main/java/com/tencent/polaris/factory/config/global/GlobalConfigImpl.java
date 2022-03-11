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

package com.tencent.polaris.factory.config.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.util.ConfigUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局配置对象
 *
 * @author andrewshan, Haotian Zhang
 */
public class GlobalConfigImpl implements GlobalConfig {

    @JsonProperty
    private SystemConfigImpl system;

    @JsonProperty
    private APIConfigImpl api;

    @JsonProperty
    private ServerConnectorConfigImpl serverConnector;

    @JsonProperty
    private List<ServerConnectorConfigImpl> serverConnectors;

    @JsonProperty
    private StatReporterConfigImpl statReporter;

    @Override
    public SystemConfigImpl getSystem() {
        return system;
    }

    @Override
    public APIConfigImpl getAPI() {
        return api;
    }

    @Override
    public ServerConnectorConfigImpl getServerConnector() {
        return serverConnector;
    }

    public void setServerConnector(ServerConnectorConfigImpl serverConnector) {
        this.serverConnector = serverConnector;
    }

    @Override
    public List<ServerConnectorConfigImpl> getServerConnectors() {
        return serverConnectors;
    }

    @Override
    public StatReporterConfigImpl getStatReporter() {
        return statReporter;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(system, "system");
        ConfigUtils.validateNull(api, "api");
        Map<String, Object> validateMap = new HashMap<>();
        validateMap.put("serverConnector", serverConnector);
        validateMap.put("serverConnectors", serverConnectors);
        ConfigUtils.validateAllNull(validateMap);
        ConfigUtils.validateNull(statReporter, "statReporter");

        system.verify();
        api.verify();

        boolean hasGrpc = false;
        if (CollectionUtils.isNotEmpty(serverConnectors)) {
            for (ServerConnectorConfig serverConnectorConfig : serverConnectors) {
                serverConnectorConfig.verify();
                if (DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(serverConnectorConfig.getProtocol())) {
                    hasGrpc = true;
                }
            }
        } else {
            serverConnector.verify();
            hasGrpc = DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(serverConnector.getProtocol());
        }
        ConfigUtils.validateTrue(hasGrpc, "HasGRPC");
        statReporter.verify();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null == system) {
            system = new SystemConfigImpl();
        }
        if (null == api) {
            api = new APIConfigImpl();
        }
        if (CollectionUtils.isEmpty(serverConnectors) && null == serverConnector) {
            serverConnector = new ServerConnectorConfigImpl();
        }
        if (null == statReporter) {
            statReporter = new StatReporterConfigImpl();
        }
        if (null != defaultObject) {
            GlobalConfig globalConfig = (GlobalConfig) defaultObject;
            system.setDefault(globalConfig.getSystem());
            api.setDefault(globalConfig.getAPI());
            // Only grpc server connector should be set default.
            if (CollectionUtils.isNotEmpty(serverConnectors)) {
                for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectors) {
                    if (DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(serverConnectorConfig.getProtocol())) {
                        serverConnectorConfig.setDefault(globalConfig.getServerConnector());
                        serverConnector = serverConnectorConfig;
                    }
                }
            } else {
                serverConnector.setDefault(globalConfig.getServerConnector());
            }
            statReporter.setDefault(globalConfig.getStatReporter());
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "GlobalConfigImpl{" +
                "system=" + system +
                ", api=" + api +
                ", serverConnector=" + serverConnector +
                ", serverConnectors=" + serverConnectors +
                ", statReporter=" + statReporter +
                '}';
    }
}
