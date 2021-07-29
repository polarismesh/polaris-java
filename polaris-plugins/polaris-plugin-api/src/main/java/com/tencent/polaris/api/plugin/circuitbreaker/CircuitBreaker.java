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

package com.tencent.polaris.api.plugin.circuitbreaker;

import com.tencent.polaris.api.plugin.IdAwarePlugin;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.Subset;
import java.util.Collection;

/**
 * 【扩展点接口】节点熔断器
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface CircuitBreaker extends IdAwarePlugin {

    /**
     * 定期进行实例熔断计算，返回需要进行状态转换的实例ID.
     *
     * @param instances 服务实例列表
     * @return 熔断结果
     */
    CircuitBreakResult checkInstance(Collection<Instance> instances);

    /**
     * 定期进行实例分组熔断计算
     *
     * @param subsets 实例分组
     * @return 熔断结果
     */
    CircuitBreakResult checkSubset(Collection<Subset> subsets);

    /**
     * 实时上报健康状态并进行连续失败熔断判断，返回当前实例是否需要进行立即熔断.
     *
     * @param metric 服务调用结果信息
     * @return 是否需要进行实时熔断
     */
    boolean stat(InstanceGauge metric);
}
