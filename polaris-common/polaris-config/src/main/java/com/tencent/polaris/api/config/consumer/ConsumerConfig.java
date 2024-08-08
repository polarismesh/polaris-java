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

package com.tencent.polaris.api.config.consumer;

import com.tencent.polaris.api.config.verify.Verifier;

import java.util.List;
import java.util.Map;

/**
 * 调用者配置对象
 *
 * @author andrewshan, Haotian Zhang
 */
public interface ConsumerConfig extends Verifier {

    /**
     * services.consumer.localCache前缀开头的所有配置
     *
     * @return LocalCacheConfig
     */
    LocalCacheConfig getLocalCache();

    /**
     * services.consumer.serviceRouter前缀开头的所有配置
     *
     * @return ServiceRouterConfig
     */
    ServiceRouterConfig getServiceRouter();

    /**
     * services.consumer.loadbalancer前缀开头的所有配置
     *
     * @return LoadBalanceConfig
     */
    LoadBalanceConfig getLoadbalancer();

    /**
     * services.consumer.circuitbreaker前缀开头的所有配置
     *
     * @return CircuitBreakerConfig
     */
    CircuitBreakerConfig getCircuitBreaker();

    /**
     * services.consumer.outlierDetection前缀开头的所有配置
     *
     * @return OutlierDetectionConfig
     */
    OutlierDetectionConfig getOutlierDetection();

    /**
     * services.consumer.subscribe前缀开头的所有配置
     *
     * @return SubscribeConfig
     */
    SubscribeConfig getSubscribe();

    /**
     * consumer.zeroProtection前缀开头的所有配置
     *
     * @return ZeroProtectionConfig
     */
    ZeroProtectionConfig getZeroProtection();

    /**
     * Configuration of prefix of "consumer.discoveries".
     *
     * @return List of {@link DiscoveryConfig}
     */
    List<? extends DiscoveryConfig> getDiscoveries();

    /**
     * Configuration of prefix of "consumer.discoveries".
     *
     * @return Map of {@link DiscoveryConfig}
     */
    Map<String, ? extends DiscoveryConfig> getDiscoveryConfigMap();

    WeightAdjustConfig getWeightAdjust();
}
