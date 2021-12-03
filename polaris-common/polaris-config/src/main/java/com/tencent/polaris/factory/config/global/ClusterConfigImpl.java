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

package com.tencent.polaris.factory.config.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.global.ClusterConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;
import java.util.List;

public class ClusterConfigImpl implements ClusterConfig {

    @JsonProperty
    private String namespace;

    @JsonProperty
    private String service;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long refreshInterval;

    @JsonProperty
    private Boolean sameAsBuiltin;

    @JsonProperty
    private List<String> routers;

    @JsonProperty
    private String lbPolicy;

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public long getRefreshInterval() {
        if (null == refreshInterval) {
            return 0;
        }
        return refreshInterval;
    }

    public void setRefreshInterval(long refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    @Override
    public List<String> getRouters() {
        return routers;
    }

    public void setRouters(List<String> routers) {
        this.routers = routers;
    }

    @Override
    public String getLbPolicy() {
        return lbPolicy;
    }

    public void setLbPolicy(String lbPolicy) {
        this.lbPolicy = lbPolicy;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    public boolean isSameAsBuiltin() {
        if (null == sameAsBuiltin) {
            return false;
        }
        return sameAsBuiltin;
    }

    public void setSameAsBuiltin(boolean sameAsBuiltin) {
        this.sameAsBuiltin = sameAsBuiltin;
    }

    @Override
    public void verify() {
        ConfigUtils.validateString(lbPolicy, "lbPolicy");
        if (!sameAsBuiltin) {
            ConfigUtils.validateString(namespace, "namespace");
            ConfigUtils.validateString(service, "service");
            if (CollectionUtils.isEmpty(routers)) {
                throw new IllegalArgumentException("routers should be not empty");
            }
        }
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            ClusterConfig clusterConfig = (ClusterConfig) defaultObject;
            if (null == namespace) {
                setNamespace(clusterConfig.getNamespace());
            }
            if (null == service) {
                setService(clusterConfig.getService());
            }
            if (null == refreshInterval) {
                setRefreshInterval(clusterConfig.getRefreshInterval());
            }
            if (null == sameAsBuiltin) {
                setSameAsBuiltin(clusterConfig.isSameAsBuiltin());
            }
            if (CollectionUtils.isEmpty(routers)) {
                setRouters(clusterConfig.getRouters());
            }
            if (null == lbPolicy) {
                setLbPolicy(clusterConfig.getLbPolicy());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ClusterConfigImpl{" +
                "namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", refreshInterval=" + refreshInterval +
                ", sameAsBuiltin=" + sameAsBuiltin +
                ", routers=" + routers +
                ", lbPolicy='" + lbPolicy + '\'' +
                '}';
    }
}
