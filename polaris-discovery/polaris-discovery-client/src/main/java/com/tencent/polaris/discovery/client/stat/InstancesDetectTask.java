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

package com.tencent.polaris.discovery.client.stat;

import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig.When;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.detect.HealthChecker;
import com.tencent.polaris.api.plugin.registry.InstanceProperty;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.plugin.registry.ServiceUpdateRequest;
import com.tencent.polaris.api.pojo.*;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.MapUtils;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tencent.polaris.api.plugin.registry.InstanceProperty.PROPERTY_DETECT_RESULT;

public class InstancesDetectTask implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(InstancesDetectTask.class);

    private final Extensions extensions;
    private final When when;

    private final AtomicBoolean destroy = new AtomicBoolean(false);

    private final Set<ServiceKey> serviceKeySet;

    public InstancesDetectTask(Extensions extensions, When when) {
        this.extensions = extensions;
        this.when = when;
        this.serviceKeySet = new HashSet<>();
    }

    @Override
    public void run() {
        Set<ServiceKey> services;
        if (when == When.after_call) {
            services = serviceKeySet;
        } else {
            services = extensions.getLocalRegistry().getServices();
        }
        for (ServiceKey serviceKey : services) {
            if (Objects.equals(serviceKey.getNamespace(), "Polaris")) {
                continue; // 北极星内部服务器不进行网络探测
            }
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
        if (!instances.isInitialized() || CollectionUtils.isEmpty(instances.getInstances())) {
            return;
        }

        Map<Instance, DetectResult> aliveResults = new HashMap<>();
        for (Instance instance : instances.getInstances()) {
            if (destroy.get()) {
                // 如果要停止定时任务，则剩下的实例探测也没必要进行下去了
                break;
            }
            DetectResult result = detectInstance(instance);
            if (result == null) {
                continue;
            }
            aliveResults.put(instance, result);

        }
        if (MapUtils.isNotEmpty(aliveResults)) {
            ServiceUpdateRequest updateRequest = buildInstanceUpdateResult(serviceKey, aliveResults);
            LOG.debug("update cache for outlier detect, value is {}", updateRequest);
            extensions.getLocalRegistry().updateInstances(updateRequest);
        }
    }

    private ServiceUpdateRequest buildInstanceUpdateResult(ServiceKey serviceKey,
                                                           Map<Instance, DetectResult> aliveResults) {
        List<InstanceProperty> instances = new ArrayList<>();
        int notChange = 0;
        for (Map.Entry<Instance, DetectResult> entry : aliveResults.entrySet()) {
            InstanceByProto instance = (InstanceByProto) entry.getKey();
            DetectResult detectResult = instance.getDetectResult();
            if (detectResult != null && detectResult.getRetStatus() == entry.getValue().getRetStatus()) {
                notChange++;
                continue;
            }
            Map<String, Object> properties = new HashMap<>();
            properties.put(PROPERTY_DETECT_RESULT, entry.getValue());
            InstanceProperty instanceProperty = new InstanceProperty(entry.getKey(), properties);
            instances.add(instanceProperty);
        }
        LOG.info("{} detect {} instances, update {}", serviceKey, aliveResults.size(),
                aliveResults.size() - notChange);
        return new ServiceUpdateRequest(serviceKey, instances);
    }

    private DetectResult detectInstance(Instance instance) throws PolarisException {
        DetectResult result = null;
        for (HealthChecker detector : extensions.getHealthCheckers()) {
            DetectResult pluginResult = detector.detectInstance(instance, null);
            if (pluginResult == null) {
                continue;
            }
            result = pluginResult;
            if (result.getRetStatus() == RetStatus.RetSuccess) {
                result.setDetectType(detector.getName());
                return result;
            }
        }
        if (result != null) {
            result.setDetectType("all");
        }
        return result; // 如果没有探测则返回null, 全部失败返回失败
    }

    public Set<ServiceKey> getServiceKeySet() {
        return serviceKeySet;
    }
}
