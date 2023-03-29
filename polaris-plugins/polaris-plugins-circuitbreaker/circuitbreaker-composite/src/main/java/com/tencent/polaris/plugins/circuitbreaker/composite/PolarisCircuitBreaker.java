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

package com.tencent.polaris.plugins.circuitbreaker.composite;

import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceResourceProvider;
import com.tencent.polaris.client.flow.DefaultServiceResourceProvider;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;

public class PolarisCircuitBreaker extends Destroyable implements CircuitBreaker {

    private final Map<Level, Map<Resource, ResourceCounters>> countersCache = new HashMap<>();

    private final Map<Resource, ResourceHealthChecker> healthCheckCache = new HashMap<>();

    private final ScheduledExecutorService stateChangeExecutors = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("circuitbreaker-state-worker"));

    private final ScheduledExecutorService pullRulesExecutors = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("circuitbreaker-pull-rules-worker"));

    private final ScheduledExecutorService healthCheckExecutors = new ScheduledThreadPoolExecutor(4,
            new NamedThreadFactory("circuitbreaker-health-check-worker"));


    private final Map<Resource, CircuitBreakerRuleContainer> containers = new ConcurrentHashMap<>();

    private Extensions extensions;

    private ServiceResourceProvider serviceResourceProvider;

    private Map<String, HealthChecker> healthCheckers = Collections.emptyMap();

    @Override
    public CircuitBreakerStatus checkResource(Resource resource) {
        ResourceCounters resourceCounters = getResourceCounters(resource);
        if (null == resourceCounters) {
            return null;
        }
        return resourceCounters.getCircuitBreakerStatus();
    }

    private ResourceCounters getResourceCounters(Resource resource) {
        Map<Resource, ResourceCounters> resourceResourceCountersMap = countersCache.get(resource.getLevel());
        return resourceResourceCountersMap.get(resource);
    }

    @Override
    public void report(ResourceStat resourceStat) {
        Resource resource = resourceStat.getResource();
        if (resource.getLevel() == Level.UNKNOWN) {
            return;
        }
        RetStatus retStatus = resourceStat.getRetStatus();
        if (retStatus == RetStatus.RetReject || retStatus == RetStatus.RetFlowControl) {
            return;
        }
        ResourceCounters resourceCounters = getResourceCounters(resource);
        if (null == resourceCounters) {
            containers.computeIfAbsent(resource, new Function<Resource, CircuitBreakerRuleContainer>() {
                @Override
                public CircuitBreakerRuleContainer apply(Resource resource) {
                    return new CircuitBreakerRuleContainer(resource, PolarisCircuitBreaker.this);
                }
            });
        } else {
            resourceCounters.report(resourceStat);
            addInstanceForHealthCheck(resourceStat.getResource());
        }
    }

    private void addInstanceForHealthCheck(Resource resource) {
        if (!(resource instanceof InstanceResource)) {
            return;
        }
        InstanceResource instanceResource = (InstanceResource) resource;
        ResourceHealthChecker resourceHealthChecker = healthCheckCache.get(instanceResource.getServiceResource());
        if (null != resourceHealthChecker) {
            resourceHealthChecker.addInstance(instanceResource);
        }
    }

    @Override
    public PluginType getType() {
        return PluginTypes.CIRCUIT_BREAKER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        countersCache.put(Level.SERVICE, new ConcurrentHashMap<>());
        countersCache.put(Level.METHOD, new ConcurrentHashMap<>());
        countersCache.put(Level.GROUP, new ConcurrentHashMap<>());
        countersCache.put(Level.INSTANCE, new ConcurrentHashMap<>());
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
        this.extensions = extensions;
        serviceResourceProvider = new DefaultServiceResourceProvider(extensions);
        extensions.getLocalRegistry().registerResourceListener(new CircuitBreakerRuleListener(this));
        healthCheckers = extensions.getAllHealthCheckers();
    }

    // for test
    public void setServiceRuleProvider(ServiceResourceProvider serviceResourceProvider) {
        this.serviceResourceProvider = serviceResourceProvider;
    }

    @Override
    protected void doDestroy() {
        stateChangeExecutors.shutdown();
        pullRulesExecutors.shutdown();
        healthCheckExecutors.shutdown();
    }

    Map<Level, Map<Resource, ResourceCounters>> getCountersCache() {
        return countersCache;
    }

    Map<Resource, ResourceHealthChecker> getHealthCheckCache() {
        return healthCheckCache;
    }

    Extensions getExtensions() {
        return extensions;
    }

    ScheduledExecutorService getPullRulesExecutors() {
        return pullRulesExecutors;
    }

    ScheduledExecutorService getStateChangeExecutors() {
        return stateChangeExecutors;
    }

    ScheduledExecutorService getHealthCheckExecutors() {
        return healthCheckExecutors;
    }

    Map<Resource, CircuitBreakerRuleContainer> getContainers() {
        return containers;
    }

    public ServiceResourceProvider getServiceRuleProvider() {
        return serviceResourceProvider;
    }

    public Map<String, HealthChecker> getHealthCheckers() {
        return healthCheckers;
    }

    public void setHealthCheckers(
            Map<String, HealthChecker> healthCheckers) {
        this.healthCheckers = healthCheckers;
    }

    @Override
    public String getName() {
        return DefaultPlugins.CIRCUIT_BREAKER_COMPOSITE;
    }

}
