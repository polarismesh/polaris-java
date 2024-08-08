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

package com.tencent.polaris.factory.config.consumer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.consumer.ConsumerConfig;
import com.tencent.polaris.api.config.consumer.WeightAdjustConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调用者配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class ConsumerConfigImpl implements ConsumerConfig {

    @JsonProperty
    private LocalCacheConfigImpl localCache;

    @JsonProperty
    private ServiceRouterConfigImpl serviceRouter;

    @JsonProperty
    private LoadBalanceConfigImpl loadbalancer;

    @JsonProperty
    private CircuitBreakerConfigImpl circuitBreaker;

    @JsonProperty
    private OutlierDetectionConfigImpl outlierDetection;

    @JsonProperty
    private SubscribeConfigImpl subscribe;

    @JsonProperty
    private ZeroProtectionConfigImpl zeroProtection;

    @JsonProperty
    private List<DiscoveryConfigImpl> discoveries;

    @JsonIgnore
    private final Map<String, DiscoveryConfigImpl> discoveryConfigMap = new ConcurrentHashMap<>();

    @JsonProperty
    private WeightAdjustConfigImpl weightAdjust;

    @Override
    public LocalCacheConfigImpl getLocalCache() {
        return localCache;
    }

    @Override
    public ServiceRouterConfigImpl getServiceRouter() {
        return serviceRouter;
    }

    @Override
    public LoadBalanceConfigImpl getLoadbalancer() {
        return loadbalancer;
    }

    public CircuitBreakerConfigImpl getCircuitBreaker() {
        return circuitBreaker;
    }

    @Override
    public OutlierDetectionConfigImpl getOutlierDetection() {
        return outlierDetection;
    }

    @Override
    public SubscribeConfigImpl getSubscribe() {
        return subscribe;
    }

    @Override
    public ZeroProtectionConfigImpl getZeroProtection() {
        return zeroProtection;
    }

    @Override
    public List<DiscoveryConfigImpl> getDiscoveries() {
        if (CollectionUtils.isEmpty(discoveries)) {
            discoveries = new ArrayList<>();
        }
        return discoveries;
    }

    private void setDiscoveryConfigMap(List<DiscoveryConfigImpl> discoveries) {
        if (CollectionUtils.isNotEmpty(discoveries)) {
            for (DiscoveryConfigImpl discoveryConfig : discoveries) {
                if (discoveryConfigMap.containsKey(discoveryConfig.getServerConnectorId())) {
                    throw new IllegalArgumentException(String.format("Discovery config of [%s] is already exist.",
                            discoveryConfig.getServerConnectorId()));
                } else {
                    discoveryConfigMap.put(discoveryConfig.getServerConnectorId(), discoveryConfig);
                }
            }
        }
    }

    @Override
    public Map<String, DiscoveryConfigImpl> getDiscoveryConfigMap() {
        return discoveryConfigMap;
    }

    @Override
    public WeightAdjustConfig getWeightAdjust() {
        return weightAdjust;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(localCache, "localCache");
        ConfigUtils.validateNull(serviceRouter, "serviceRouter");
        ConfigUtils.validateNull(loadbalancer, "loadbalancer");
        ConfigUtils.validateNull(circuitBreaker, "circuitBreaker");
        ConfigUtils.validateNull(outlierDetection, "outlierDetection");
        ConfigUtils.validateNull(weightAdjust, "weightAdjust");

        localCache.verify();
        serviceRouter.verify();
        loadbalancer.verify();
        circuitBreaker.verify();
        outlierDetection.verify();
        subscribe.verify();
        zeroProtection.verify();
        weightAdjust.verify();
        if (CollectionUtils.isNotEmpty(discoveries)) {
            for (DiscoveryConfigImpl discoveryConfig : discoveries) {
                discoveryConfig.verify();
            }
        }
        setDiscoveryConfigMap(discoveries);
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null == localCache) {
            localCache = new LocalCacheConfigImpl();
        }
        if (null == serviceRouter) {
            serviceRouter = new ServiceRouterConfigImpl();
        }
        if (null == loadbalancer) {
            loadbalancer = new LoadBalanceConfigImpl();
        }
        if (null == circuitBreaker) {
            circuitBreaker = new CircuitBreakerConfigImpl();
        }
        if (null == outlierDetection) {
            outlierDetection = new OutlierDetectionConfigImpl();
        }
        if (null == subscribe) {
            subscribe = new SubscribeConfigImpl();
        }
        if (null == zeroProtection) {
            zeroProtection = new ZeroProtectionConfigImpl();
        }
        if (null == weightAdjust) {
            weightAdjust = new WeightAdjustConfigImpl();
        }
        if (null != defaultObject) {
            ConsumerConfig consumerConfig = (ConsumerConfig) defaultObject;
            localCache.setDefault(consumerConfig.getLocalCache());
            serviceRouter.setDefault(consumerConfig.getServiceRouter());
            loadbalancer.setDefault(consumerConfig.getLoadbalancer());
            circuitBreaker.setDefault(consumerConfig.getCircuitBreaker());
            outlierDetection.setDefault(consumerConfig.getOutlierDetection());
            subscribe.setDefault(consumerConfig.getSubscribe());
            zeroProtection.setDefault(consumerConfig.getZeroProtection());
            weightAdjust.setDefault(consumerConfig.getWeightAdjust());
            if (CollectionUtils.isNotEmpty(discoveries)) {
                for (DiscoveryConfigImpl discoveryConfig : discoveries) {
                    discoveryConfig.setDefault(consumerConfig.getDiscoveries().get(0));
                }
            } else {
                discoveries = new ArrayList<>();
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ConsumerConfigImpl{" +
                "localCache=" + localCache +
                ", serviceRouter=" + serviceRouter +
                ", loadbalancer=" + loadbalancer +
                ", circuitBreaker=" + circuitBreaker +
                ", outlierDetection=" + outlierDetection +
                '}';
    }
}
