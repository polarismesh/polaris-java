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

package com.tencent.polaris.api.plugin.server;

import com.tencent.polaris.api.config.global.ClusterType;
import com.tencent.polaris.api.pojo.ServiceEventKey;

/**
 * 服务监听器
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class ServiceEventHandler {

    /**
     * 服务的唯一KEY
     */
    private final ServiceEventKey serviceEventKey;

    /**
     * 集群类型
     */
    private ClusterType targetCluster;

    /**
     * 服务定期刷新时间
     */
    private long refreshInterval;

    /**
     * 事件回调函数
     */
    private final EventHandler eventHandler;

    /**
     * 保存上一次的更新时间
     */
    private long lastUpdateTimeMs;

    public ServiceEventHandler(ServiceEventKey serviceEventKey, EventHandler eventHandler) {
        this.serviceEventKey = serviceEventKey;
        this.eventHandler = eventHandler;
    }

    public ClusterType getTargetCluster() {
        return targetCluster;
    }

    public void setTargetCluster(ClusterType targetCluster) {
        this.targetCluster = targetCluster;
    }

    public void setRefreshInterval(long refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public ServiceEventKey getServiceEventKey() {
        return serviceEventKey;
    }

    public long getRefreshIntervalMs() {
        return refreshInterval;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public long getLastUpdateTimeMs() {
        return lastUpdateTimeMs;
    }

    public void setLastUpdateTimeMs(long lastUpdateTimeMs) {
        this.lastUpdateTimeMs = lastUpdateTimeMs;
    }

}
