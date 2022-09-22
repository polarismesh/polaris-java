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

import java.util.Map;

/**
 * 服务信息
 *
 * @author andrewshan
 * @date 2019/9/23
 */
public class ServiceInfo implements ServiceMetadata, Comparable<ServiceInfo> {

    private String namespace;

    private String service;

    private Map<String, String> metadata;

    private String revision;

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ServiceInfo{" +
                "namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", metadata=" + metadata +
                '}';
    }

    public static ServiceInfoBuilder builder() {
        return new ServiceInfoBuilder();
    }

    @Override
    public int compareTo(ServiceInfo o) {
        String key1 = namespace + "##" + service;
        String key2 = o.namespace + "##" + o.service;
        return key1.compareTo(key2);
    }

    public static final class ServiceInfoBuilder {

        private String namespace;
        private String service;
        private Map<String, String> metadata;
        private String revision;

        private ServiceInfoBuilder() {
        }

        public ServiceInfoBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public ServiceInfoBuilder service(String service) {
            this.service = service;
            return this;
        }

        public ServiceInfoBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public ServiceInfoBuilder revision(String revision) {
            this.revision = revision;
            return this;
        }

        public ServiceInfo build() {
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setNamespace(namespace);
            serviceInfo.setService(service);
            serviceInfo.setMetadata(metadata);
            serviceInfo.setRevision(revision);
            return serviceInfo;
        }
    }
}