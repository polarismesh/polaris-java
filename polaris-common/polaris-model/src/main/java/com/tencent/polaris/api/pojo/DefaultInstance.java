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

import com.tencent.polaris.api.utils.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DefaultInstance extends DefaultBaseInstance implements Instance {

    private String revision;

    private final Map<StatusDimension, CircuitBreakerStatus> circuitBreakerStatuses = new HashMap<>();

    private boolean healthy;

    private boolean isolated;

    private String protocol;

    private String id;

    private String version;

    private Map<String, String> metadata;

    private boolean enableHealthCheck;

    private String region;

    private String zone;

    private String campus;

    private int priority;

    private int weight;

    private String logicSet;

    private Long createTime;

    private Map<String, String> serviceMetadata = new HashMap<>();

    @Override
    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Map<StatusDimension, CircuitBreakerStatus> getCircuitBreakerStatuses() {
        return circuitBreakerStatuses;
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    @Override
    public boolean isIsolated() {
        return isolated;
    }

    public void setIsolated(boolean isolated) {
        this.isolated = isolated;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public boolean isEnableHealthCheck() {
        return enableHealthCheck;
    }

    public void setEnableHealthCheck(boolean enableHealthCheck) {
        this.enableHealthCheck = enableHealthCheck;
    }

    @Override
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    @Override
    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    @Override
    public String getLogicSet() {
        return logicSet;
    }

    public void setLogicSet(String logicSet) {
        this.logicSet = logicSet;
    }

    public Map<String, String> getServiceMetadata() {
        return serviceMetadata;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    @Override
    public Long getCreateTime() {
        return createTime;
    }

    public void setServiceMetadata(Map<String, String> serviceMetadata) {
        if (serviceMetadata != null) {
            this.serviceMetadata = serviceMetadata;
        } else {
            this.serviceMetadata = new HashMap<>();
        }
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus() {
        return circuitBreakerStatuses.get(StatusDimension.EMPTY_DIMENSION);
    }

    @Override
    public Collection<StatusDimension> getStatusDimensions() {
        return circuitBreakerStatuses.keySet();
    }

    @Override
    public CircuitBreakerStatus getCircuitBreakerStatus(StatusDimension statusDimension) {
        return circuitBreakerStatuses.get(statusDimension);
    }

    @Override
    public int compareTo(Instance instance) {
        String curHost = StringUtils.defaultString(this.getHost());
        String remoteHost = StringUtils.defaultString(instance.getHost());
        int result = curHost.compareTo(remoteHost);
        if (result != 0) {
            return result;
        }
        return this.getPort() - instance.getPort();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultInstance)) return false;
        if (!super.equals(o)) return false;
        DefaultInstance instance = (DefaultInstance) o;
        return Objects.equals(id, instance.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id);
    }

    @Override
    public String toString() {
        return "DefaultInstance{" +
                "registry='" + getRegistry() + '\'' +
                ", namespace='" + getNamespace() + '\'' +
                ", service='" + getService() + '\'' +
                ", revision='" + revision + '\'' +
                ", circuitBreakerStatuses=" + circuitBreakerStatuses +
                ", healthy=" + healthy +
                ", isolated=" + isolated +
                ", protocol='" + protocol + '\'' +
                ", id='" + id + '\'' +
                ", host='" + getHost() + '\'' +
                ", port=" + getPort() +
                ", version='" + version + '\'' +
                ", metadata=" + metadata +
                ", enableHealthCheck=" + enableHealthCheck +
                ", region='" + region + '\'' +
                ", zone='" + zone + '\'' +
                ", campus='" + campus + '\'' +
                ", priority=" + priority +
                ", weight=" + weight +
                ", logicSet='" + logicSet + '\'' +
                ", serviceMetadata=" + serviceMetadata +
                '}';
    }
}
