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

package com.tencent.polaris.api.pojo;

import java.util.Collection;
import java.util.function.Function;

/**
 * 实例本地属性值
 *
 * @author andrewshan
 * @date 2019/9/2
 */
public interface InstanceLocalValue {

    /**
     * 获取被熔断的接口列表
     *
     * @return 接口列表
     */
    Collection<StatusDimension> getStatusDimensions();

    /**
     * 获取熔断状态
     *
     * @param statusDimension 维度
     * @return CircuitBreakerStatus
     */
    CircuitBreakerStatus getCircuitBreakerStatus(StatusDimension statusDimension);

    /**
     * 设置熔断状态
     *
     * @param statusDimension 维度
     * @param circuitBreakerStatus 熔断状态
     */
    void setCircuitBreakerStatus(StatusDimension statusDimension, CircuitBreakerStatus circuitBreakerStatus);

    /**
     * 获取探测结果
     *
     * @return DetectResult
     */
    DetectResult getDetectResult();

    /**
     * 设置探测结果
     *
     * @param detectResult 探测结果
     */
    void setDetectResult(DetectResult detectResult);

    /**
     * 获取实例统计信息
     *
     * @return InstanceStatistic
     */
    InstanceStatistic getInstanceStatistic();

    /**
     * 设置实例统计信息
     *
     * @param instanceStatistic
     */
    void setInstanceStatistic(InstanceStatistic instanceStatistic);
    /**
     * 获取插件数据
     *
     * @param pluginId 插件ID
     * @param create 创建对象的函数
     * @return 插件数据
     */
    Object getPluginValue(int pluginId, Function<Integer, Object> create);

}
