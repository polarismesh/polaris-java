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

package com.tencent.polaris.api.rpc;

import java.util.Map;
import java.util.Objects;

/**
 * 批量获取服务信息请求
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class GetServicesRequest extends RequestBaseEntity {

    private String namespace;

    private Map<String, String> metadata;

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "GetServicesRequest{" +
                "namespace='" + namespace + '\'' +
                ", metadata=" + metadata +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetServicesRequest)) {
            return false;
        }
        GetServicesRequest request = (GetServicesRequest) o;
        return Objects.equals(getNamespace(), request.getNamespace()) && Objects.equals(getMetadata(),
                request.getMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNamespace(), getMetadata());
    }

    public static GetServicesRequestBuilder builder() {
        return new GetServicesRequestBuilder();
    }

    public static final class GetServicesRequestBuilder {

        private String service;
        private String namespace;
        private long timeoutMs;
        private Map<String, String> metadata;

        private GetServicesRequestBuilder() {
        }

        public GetServicesRequestBuilder service(String service) {
            this.service = service;
            return this;
        }

        public GetServicesRequestBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public GetServicesRequestBuilder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public GetServicesRequestBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public GetServicesRequest build() {
            GetServicesRequest getServicesRequest = new GetServicesRequest();
            getServicesRequest.setService(service);
            getServicesRequest.setNamespace(namespace);
            getServicesRequest.setTimeoutMs(timeoutMs);
            getServicesRequest.setNamespace(namespace);
            getServicesRequest.setMetadata(metadata);
            return getServicesRequest;
        }
    }
}
