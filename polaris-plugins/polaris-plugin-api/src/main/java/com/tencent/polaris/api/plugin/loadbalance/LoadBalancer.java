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

package com.tencent.polaris.api.plugin.loadbalance;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.rpc.Criteria;

/**
 * 【扩展点接口】负载均衡
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface LoadBalancer extends Plugin {

    /**
     * 进行负载均衡，选择一个实例
     *
     * @param criteria 负载均衡的关键因子
     * @param instances 服务实例列表
     * @return 单个服务实例
     * @throws PolarisException SDK被销毁则抛出异常
     */
    Instance chooseInstance(Criteria criteria, ServiceInstances instances) throws PolarisException;
}
