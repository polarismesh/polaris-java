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

package com.tencent.polaris.api.plugin.registry;


import com.tencent.polaris.api.pojo.Instance;
import java.util.Map;

/**
 * 服务实例更新属性列表
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class InstanceProperty {

    /**
     * 属性标签，标识熔断状态
     */
    public static final String PROPERTY_CIRCUIT_BREAKER_STATUS = "circuitBreakerStatus";
    /**
     * 属性标签，标识故障探测结果
     */
    public static final String PROPERTY_DETECT_RESULT = "detectResult";

    /**
     * 属性标签，标识实例统计信息
     */
    public static final String PROPERTY_INSTANCE_STATISTIC = "instanceStatistic";

    private final Instance instance;

    private final Map<String, Object> properties;

    public InstanceProperty(Instance instance, Map<String, Object> properties) {
        this.instance = instance;
        this.properties = properties;
    }

    public Instance getInstance() {
        return instance;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "InstanceProperty{" +
                "id=" + instance.getId() +
                ", host=" + instance.getHost() +
                ", port=" + instance.getPort() +
                ", properties=" + properties +
                '}';
    }
}
