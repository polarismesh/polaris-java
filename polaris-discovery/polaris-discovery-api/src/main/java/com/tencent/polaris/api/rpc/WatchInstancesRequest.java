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

package com.tencent.polaris.api.rpc;

import com.tencent.polaris.api.listener.ServiceListener;
import java.util.List;

public class WatchInstancesRequest extends BaseEntity {

    private List<ServiceListener> listeners;

    public List<ServiceListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<ServiceListener> listeners) {
        this.listeners = listeners;
    }

    public static WatchInstancesRequestBuilder builder() {
        return new WatchInstancesRequestBuilder();
    }

    public static final class WatchInstancesRequestBuilder {
        private String service;
        private String namespace;
        private String token;
        private List<ServiceListener> listeners;

        private WatchInstancesRequestBuilder() {
        }

        public WatchInstancesRequestBuilder service(String service) {
            this.service = service;
            return this;
        }

        public WatchInstancesRequestBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public WatchInstancesRequestBuilder token(String token) {
            this.token = token;
            return this;
        }

        public WatchInstancesRequestBuilder listeners(List<ServiceListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public WatchInstancesRequest build() {
            WatchInstancesRequest watchInstancesRequest = new WatchInstancesRequest();
            watchInstancesRequest.setService(service);
            watchInstancesRequest.setNamespace(namespace);
            watchInstancesRequest.setListeners(listeners);
            return watchInstancesRequest;
        }
    }
}
