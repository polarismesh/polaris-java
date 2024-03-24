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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.Node;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 服务实例集合的包装类，用于实例动态变化的场景
 *
 * @author andrewshan
 * @date 2019/8/25
 */
public class ServiceInstancesWrap implements ServiceInstances {

    private final String uuid = UUID.randomUUID().toString();

    private final ServiceInstances serviceInstances;

    private List<Instance> instances;

    private int totalWeight;

    private int hashCode;

    public ServiceInstancesWrap(ServiceInstances serviceInstances, List<Instance> instances, int totalWeight) {
        this.totalWeight = totalWeight;
        this.serviceInstances = serviceInstances;
        if (null == instances) {
            this.instances = serviceInstances.getInstances();
        } else {
            this.instances = instances;
        }
        hashCode = Objects.hash(serviceInstances.getServiceKey(), instances);
    }

    @Override
    public ServiceKey getServiceKey() {
        return serviceInstances.getServiceKey();
    }

    @Override
    public int getTotalWeight() {
        return totalWeight;
    }

    public void reloadTotalWeight() {
        this.totalWeight = 0;
        if (CollectionUtils.isEmpty(instances)) {
            return;
        }
        for (Instance instance : instances) {
            this.totalWeight += instance.getWeight();
        }
    }

    @Override
    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
        hashCode = Objects.hash(serviceInstances.getServiceKey(), this.instances);
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public String getRevision() {
        return serviceInstances.getRevision();
    }

    @Override
    public Instance getInstance(Node node) {
        throw new UnsupportedOperationException("getInstance not supported in ServiceInstancesWrap");
    }

    @Override
    public Instance getInstance(String id) {
        throw new UnsupportedOperationException("getInstance not supported in ServiceInstancesWrap");
    }

    @Override
    public String getService() {
        return serviceInstances.getService();
    }

    @Override
    public String getNamespace() {
        return serviceInstances.getNamespace();
    }

    @Override
    public Map<String, String> getMetadata() {
        return serviceInstances.getMetadata();
    }

    public List<Instance> getAllInstances() {
        return serviceInstances.getInstances();
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ServiceInstances that = (ServiceInstances) o;
        return Objects.equals(serviceInstances.getServiceKey(), that.getServiceKey()) &&
                Objects.equals(instances, that.getInstances());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ServiceInstancesWrap{" +
                "service=" + serviceInstances.getServiceKey() +
                ", totalWeight=" + totalWeight +
                ", instances=" + instances +
                '}';
    }

    public String getObjectId() {
        return uuid;
    }
}
