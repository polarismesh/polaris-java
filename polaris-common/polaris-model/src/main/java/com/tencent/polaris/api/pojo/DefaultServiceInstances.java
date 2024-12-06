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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.pojo.Node;

import java.util.*;

public class DefaultServiceInstances implements ServiceInstances {

    private final ServiceKey serviceKey;

    private final List<Instance> instances;

    private final Map<String, Instance> idMap;

    private final Map<Node, Instance> nodeMap;

    private final Map<String, String> metadata;

    private final int totalWeight;

    private final int hashCode;

    private final String revision;

    public DefaultServiceInstances(ServiceKey serviceKey, List<Instance> instances) {
        this(serviceKey, instances, null);
    }

    public DefaultServiceInstances(ServiceKey serviceKey, List<Instance> instances, Map<String, String> metadata) {
        this.serviceKey = serviceKey;
        this.instances = Collections.unmodifiableList(instances);
        this.totalWeight = getTotalWeight(instances);
        hashCode = Objects.hash(serviceKey, instances);
        revision = Integer.toHexString(hashCode);
        idMap = new HashMap<>();
        nodeMap = new HashMap<>();
        for (Instance instance : instances) {
            idMap.put(instance.getId(), instance);
            nodeMap.put(new Node(instance.getHost(), instance.getPort()), instance);
        }
        this.metadata = new HashMap<>();
        if (CollectionUtils.isNotEmpty(metadata)) {
            this.metadata.putAll(metadata);
        }
    }

    private int getTotalWeight(List<Instance> instances) {
        int totalWeight = 0;
        if (CollectionUtils.isNotEmpty(instances)) {
            for (Instance instance : instances) {
                totalWeight += instance.getWeight();
            }
        }
        return totalWeight;
    }

    @Override
    public ServiceKey getServiceKey() {
        return serviceKey;
    }

    @Override
    public int getTotalWeight() {
        return totalWeight;
    }

    @Override
    public List<Instance> getInstances() {
        return instances;
    }

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    public Instance getInstance(Node node) {
        return nodeMap.get(node);
    }

    @Override
    public Instance getInstance(String id) {
        return idMap.get(id);
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String getService() {
        return this.serviceKey.getService();
    }

    @Override
    public String getNamespace() {
        return this.serviceKey.getNamespace();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultServiceInstances that = (DefaultServiceInstances) o;
        return Objects.equals(serviceKey, that.serviceKey) &&
                Objects.equals(instances, that.instances);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
