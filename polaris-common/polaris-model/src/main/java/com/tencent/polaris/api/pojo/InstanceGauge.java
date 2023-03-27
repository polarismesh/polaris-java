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

import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import java.util.Map;

/**
 * 服务上报数据信息
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface InstanceGauge extends Service {

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
     * 获取实例
     *
     * @return 实例对象
     */
    Instance getInstance();

    /**
     * 设置实例信息
     *
     * @param instance 实例数据
     */
    void setInstance(Instance instance);

    /**
     * 获取服务实例ID
     *
     * @return String
     */
    String getInstanceId();

    /**
     * 获取服务调用时延
     *
     * @return delay
     */
    Long getDelay();

    /**
     * 获取服务调用状态
     *
     * @return retStatus
     */
    RetStatus getRetStatus();

    /**
     * 服务调用返回码
     *
     * @return int
     */
    Integer getRetCode();

    /**
     * 获取实例分组
     *
     * @return subset
     */
    String getSubset();

    /**
     * 获取方法名
     *
     * @return method
     */
    String getMethod();

    /**
     * 获取主调服务信息
     *
     * @return Service
     */
    Service getCallerService();

    /**
     * 获取请求标签
     * 已废弃，请使用getSubsetMetadata
     *
     * @return labels
     */
    @Deprecated
    String getLabels();

    /**
     * 获取服务实例分组的过滤标签
     *
     * @return metadata
     */
    Map<String, MatchString> getSubsetMetadata();

    /**
     * 获取主调节点的IP信息
     *
     * @return callerIp
     */
    String getCallerIp();
}
