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

package com.tencent.polaris.circuitbreak.client.task;

import static com.tencent.polaris.api.plugin.registry.InstanceProperty.PROPERTY_CIRCUIT_BREAKER_STATUS;

import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreakResult;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreakResult.ResultKey;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.registry.InstanceProperty;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.plugin.registry.ServiceUpdateRequest;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pojo.ServiceInstancesByProto;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 熔断单个实例的任务
 */
public class InstancesCircuitBreakTask implements Runnable, Comparable<InstancesCircuitBreakTask> {

    @Override
    public int compareTo(InstancesCircuitBreakTask o) {
        return this.priority.ordinal() - o.priority.ordinal();
    }

    /**
     * 任务优先级
     */
    public enum TaskPriority {
        HIGH,
        LOW,
    }

    private static final Logger LOG = LoggerFactory.getLogger(InstancesCircuitBreakTask.class);

    private final ServiceKey serviceKey;

    private final String cbName;

    private final Collection<Instance> instances;

    private final String instId;

    private final Extensions extensions;

    private final TaskPriority priority;

    public InstancesCircuitBreakTask(ServiceKey serviceKey, String cbName, Collection<Instance> instances,
            String instId, Extensions extensions, TaskPriority priority) {
        this.serviceKey = serviceKey;
        this.cbName = cbName;
        this.instId = instId;
        this.instances = instances;
        this.extensions = extensions;
        this.priority = priority;
    }

    private Instance getInstance() {
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.INSTANCE);
        ResourceFilter resourceFilter = new ResourceFilter(serviceEventKey, true, true);
        ServiceInstancesByProto instances = (ServiceInstancesByProto) extensions.getLocalRegistry()
                .getInstances(resourceFilter);
        if (!instances.isInitialized()) {
            return null;
        }
        return instances.getInstance(instId);
    }

    @Override
    public void run() {
        Map<String, CircuitBreakResult> allResults = new HashMap<>();
        Set<ResultKey> statusChangedInstances = new HashSet<>();
        Collection<Instance> targetInstances = instances;
        if (StringUtils.isNotEmpty(instId)) {
            Instance instance = getInstance();
            if (null != instance) {
                targetInstances = new ArrayList<>();
                targetInstances.add(instance);
            }
        }
        if (CollectionUtils.isEmpty(targetInstances)) {
            return;
        }
        for (CircuitBreaker circuitBreaker : extensions.getCircuitBreakers()) {
            if (StringUtils.isNotBlank(cbName) && !cbName.equals(circuitBreaker.getName())) {
                continue;
            }
            CircuitBreakResult circuitBreakResult = circuitBreaker.checkInstance(targetInstances);
            if (null == circuitBreakResult || circuitBreakResult.isEmptyResult()) {
                continue;
            }
            cleanInstanceSet(circuitBreakResult.getInstancesToOpen(), statusChangedInstances);
            cleanInstanceSet(circuitBreakResult.getInstancesToHalfOpen(), statusChangedInstances);
            cleanInstanceSet(circuitBreakResult.getInstancesToClose(), statusChangedInstances);
            allResults.put(circuitBreaker.getName(), circuitBreakResult);
        }
        ServiceUpdateRequest updateRequest = buildServiceUpdateRequest(serviceKey, allResults);
        if (CollectionUtils.isEmpty(updateRequest.getProperties())) {
            return;
        }
        LOG.info("update cache for circuitbreaker, value is {}", updateRequest);
        extensions.getLocalRegistry().updateInstances(updateRequest);
    }

    private void cleanInstanceSet(Map<ResultKey, Instance> instanceSet, Set<ResultKey> allInstances) {
        Set<ResultKey> instIdsToRemove = new HashSet<>();
        for (Map.Entry<ResultKey, Instance> entry : instanceSet.entrySet()) {
            ResultKey resultKey = entry.getKey();
            if (allInstances.contains(resultKey)) {
                instIdsToRemove.add(resultKey);
            } else {
                allInstances.add(resultKey);
            }
        }
        instIdsToRemove.forEach(instanceSet::remove);
    }

    @SuppressWarnings("unchecked")
    private void buildInstanceProperty(long now, Map<ResultKey, Instance> results, int maxRequestAfterHalfOpen,
            Map<String, InstanceProperty> instanceProperties, String cbName, CircuitBreakerStatus.Status status) {
        if (MapUtils.isEmpty(results)) {
            return;
        }
        for (Map.Entry<ResultKey, Instance> entry : results.entrySet()) {
            ResultKey resultKey = entry.getKey();
            Instance instance = entry.getValue();
            String instId = resultKey.getInstId();
            InstanceProperty instanceProperty = instanceProperties.get(instId);
            if (null == instanceProperty) {
                Map<String, Object> properties = new HashMap<>();
                properties.put(PROPERTY_CIRCUIT_BREAKER_STATUS, new HashMap<StatusDimension, CircuitBreakerStatus>());
                instanceProperty = new InstanceProperty(instance, properties);
                instanceProperties.put(instId, instanceProperty);
            }
            Map<StatusDimension, CircuitBreakerStatus> statusMap = (Map<StatusDimension, CircuitBreakerStatus>)
                    instanceProperty.getProperties().get(PROPERTY_CIRCUIT_BREAKER_STATUS);
            statusMap.put(resultKey.getStatusDimension(),
                    new CircuitBreakerStatus(cbName, status, now));
        }
    }

    private ServiceUpdateRequest buildServiceUpdateRequest(
            ServiceKey serviceKey, Map<String, CircuitBreakResult> allResults) {
        Map<String, InstanceProperty> properties = new HashMap<>();
        allResults.forEach((cbName, result) -> {
            buildInstanceProperty(result.getCreateTimeMs(), result.getInstancesToOpen(),
                    result.getMaxRequestCountAfterHalfOpen(), properties, cbName, CircuitBreakerStatus.Status.OPEN);
            buildInstanceProperty(result.getCreateTimeMs(), result.getInstancesToHalfOpen(),
                    result.getMaxRequestCountAfterHalfOpen(), properties, cbName,
                    CircuitBreakerStatus.Status.HALF_OPEN);
            buildInstanceProperty(result.getCreateTimeMs(), result.getInstancesToClose(),
                    result.getMaxRequestCountAfterHalfOpen(), properties, cbName, CircuitBreakerStatus.Status.CLOSE);
        });
        return new ServiceUpdateRequest(serviceKey, properties.values());
    }
}
