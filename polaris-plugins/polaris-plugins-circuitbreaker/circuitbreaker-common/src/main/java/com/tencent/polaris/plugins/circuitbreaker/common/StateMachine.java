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

package com.tencent.polaris.plugins.circuitbreaker.common;

import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.StatusDimension;
import java.util.Set;

/**
 * 状态机通用接口（用于处理熔断状态的转换的计算）
 *
 * @author andrewshan
 * @date 2019/8/26
 */
public interface StateMachine<T extends Verifier> {

    /**
     * 获取所有的实例统计维度
     *
     * @param instance 实例
     * @param parameter 参数
     * @return 统计维度列表
     */
    Set<StatusDimension> getStatusDimensions(Instance instance, Parameter parameter);

    /**
     * 熔断器从关闭状态转换为开启状态
     *
     * @param instance 节点实例信息
     * @param statusDimension 维度
     * @param parameter 状态机转换参数
     * @return 是否打开熔断器
     */
    boolean closeToOpen(Instance instance, StatusDimension statusDimension, Parameter parameter);

    /**
     * 熔断器从打开状态转换为半开状态
     *
     * @param instance 节点实例信息
     * @param statusDimension 维度
     * @param parameter 状态机转换参数
     * @return 是否半开熔断器
     */
    boolean openToHalfOpen(Instance instance, StatusDimension statusDimension, Parameter parameter);

    /**
     * 熔断器从半开状态转换为打开状态
     *
     * @param instance 节点实例信息
     * @param statusDimension 维度
     * @param parameter 状态机转换参数
     * @return 是否打开熔断器
     */
    boolean halfOpenToOpen(Instance instance, StatusDimension statusDimension, Parameter parameter);

    /**
     * 熔断器从半开状态转换为关闭状态
     *
     * @param instance 节点实例信息
     * @param statusDimension 维度
     * @param parameter 状态机转换参数
     * @return 是否关闭熔断器
     */
    boolean halfOpenToClose(Instance instance, StatusDimension statusDimension, Parameter parameter);

    /**
     * 状态机转换所需要的参数类
     */
    class Parameter {

        private final int pluginId;

        private final String circuitBreakerName;

        private final long currentTimeMs;

        private final int halfOpenMaxReqCount;

        public Parameter(int pluginId, String circuitBreakerName, int halfOpenMaxReqCount) {
            this.pluginId = pluginId;
            this.circuitBreakerName = circuitBreakerName;
            this.currentTimeMs = System.currentTimeMillis();
            this.halfOpenMaxReqCount = halfOpenMaxReqCount;
        }

        public int getPluginId() {
            return pluginId;
        }

        public String getCircuitBreakerName() {
            return circuitBreakerName;
        }

        public long getCurrentTimeMs() {
            return currentTimeMs;
        }

        public int getHalfOpenMaxReqCount() {
            return halfOpenMaxReqCount;
        }
    }
}
