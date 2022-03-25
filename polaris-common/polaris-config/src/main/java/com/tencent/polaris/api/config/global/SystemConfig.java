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

package com.tencent.polaris.api.config.global;

import com.tencent.polaris.api.config.verify.Verifier;
import java.util.Map;

/**
 * api相关的配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface SystemConfig extends Verifier {

    /**
     * 获取流程缓存配置
     *
     * @return flowCacheConfig
     */
    FlowCacheConfig getFlowCache();

    /**
     * 获取服务发现集群地址
     *
     * @return discover
     */
    ClusterConfig getDiscoverCluster();

    /**
     * 获取配置中心集群信息
     *
     */
    ClusterConfig getConfigCluster();

    /**
     * 获取心跳集群地址
     *
     * @return healthCheck
     */
    ClusterConfig getHealthCheckCluster();

    /**
     * 获取monitor集群地址
     *
     * @return monitor
     */
    ClusterConfig getMonitorCluster();

    /**
     * 获取变量列表
     *
     * @return variables map
     */
    Map<String, String> getVariables();

}
