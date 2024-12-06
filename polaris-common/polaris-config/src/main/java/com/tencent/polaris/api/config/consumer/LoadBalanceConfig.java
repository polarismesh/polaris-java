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

package com.tencent.polaris.api.config.consumer;

import com.tencent.polaris.api.config.plugin.PluginConfig;
import com.tencent.polaris.api.config.verify.Verifier;

/**
 * 负载均衡相关配置项
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface LoadBalanceConfig extends PluginConfig, Verifier {

    /**
     * 权重随机负载均衡插件名
     */
    String LOAD_BALANCE_WEIGHTED_RANDOM = "weightedRandom";

    /**
     * 权重一致性负载均衡插件名
     */
    String LOAD_BALANCE_RING_HASH = "ringHash";

    /**
     * 权重轮训负载均衡插件名
     */
    String LOAD_BALANCE_WEIGHTED_ROUND_ROBIN = "weightedRoundRobin";

    /**
     * 负载均衡类型
     *
     * @return String
     */
    String getType();

}
