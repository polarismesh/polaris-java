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

package com.tencent.polaris.api.pojo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务实例通用接口
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface Instance extends BaseInstance, Comparable<Instance> {

    /**
     * 默认权重为100
     */
    int DEFAULT_WEIGHT = 100;

    String getRevision();

    /**
     * 获取整体熔断状态
     *
     * @return 熔断状态
     */
    CircuitBreakerStatus getCircuitBreakerStatus();

    /**
     * @return 接口列表
     * @deprecated 获取熔断的接口列表
     */
    Collection<StatusDimension> getStatusDimensions();

    /**
     * 支持按接口等维度获取熔断状态
     *
     * @param statusDimension 维度
     * @return 熔断状态
     */
    CircuitBreakerStatus getCircuitBreakerStatus(StatusDimension statusDimension);

    boolean isHealthy();

    boolean isIsolated();

    String getProtocol();

    String getId();

    String getVersion();

    Map<String, String> getMetadata();

    boolean isEnableHealthCheck();

    String getRegion();

    String getZone();

    String getCampus();

    int getPriority();

    int getWeight();

    String getLogicSet();

    Long getCreateTime();

    default Map<String, String> getServiceMetadata() {
        return new HashMap<>();
    }

    static Instance createDefaultInstance(String instId, String namespace, String service, String host, int port) {
        DefaultInstance defaultInstance = new DefaultInstance();
        defaultInstance.setHealthy(true);
        defaultInstance.setIsolated(false);
        defaultInstance.setId(instId);
        defaultInstance.setNamespace(namespace);
        defaultInstance.setService(service);
        defaultInstance.setHost(host);
        defaultInstance.setPort(port);
        return defaultInstance;
    }
}
