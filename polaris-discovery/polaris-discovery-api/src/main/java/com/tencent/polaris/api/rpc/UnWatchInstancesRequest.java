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

import com.tencent.polaris.api.listener.ServiceListener;

import java.util.List;

public class UnWatchInstancesRequest extends BaseEntity {

    private List<ServiceListener> listeners;

    private boolean removeAll;

    public List<ServiceListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<ServiceListener> listeners) {
        this.listeners = listeners;
    }

    public boolean isRemoveAll() {
        return removeAll;
    }

    public void setRemoveAll(boolean removeAll) {
        this.removeAll = removeAll;
    }

    public static UnWatchInstancesRequestBuilder builder() {
        return new UnWatchInstancesRequestBuilder();
    }

    public static final class UnWatchInstancesRequestBuilder {
        private String service;
        private String namespace;
        private String token;
        private boolean removeAll;
        private List<ServiceListener> listeners;

        private UnWatchInstancesRequestBuilder() {
        }

        public UnWatchInstancesRequestBuilder service(String service) {
            this.service = service;
            return this;
        }

        public UnWatchInstancesRequestBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public UnWatchInstancesRequestBuilder token(String token) {
            this.token = token;
            return this;
        }

        public UnWatchInstancesRequestBuilder removeAll(boolean removeAll) {
            this.removeAll = removeAll;
            return this;
        }

        public UnWatchInstancesRequestBuilder listeners(List<ServiceListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public UnWatchInstancesRequest build() {
            UnWatchInstancesRequest unWatchInstancesRequest = new UnWatchInstancesRequest();
            unWatchInstancesRequest.setService(service);
            unWatchInstancesRequest.setNamespace(namespace);
            unWatchInstancesRequest.setRemoveAll(removeAll);
            unWatchInstancesRequest.setListeners(listeners);
            return unWatchInstancesRequest;
        }
    }
}
