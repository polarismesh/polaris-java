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

import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.DetectResult;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.InstanceLocalValue;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.api.utils.TimeUtils;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 通过PB对象封装的实例信息
 *
 * @author andrewshan
 * @date 2019/8/22
 */
public class InstanceByProto implements Instance {

    private final ServiceKey serviceKey;

    private final ServiceProto.Instance instance;

    private final InstanceLocalValue instanceLocalValue;

    private final int hashCode;

    private final Long createTime;

    public InstanceByProto(ServiceKey serviceKey, ServiceProto.Instance instance,
            InstanceLocalValue localValue) {
        this.serviceKey = serviceKey;
        this.instance = instance;
        this.instanceLocalValue = localValue;
        hashCode = Objects.hash(instance.getHost(), instance.getPort());
        createTime = TimeUtils.getCreateTime(instance.getCtime().getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InstanceByProto that = (InstanceByProto) o;
        return Objects.equals(instance.getHost(), that.instance.getHost()) && Objects
                .equals(instance.getPort(), that.instance.getPort());
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String getNamespace() {
        return serviceKey.getNamespace();
    }

    @Override
    public String getService() {
        return serviceKey.getService();
    }

    @Override
    public String getRevision() {
        return instance.getRevision().getValue();
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus() {
        if (null == instanceLocalValue) {
            return null;
        }
        return instanceLocalValue.getCircuitBreakerStatus(StatusDimension.EMPTY_DIMENSION);
    }

    @Override
    public Collection<StatusDimension> getStatusDimensions() {
        if (null == instanceLocalValue) {
            return Collections.emptySet();
        }
        return instanceLocalValue.getStatusDimensions();
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus(StatusDimension statusDimension) {
        if (null == instanceLocalValue) {
            return null;
        }
        return instanceLocalValue.getCircuitBreakerStatus(statusDimension);
    }

    public DetectResult getDetectResult() {
        if (null == instanceLocalValue) {
            return null;
        }
        return instanceLocalValue.getDetectResult();
    }

    @Override
    public boolean isHealthy() {
        return instance.getHealthy().getValue();
    }

    @Override
    public boolean isIsolated() {
        return instance.getIsolate().getValue();
    }

    @Override
    public String getProtocol() {
        return instance.getProtocol().getValue();
    }

    @Override
    public String getId() {
        return instance.getId().getValue();
    }

    @Override
    public String getHost() {
        return instance.getHost().getValue();
    }

    @Override
    public int getPort() {
        return instance.getPort().getValue();
    }

    @Override
    public String getVersion() {
        return instance.getVersion().getValue();
    }

    @Override
    public Map<String, String> getMetadata() {
        return instance.getMetadataMap();
    }

    @Override
    public boolean isEnableHealthCheck() {
        return instance.hasHealthCheck();
    }

    @Override
    public String getRegion() {
        return instance.getLocation().getRegion().getValue();
    }

    @Override
    public String getZone() {
        return instance.getLocation().getZone().getValue();
    }

    @Override
    public String getCampus() {
        return instance.getLocation().getCampus().getValue();
    }

    @Override
    public int getPriority() {
        return instance.getPriority().getValue();
    }

    @Override
    public int getWeight() {
        int weight = DEFAULT_WEIGHT;
        if (instance.hasWeight()) {
            UInt32Value weightValue = instance.getWeight();
            weight = weightValue.getValue();
        }
        return weight;
    }

    @Override
    public String getLogicSet() {
        return instance.getLogicSet().getValue();
    }

    @Override
    public Long getCreateTime() {
        return createTime;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "InstanceByProto{" +
                "serviceKey=" + serviceKey +
                ", instance=" + instance +
                '}';
    }

    public InstanceLocalValue getInstanceLocalValue() {
        return instanceLocalValue;
    }

    @Override
    public int compareTo(Instance instance) {
        String curHost = this.getHost();
        String remoteHost = instance.getHost();
        int result = curHost.compareTo(remoteHost);
        if (result != 0) {
            return result;
        }
        return this.getPort() - instance.getPort();
    }
}
