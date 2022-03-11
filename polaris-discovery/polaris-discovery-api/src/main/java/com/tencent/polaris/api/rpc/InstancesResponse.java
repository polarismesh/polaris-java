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

package com.tencent.polaris.api.rpc;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceInstancesWrap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Response for instances query request.
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class InstancesResponse extends BaseEntity {

    private final ServiceInstances serviceInstances;

    private final Map<String, String> metadata;

    private final int totalWeight;

    private final Instance[] instances;

    public InstancesResponse(ServiceInstances serviceInstances) {
        this.serviceInstances = serviceInstances;
        this.metadata = serviceInstances.getMetadata();
        this.setService(serviceInstances.getService());
        this.setNamespace(serviceInstances.getNamespace());
        Collection<Instance> svcInstances = serviceInstances.getInstances();
        this.instances = svcInstances.toArray(new Instance[svcInstances.size()]);
        this.totalWeight = serviceInstances.getTotalWeight();
    }

    public InstancesResponse(ServiceInstances serviceInstances, Instance singleInstance) {
        this.serviceInstances = serviceInstances;
        this.metadata = serviceInstances.getMetadata();
        this.setService(serviceInstances.getService());
        this.setNamespace(serviceInstances.getNamespace());
        this.instances = new Instance[]{singleInstance};
        this.totalWeight = serviceInstances.getTotalWeight();
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Instance[] getInstances() {
        return instances;
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public ServiceInstances toServiceInstances() {
        return new ServiceInstancesWrap(serviceInstances, Arrays.asList(getInstances()), totalWeight);
    }

    public boolean isServiceExist() {
        return this.serviceInstances.isInitialized();
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "InstancesResponse{" +
                "metadata=" + metadata +
                ", totalWeight=" + totalWeight +
                ", instances=" + Arrays.toString(instances) +
                "} " + super.toString();
    }
}
