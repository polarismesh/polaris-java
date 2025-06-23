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

import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig;
import com.tencent.polaris.api.config.consumer.OutlierDetectionConfig.When;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.api.ServiceCallResultListener;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 调用结果记录。
 *
 * @author Haotian Zhang
 */
public class ServiceCallResultCollector implements ServiceCallResultListener {

    private ScheduledExecutorService detectTaskExecutors;

    private OutlierDetectionConfig outlierDetectionConfig;

    private InstancesDetectTask detectTask;

    private final AtomicInteger state = new AtomicInteger(0);

    private Set<ServiceKey> calledServiceSet = new HashSet<>();

    private InstancesStatisticUpdater instancesStatisticUpdater;
    @Override
    public synchronized void init(SDKContext sdkContext) {
        if (!state.compareAndSet(0, 1)) {
            return;
        }
        outlierDetectionConfig = sdkContext.getConfig().getConsumer().getOutlierDetection();
        LocalRegistry localRegistry = sdkContext.getExtensions().getLocalRegistry();
        instancesStatisticUpdater = new InstancesStatisticUpdater(localRegistry);
        if (outlierDetectionConfig.getWhen() != When.never) {
            detectTaskExecutors = Executors.newSingleThreadScheduledExecutor();
            long checkPeriodMs = outlierDetectionConfig.getCheckPeriod();
            detectTask = new InstancesDetectTask(sdkContext.getExtensions(), outlierDetectionConfig.getWhen());
            calledServiceSet = detectTask.getServiceKeySet();
            detectTaskExecutors.scheduleAtFixedRate(detectTask, checkPeriodMs, checkPeriodMs, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onServiceCallResult(InstanceGauge result) {
        if (outlierDetectionConfig.getWhen() == When.after_call) {
            calledServiceSet.add(new ServiceKey(result.getNamespace(), result.getService()));
        }
        instancesStatisticUpdater.updateInstanceStatistic(result);
    }

    @Override
    public synchronized void destroy() {
        if (!state.compareAndSet(1, 0)) {
            return;
        }
        if (null != detectTask) {
            detectTask.destroy();
        }
        if (null != detectTaskExecutors) {
            detectTaskExecutors.shutdown();
        }
    }
}
