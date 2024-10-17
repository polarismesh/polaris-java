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

package com.tencent.polaris.api.plugin.weight;

import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.InstanceWeight;
import com.tencent.polaris.api.pojo.ServiceInstances;
import java.util.List;
import java.util.Map;

/**
 * 【扩展点接口】动态权重调整接口
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface WeightAdjuster extends Plugin {

    /**
     * Update dynamic weight.
     * @param dynamicWeight original dynamic weight
     * @param instances instances
     * @return updated dynamic weight
     */
    Map<String, InstanceWeight> timingAdjustDynamicWeight(Map<String, InstanceWeight> dynamicWeight,
            ServiceInstances instances);

    /**
     * 实时上报健康状态，并判断是否需要立刻进行动态权重调整，用于流量削峰.
     *
     * @param metric 实例调用信息
     * @return 是否需要立刻调整动态权重
     */
    boolean realTimeAdjustDynamicWeight(InstanceGauge metric);

}
