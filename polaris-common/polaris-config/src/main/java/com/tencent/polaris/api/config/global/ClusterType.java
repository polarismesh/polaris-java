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

/**
 * 集群类型
 */
public enum ClusterType {
    //埋点集群
    BUILTIN_CLUSTER,
    //服务注册中心
    SERVICE_DISCOVER_CLUSTER,
    //配置中心集群
    SERVICE_CONFIG_CLUSTER,
    //健康检查集群
    HEALTH_CHECK_CLUSTER,
    //监控集群
    MONITOR_CLUSTER,
}
