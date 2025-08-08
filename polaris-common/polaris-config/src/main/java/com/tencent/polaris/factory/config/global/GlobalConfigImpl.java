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

package com.tencent.polaris.factory.config.global;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.api.config.global.LocationConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    @JsonIgnore
    private Map<String, ServerConnectorConfigImpl> serverConnectorConfigMap = new ConcurrentHashMap<>();

    @JsonProperty
    private StatReporterConfigImpl statReporter;

    @JsonProperty
    private TraceReporterConfigImpl traceReporter;

    @JsonProperty
    private EventReporterConfigImpl eventReporter;

    @JsonProperty
    private AdminConfigImpl admin;

    @JsonProperty
    private LocationConfigImpl location;

    @JsonProperty
    private CertificateConfigImpl certificate;

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

    @Override
    public LocationConfig getLocation() {
        return location;
    }

    public void setServerConnector(ServerConnectorConfigImpl serverConnector) {
        this.serverConnector = serverConnector;
    }

    @Override
    public List<ServerConnectorConfigImpl> getServerConnectors() {
        return serverConnectors;
    }

    public void setServerConnectors(List<ServerConnectorConfigImpl> serverConnectors) {
        for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectors) {
            if (serverConnectorConfigMap.containsKey(serverConnectorConfig.getId())) {
                throw new IllegalArgumentException(
                        String.format("Server connector config of [%s] is already exist.",
                                serverConnectorConfig.getId()));
            } else {
                serverConnectorConfigMap.put(serverConnectorConfig.getId(), serverConnectorConfig);
            }
        }
        this.serverConnectors = serverConnectors;
    }

    public Map<String, ServerConnectorConfigImpl> getServerConnectorConfigMap() {
        return serverConnectorConfigMap;
    }

    @Override
    public StatReporterConfigImpl getStatReporter() {
        return statReporter;
    }

    @Override
    public TraceReporterConfigImpl getTraceReporter() {
        return traceReporter;
    }

    @Override
    public EventReporterConfigImpl getEventReporter() {
        return eventReporter;
    }

    @Override
    public AdminConfigImpl getAdmin() {
        return admin;
    }

    @Override
    public CertificateConfigImpl getCertificate() {
        return certificate;
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
        ConfigUtils.validateNull(traceReporter, "traceReporter");
        ConfigUtils.validateNull(eventReporter, "eventReporter");
        ConfigUtils.validateNull(admin, "admin");
        ConfigUtils.validateNull(certificate, "certificate");

        system.verify();
        api.verify();

        if (CollectionUtils.isNotEmpty(serverConnectors)) {
            for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectors) {
                serverConnectorConfig.verify();
            }
        } else {
            ConfigUtils.validateTrue(DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(serverConnector.getProtocol()),
                    "The protocol of server connector(not server connectors) is polaris");
            serverConnector.verify();
            serverConnectorConfigMap.put(serverConnector.getId(), serverConnector);
        }
        statReporter.verify();
        traceReporter.verify();
        eventReporter.verify();
        admin.verify();
        certificate.verify();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null == system) {
            system = new SystemConfigImpl();
        }
        if (null == api) {
            api = new APIConfigImpl();
        }
        if (null == serverConnector) {
            serverConnector = new ServerConnectorConfigImpl();
        }
        if (null == statReporter) {
            statReporter = new StatReporterConfigImpl();
        }
        if (null == traceReporter) {
            traceReporter = new TraceReporterConfigImpl();
        }
        if (null == eventReporter) {
            eventReporter = new EventReporterConfigImpl();
        }
        if (null == admin) {
            admin = new AdminConfigImpl();
        }
        if (null == location) {
            location = new LocationConfigImpl();
        }
        if (null == certificate) {
            certificate = new CertificateConfigImpl();
        }
        if (null != defaultObject) {
            GlobalConfig globalConfig = (GlobalConfig) defaultObject;
            system.setDefault(globalConfig.getSystem());
            api.setDefault(globalConfig.getAPI());
            // Only grpc server connector should be set default.
            boolean ifInit = false;
            if (CollectionUtils.isNotEmpty(serverConnectors)) {
                for (ServerConnectorConfigImpl serverConnectorConfig : serverConnectors) {
                    if (DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(serverConnectorConfig.getProtocol())) {
                        serverConnectorConfig.setDefault(globalConfig.getServerConnector());
                        serverConnector = serverConnectorConfig;
                        ifInit = true;
                    }
                }
            }
            if (!ifInit) {
                serverConnector.setDefault(globalConfig.getServerConnector());
            }
            statReporter.setDefault(globalConfig.getStatReporter());
            traceReporter.setDefault(globalConfig.getTraceReporter());
            eventReporter.setDefault(globalConfig.getEventReporter());
            admin.setDefault(globalConfig.getAdmin());
            location.setDefault(globalConfig.getLocation());
            certificate.setDefault(globalConfig.getCertificate());
        }
    }

    @Override
    public String toString() {
        return "GlobalConfigImpl{" +
                "system=" + system +
                ", api=" + api +
                ", serverConnector=" + serverConnector +
                ", serverConnectors=" + serverConnectors +
                ", statReporter=" + statReporter +
                ", traceReporter=" + traceReporter +
                ", eventReporter=" + eventReporter +
                ", admin=" + admin +
                ", location=" + location +
                ", certificate=" + certificate +
                '}';
    }
}
