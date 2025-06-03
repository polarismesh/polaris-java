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

/**
 * 获取全量服务实例请求
 */
public class GetAllInstancesRequest extends InstanceRequest {

    /**
     * 服务元数据信息，用于服务路由过滤
     */
    private Map<String, String> metadata;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "GetAllInstancesRequest{" +
                "metadata=" + metadata +
                '}';
    }

    public static GetAllInstancesRequestBuilder builder() {
        return new GetAllInstancesRequestBuilder();
    }

    public static final class GetAllInstancesRequestBuilder {
        private String service;
        private String namespace;
        private long timeoutMs;
        private Map<String, String> metadata;

        private GetAllInstancesRequestBuilder() {
        }

        public GetAllInstancesRequestBuilder service(String service) {
            this.service = service;
            return this;
        }

        public GetAllInstancesRequestBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public GetAllInstancesRequestBuilder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public GetAllInstancesRequestBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public GetAllInstancesRequest build() {
            GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
            getAllInstancesRequest.setService(service);
            getAllInstancesRequest.setNamespace(namespace);
            getAllInstancesRequest.setTimeoutMs(timeoutMs);
            getAllInstancesRequest.setMetadata(metadata);
            return getAllInstancesRequest;
        }
    }
}
