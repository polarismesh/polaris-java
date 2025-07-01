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

package com.tencent.polaris.api.plugin.stat;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.Service;

public interface CircuitBreakGauge extends Service {

    /**
     * 获取方法名
     *
     * @return method
     */
    String getMethod();

    /**
     * 获取实例分组
     *
     * @return subsets
     */
    String getSubset();

    /**
     * 获取节点信息
     *
     * @return host
     */
    String getHost();

    /**
     * 获取端口信息
     *
     * @return port
     */
    int getPort();

    /**
     * 获取服务实例ID
     *
     * @return String
     */
    String getInstanceId();

    /**
     * 回写实例ID
     *
     * @param instanceId 实例ID
     */
    void setInstanceId(String instanceId);

    /**
     * 获取主调服务信息
     *
     * @return Service
     */
    Service getCallerService();

    /**
     * 获取熔断状态
     *
     * @return 熔断状态
     */
    CircuitBreakerStatus getCircuitBreakStatus();

    /**
     * 获取熔断粒度
     *
     * @return 熔断粒度
     */
    String getLevel();

    /**
     * 获取生效的熔断规则名称
     *
     * @return 熔断规则名称
     */
    String getRuleName();
}
