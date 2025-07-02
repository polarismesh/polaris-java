/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

import static com.tencent.polaris.api.exception.ErrorCode.INSTANCE_NOT_FOUND;
import static com.tencent.polaris.api.exception.ErrorCode.SERVICE_NOT_FOUND;
import static com.tencent.polaris.api.plugin.registry.InstanceProperty.PROPERTY_INSTANCE_STATISTIC;
import static com.tencent.polaris.api.pojo.RetStatus.RetSuccess;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.registry.LocalRegistry;
import com.tencent.polaris.api.plugin.registry.ResourceFilter;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.api.pojo.InstanceStatistic;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.InstanceByProto;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.List;
import org.slf4j.Logger;

public class InstancesStatisticUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(InstancesDetectTask.class);

    private final LocalRegistry localRegistry;

    private static final String POLARIS_NAMESPACE = "Polaris";

    public InstancesStatisticUpdater(LocalRegistry localRegistry) {
        this.localRegistry = localRegistry;
    }

    public void updateInstanceStatistic(InstanceGauge result) throws PolarisException {
        ServiceKey serviceKey = new ServiceKey(result.getNamespace(), result.getService());
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.INSTANCE);
        ServiceInstances serviceInstances = localRegistry.getInstances(new ResourceFilter(serviceEventKey, true, true));
        // 如果调用的是北极星内部的服务，则不统计
        if (serviceKey.getNamespace().equals(POLARIS_NAMESPACE)) {
            return;
        }
        if (serviceInstances == null) {
            LOG.warn("[InstanceStatisticUpdater]: " + "service: " + serviceKey.getService() + " in namespace: "
                    + serviceKey.getNamespace() + " not found");
            return;
        }
        if (CollectionUtils.isEmpty(serviceInstances.getInstances())) {
            LOG.warn("[InstanceStatisticUpdater]: " + "service: " + serviceKey.getService() + " in namespace: "
                    + serviceKey.getNamespace() + " has no instance");
            return;
        }

        List<Instance> instances = serviceInstances.getInstances();
        InstanceByProto targetInstance = null;
        for (Instance instance : instances) {
            if (instance.getHost().equals(result.getHost()) && instance.getPort() == result.getPort()) {
                targetInstance = (InstanceByProto) instance;
                break;
            }
        }
        if (targetInstance != null) {
            InstanceStatistic instanceStatistic = targetInstance.getInstanceLocalValue().getInstanceStatistic();
            instanceStatistic.count(result.getDelay(), RetSuccess.equals(result.getRetStatus()));
            LOG.debug(
                    "[InstanceStatisticUpdater]: " + targetInstance.getHost() + ":" + targetInstance.getPort() + ":"
                            + result.getPort() + ": Delay: " + result.getDelay() + "TotalCount"
                            + instanceStatistic.getTotalCount() + "TotalElapsed"
                            + instanceStatistic.getTotalElapsed());
        } else {
            LOG.warn("[InstanceStatisticUpdater]: " + result.getHost() + ":" + result.getPort() + ": not found");
        }
    }
}
