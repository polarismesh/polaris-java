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
import static com.tencent.polaris.api.plugin.registry.InstanceProperty.PROPERTY_DETECT_RESULT;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.plugin.registry.InstanceProperty;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.plugin.registry.ServiceUpdateRequest;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.OutlierDetectionStatus;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.MapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstancesDetectTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(InstancesDetectTask.class);

    private final Extensions extensions;

    private final AtomicBoolean destroy = new AtomicBoolean(false);

    public InstancesDetectTask(Extensions extensions) {
        this.extensions = extensions;
    }

    @Override
    public void run() {
        Set<ServiceKey> services = extensions.getLocalRegistry().getServices();
        for (ServiceKey serviceKey : services) {
            try {
                doInstanceDetectForService(serviceKey);
            } catch (PolarisException e) {
                LOG.error("fail to do instance detect for {}, e:{}", serviceKey, e);
            }
        }
    }

    /**
     * 注销任务
     */
    public void destroy() {
        destroy.set(true);
    }

    private void doInstanceDetectForService(ServiceKey serviceKey) throws PolarisException {
        ServiceEventKey svcEventKey = new ServiceEventKey(serviceKey, EventType.INSTANCE);
        ServiceInstances instances = extensions.getLocalRegistry().getInstances(
                new ResourceFilter(svcEventKey, true, true));
        if (!instances.isInitialized() || instances.getInstances().size() == 0) {
            return;
        }

        Map<Instance, DetectResult> aliveResults = new HashMap<>();
        for (Instance instance : instances.getInstances()) {
            if (destroy.get()) {
                // 如果要停止定时任务，则剩下的实例探测也没必要进行下去了
                break;
            }
            DetectResult result = detectInstance(instance);
            if (result == null || result.getRetStatus() != RetStatus.RetSuccess) {
                continue;
            }
            aliveResults.put(instance, result);

        }
        if (MapUtils.isNotEmpty(aliveResults)) {
            ServiceUpdateRequest updateRequest = buildInstanceUpdateResult(serviceKey, aliveResults);
            LOG.info("update cache for outlier detect, value is {}", updateRequest);
            extensions.getLocalRegistry().updateInstances(updateRequest);
        }
    }

    private ServiceUpdateRequest buildInstanceUpdateResult(ServiceKey serviceKey,
                                                           Map<Instance, DetectResult> aliveResults) {
        List<InstanceProperty> instances = new ArrayList<>();
        for (Map.Entry<Instance, DetectResult> entry : aliveResults.entrySet()) {
            Map<String, Object> properties = new HashMap<>();
            Optional.ofNullable(entry.getValue())
                    .ifPresent(detectResult -> {
                        properties.put(PROPERTY_DETECT_RESULT, detectResult);
                        InstanceProperty instanceProperty = new InstanceProperty(entry.getKey(), properties);
                        instances.add(instanceProperty);
                    });
        }

        return new ServiceUpdateRequest(serviceKey, instances);
    }

    private DetectResult detectInstance(Instance instance) throws PolarisException {
        for (HealthChecker detector : extensions.getHealthCheckers()) {
            DetectResult result = detector.detectInstance(instance);
            if (result == null) {
                continue;
            }
            if (result.getRetStatus() == RetStatus.RetSuccess) {
                return result;
            }
        }
        return null;
    }
}
