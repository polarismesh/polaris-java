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

package com.tencent.polaris.plugins.connector.nacos;


import static com.alibaba.nacos.api.common.Constants.DEFAULT_CLUSTER_NAME;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_GROUP;
import static com.alibaba.nacos.api.common.Constants.DEFAULT_NAMESPACE_ID;

import com.tencent.polaris.api.config.global.ServerConnectorConfig;

/**
 * Context of nacos server connector.
 *
 * @author Yuwei Fu
 */
public class NacosContext {

    private String groupName;

    private String clusterName;

    private String namespace;

    private boolean ephemeral;

    private String serviceName;

    private Long nacosErrorSleep;

    public NacosContext(){
        groupName = DEFAULT_GROUP;
        clusterName = DEFAULT_CLUSTER_NAME;
        namespace = DEFAULT_NAMESPACE_ID;
        nacosErrorSleep = 1000L;
        ephemeral = false;
    }


    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Long getNacosErrorSleep() {
        return nacosErrorSleep;
    }

    public void setNacosErrorSleep(Long nacosErrorSleep) {
        this.nacosErrorSleep = nacosErrorSleep;
    }

    @Override
    public String toString() {
        return "NacosContext{" +
                ", groupName='" + groupName + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", namespace='" + namespace + '\'' +
                ", ephemeral=" + ephemeral +
                ", serviceName='" + serviceName + '\'' +
                ", nacosErrorSleep=" + nacosErrorSleep +
                '}';
    }
}
