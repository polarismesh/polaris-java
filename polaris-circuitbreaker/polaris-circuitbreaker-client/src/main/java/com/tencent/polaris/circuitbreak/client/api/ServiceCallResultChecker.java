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

package com.tencent.polaris.circuitbreak.client.api;

import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig.When;
import com.tencent.polaris.api.plugin.circuitbreaker.CircuitBreaker;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.circuitbreak.client.task.InstancesCircuitBreakTask;
import com.tencent.polaris.circuitbreak.client.task.InstancesDetectTask;
import com.tencent.polaris.circuitbreak.client.task.PriorityTaskScheduler;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.api.ServiceCallResultListener;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据上报时用于检查下是否已经熔断
 */
public class ServiceCallResultChecker implements ServiceCallResultListener {

    private PriorityTaskScheduler priorityTaskScheduler;

    private ScheduledExecutorService cbTaskExecutors;

    private ScheduledExecutorService detectTaskExecutors;

    private Extensions extensions;

    private InstancesDetectTask detectTask;

    private final AtomicInteger state = new AtomicInteger(0);

    @Override
    public synchronized void init(SDKContext sdkContext) {
        if (!state.compareAndSet(0, 1)) {
            return;
        }
        extensions = sdkContext.getExtensions();
        CircuitBreakerConfig cbConfig = sdkContext.getConfig().getConsumer().getCircuitBreaker();
        if (cbConfig.isEnable()) {
            priorityTaskScheduler = new PriorityTaskScheduler();
            cbTaskExecutors = Executors.newSingleThreadScheduledExecutor();
            CheckServicesCircuitBreak checker = new CheckServicesCircuitBreak(
                    sdkContext.getExtensions(), priorityTaskScheduler);
            long checkPeriodMs = cbConfig.getCheckPeriod();
            cbTaskExecutors.scheduleAtFixedRate(
                    checker, checkPeriodMs, checkPeriodMs / 2, TimeUnit.MILLISECONDS);
        }
        OutlierDetectionConfig outlierDetection = sdkContext.getConfig().getConsumer().getOutlierDetection();
        if (outlierDetection.getWhen() != When.never) {
            detectTaskExecutors = Executors.newSingleThreadScheduledExecutor();
            long checkPeriodMs = outlierDetection.getCheckPeriod();
            detectTask = new InstancesDetectTask(extensions);
            detectTaskExecutors.scheduleAtFixedRate(detectTask, checkPeriodMs, checkPeriodMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onServiceCallResult(InstanceGauge result) {
        if (null == priorityTaskScheduler) {
            return;
        }
        if (CollectionUtils.isEmpty(extensions.getCircuitBreakers())) {
            return;
        }
        InstancesCircuitBreakTask rtTask = null;
        for (CircuitBreaker circuitBreaker : extensions.getCircuitBreakers()) {
            String cbName = circuitBreaker.getName();
            boolean rtLimit = circuitBreaker.stat(result);
            String instId = result.getInstanceId();
            if (rtLimit && StringUtils.isNotEmpty(instId)) {
                ServiceKey svcKey = new ServiceKey(result.getNamespace(), result.getService());
                rtTask = new InstancesCircuitBreakTask(
                        svcKey, cbName, null, instId, extensions, InstancesCircuitBreakTask.TaskPriority.HIGH);
                break;
            }
        }
        if (null == rtTask) {
            return;
        }
        priorityTaskScheduler.addCircuitBreakTask(rtTask);
    }

    @Override
    public synchronized void destroy() {
        if (!state.compareAndSet(1, 0)) {
            return;
        }
        if (null != priorityTaskScheduler) {
            priorityTaskScheduler.destroy();
        }
        if (null != cbTaskExecutors) {
            cbTaskExecutors.shutdown();
        }
        if (null != detectTask) {
            detectTask.destroy();
        }
        if (null != detectTaskExecutors) {
            detectTaskExecutors.shutdown();
        }
    }

    private static class CheckServicesCircuitBreak implements Runnable {

        private final Extensions extensions;

        private final PriorityTaskScheduler priorityTaskScheduler;

        public CheckServicesCircuitBreak(Extensions extensions, PriorityTaskScheduler priorityTaskScheduler) {
            this.extensions = extensions;
            this.priorityTaskScheduler = priorityTaskScheduler;
        }

        @Override
        public void run() {
            Set<ServiceKey> services = extensions.getLocalRegistry().getServices();
            for (ServiceKey service : services) {
                ServiceEventKey svcEventKey = new ServiceEventKey(service, EventType.INSTANCE);
                ServiceInstances svcInstances = extensions.getLocalRegistry()
                        .getInstances(new ResourceFilter(svcEventKey, true, true));
                if (!svcInstances.isInitialized() || svcInstances.getInstances().isEmpty()) {
                    continue;
                }
                priorityTaskScheduler.addCircuitBreakTask(
                        new InstancesCircuitBreakTask(service, "", svcInstances.getInstances(), "",
                                extensions, InstancesCircuitBreakTask.TaskPriority.LOW));
            }
        }
    }
}
