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
import com.tencent.polaris.api.config.global.SystemConfig;
import com.tencent.polaris.factory.util.ConfigUtils;
import java.util.Collections;
import java.util.Map;

/**
 * api相关的配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class SystemConfigImpl implements SystemConfig {

    @JsonProperty
    private FlowCacheConfigImpl flowCache;

    @JsonProperty
    private ClusterConfigImpl discoverCluster;

    @JsonProperty
    private ClusterConfigImpl configCluster;

    @JsonProperty
    private ClusterConfigImpl healthCheckCluster;

    @JsonProperty
    private ClusterConfigImpl monitorCluster;

    @JsonProperty
    private Map<String, String> variables;

    @Override
    public ClusterConfigImpl getDiscoverCluster() {
        return discoverCluster;
    }

    @Override
    public ClusterConfigImpl getConfigCluster() {
        return configCluster;
    }

    @Override
    public ClusterConfigImpl getHealthCheckCluster() {
        return healthCheckCluster;
    }

    @Override
    public ClusterConfigImpl getMonitorCluster() {
        return monitorCluster;
    }

    @Override
    public FlowCacheConfigImpl getFlowCache() {
        return flowCache;
    }

    @Override
    public Map<String, String> getVariables() {
        if (null == variables) {
            return Collections.emptyMap();
        }
        return variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(flowCache, "system.flowCache");
        ConfigUtils.validateNull(discoverCluster, "system.discoverCluster");
        ConfigUtils.validateNull(discoverCluster, "system.configCluster");
        ConfigUtils.validateNull(healthCheckCluster, "system.healthCheckCluster");
        ConfigUtils.validateNull(monitorCluster, "system.monitorCluster");
        flowCache.verify();
        discoverCluster.verify();
        configCluster.verify();
        healthCheckCluster.verify();
        monitorCluster.verify();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null == discoverCluster) {
            discoverCluster = new ClusterConfigImpl();
        }
        if (null == configCluster) {
            configCluster = new ClusterConfigImpl();
        }
        if (null == healthCheckCluster) {
            healthCheckCluster = new ClusterConfigImpl();
        }
        if (null == monitorCluster) {
            monitorCluster = new ClusterConfigImpl();
        }
        if (null == flowCache) {
            flowCache = new FlowCacheConfigImpl();
        }
        if (null != defaultObject) {
            SystemConfig systemConfig = (SystemConfig) defaultObject;
            discoverCluster.setDefault(systemConfig.getDiscoverCluster());
            configCluster.setDefault(systemConfig.getConfigCluster());
            healthCheckCluster.setDefault(systemConfig.getHealthCheckCluster());
            monitorCluster.setDefault(systemConfig.getMonitorCluster());
            flowCache.setDefault(systemConfig.getFlowCache());
            if (null == variables) {
                setVariables(systemConfig.getVariables());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "SystemConfigImpl{" +
                ", discoverCluster=" + discoverCluster +
                ", configCluster=" + configCluster +
                ", healthCheckCluster=" + healthCheckCluster +
                ", monitorCluster=" + monitorCluster +
                ", variables=" + variables +
                '}';
    }
}

