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

package com.tencent.polaris.client.pojo;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceLocalValue;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.specification.api.v1.service.manage.ResponseProto;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通过PB对象封装的服务信息
 *
 * @author andrewshan
 * @date 2019/8/22
 */
public class ServiceInstancesByProto implements ServiceInstances, RegistryCacheValue {

    public static final ServiceInstancesByProto EMPTY_INSTANCES = new ServiceInstancesByProto();

    private final ServiceProto.Service service;

    private final ServiceKey svcKey;

    private final List<Instance> instances;

    private final List<ServiceProto.Instance> originInstancesList;

    private final Map<String, InstanceByProto> instanceIdMap;

    private final Map<Node, InstanceByProto> nodeMap;

    private final Map<String, String> metadata;

    private final boolean initialized;

    private final int totalWeight;

    private final boolean loadedFromFile;

    private final int hashCode;

    /**
     * 构造函数
     *
     * @param response 应答Proto
     * @param oldSvcInstances 旧的实例列表
     * @param loadFromFile 是否从缓存文件加载
     */
    public ServiceInstancesByProto(ResponseProto.DiscoverResponse response, ServiceInstancesByProto oldSvcInstances,
            boolean loadFromFile) {
        this.service = response.getService();
        List<Instance> tmpInstances = new ArrayList<>();
        Map<String, InstanceByProto> tmpInstanceMap = new HashMap<>();
        Map<Node, InstanceByProto> tmpNodeMap = new HashMap<>();
        List<ServiceProto.Instance> tmpOriginInstances = new ArrayList<>();
        int totalWeight = 0;
        ServiceKey svcKey = new ServiceKey(this.service.getNamespace().getValue(), this.service.getName().getValue());
        // TODO 需要判断不同来源的实例列表覆盖情况
        if (CollectionUtils.isNotEmpty(response.getInstancesList())) {
            tmpOriginInstances.addAll(response.getInstancesList());
            for (ServiceProto.Instance instance : response.getInstancesList()) {
                String instID = instance.getId().getValue();
                InstanceLocalValue instanceLocalValue = null;
                if (null != oldSvcInstances) {
                    InstanceByProto oldInstance = oldSvcInstances.getInstance(instID);
                    if (null != oldInstance) {
                        instanceLocalValue = oldInstance.getInstanceLocalValue();
                    }
                }
                if (null == instanceLocalValue) {
                    //创建一个新的本地缓存实例
                    instanceLocalValue = new DefaultInstanceLocalValue();
                }
                InstanceByProto targetInstance = new InstanceByProto(svcKey, instance, instanceLocalValue);
                totalWeight += targetInstance.getWeight();
                tmpInstances.add(targetInstance);
                tmpInstanceMap.put(instID, targetInstance);
                tmpNodeMap.put(new Node(targetInstance.getHost(), targetInstance.getPort()), targetInstance);
            }
        }
        Collections.sort(tmpInstances);
        hashCode = Objects.hash(svcKey, tmpInstances);
        this.svcKey = svcKey;
        this.instanceIdMap = Collections.unmodifiableMap(tmpInstanceMap);
        this.nodeMap = Collections.unmodifiableMap(tmpNodeMap);
        this.instances = Collections.unmodifiableList(tmpInstances);
        this.originInstancesList = Collections.unmodifiableList(tmpOriginInstances);
        this.totalWeight = totalWeight;
        this.initialized = true;
        this.metadata = Collections.unmodifiableMap(this.service.getMetadataMap());
        this.loadedFromFile = loadFromFile;
    }

    /**
     * 创建空的服务对象
     */
    public ServiceInstancesByProto() {
        this.service = null;
        this.svcKey = null;
        this.initialized = false;
        this.instances = Collections.emptyList();
        this.originInstancesList = Collections.emptyList();
        this.instanceIdMap = Collections.emptyMap();
        this.nodeMap = Collections.emptyMap();
        this.metadata = Collections.emptyMap();
        this.loadedFromFile = false;
        this.totalWeight = 0;
        hashCode = Objects.hash(instances);
    }

    @Override
    public ServiceKey getServiceKey() {
        return svcKey;
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
    public boolean isLoadedFromFile() {
        return loadedFromFile;
    }

    @Override
    public EventType getEventType() {
        return EventType.INSTANCE;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public String getRevision() {
        if (null != service) {
            return service.getRevision().getValue();
        }
        return "";
    }

    @Override
    public String getService() {
        if (null != service) {
            return service.getName().getValue();
        }
        return "";
    }

    @Override
    public String getNamespace() {
        if (null != service) {
            return service.getNamespace().getValue();
        }
        return "";
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * 获取实例本地数据
     *
     * @param instId 实例ID
     * @return InstanceLocalValue
     */
    public InstanceByProto getInstance(String instId) {
        return instanceIdMap.get(instId);
    }

    public InstanceByProto getInstanceByNode(Node node) {
        return nodeMap.get(node);
    }

    public List<ServiceProto.Instance> getOriginInstancesList() {
        return originInstancesList;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ServiceInstancesByProto{" +
                "service=" + service +
                ", instances=" + instances +
                ", metadata=" + metadata +
                ", revision='" + getRevision() + '\'' +
                ", initialized=" + initialized +
                ", totalWeight=" + totalWeight +
                '}';
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
        return Objects.equals(svcKey, that.getServiceKey()) &&
                Objects.equals(instances, that.getInstances());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
