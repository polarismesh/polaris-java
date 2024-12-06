/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.api.plugin.compose;

import com.tencent.polaris.api.config.global.ClusterConfig;
import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceKey;
import java.util.Collections;
import java.util.List;

public class ServerServiceInfo {

    private final ServiceKey serviceKey;

    private final ClusterType clusterType;

    private final long refreshIntervalMs;

    private final List<String> routers;

    private final String lbPolicy;

    public ServerServiceInfo(ClusterType clusterType, ClusterConfig clusterConfig)
            throws PolarisException {
        this.clusterType = clusterType;
        this.serviceKey = new ServiceKey(clusterConfig.getNamespace(), clusterConfig.getService());
        this.refreshIntervalMs = clusterConfig.getRefreshInterval();
        routers = Collections.unmodifiableList(clusterConfig.getRouters());
        lbPolicy = clusterConfig.getLbPolicy();
    }

    public ClusterType getClusterType() {
        return clusterType;
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    public ServiceKey getServiceKey() {
        return serviceKey;
    }

    public List<String> getRouters() {
        return routers;
    }

    public String getLbPolicy() {
        return lbPolicy;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ServerServiceInfo{" +
                "serviceKey=" + serviceKey +
                '}';
    }
}
