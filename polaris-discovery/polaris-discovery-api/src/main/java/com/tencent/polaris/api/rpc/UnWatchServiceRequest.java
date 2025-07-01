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

import java.util.Collections;
import java.util.List;

/**
 * 移除服务监听的请求， Listeners 参数与 removeAll 互斥
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class UnWatchServiceRequest extends BaseEntity {

    /**
     * 需要移除的服务监听 Listener
     */
    private List<ServiceListener> listeners = Collections.emptyList();

    /**
     * 是否移除所有监听该服务的 Listener
     */
    private boolean removeAll = false;

    public List<ServiceListener> getListeners() {
        return listeners;
    }

    public boolean isRemoveAll() {
        return removeAll;
    }

    public static final class UnWatchServiceRequestBuilder {
        private String service;
        private String namespace;
        private List<ServiceListener> listeners;
        private boolean removeAll;

        private UnWatchServiceRequestBuilder() {
        }

        public static UnWatchServiceRequestBuilder anUnWatchServiceRequest() {
            return new UnWatchServiceRequestBuilder();
        }

        public UnWatchServiceRequestBuilder service(String service) {
            this.service = service;
            return this;
        }

        public UnWatchServiceRequestBuilder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public UnWatchServiceRequestBuilder listeners(List<ServiceListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        public UnWatchServiceRequestBuilder removeAll(boolean removeAll) {
            this.removeAll = removeAll;
            return this;
        }

        public UnWatchServiceRequest build() {
            UnWatchServiceRequest unWatchServiceRequest = new UnWatchServiceRequest();
            unWatchServiceRequest.setService(service);
            unWatchServiceRequest.setNamespace(namespace);
            unWatchServiceRequest.listeners = this.listeners;
            unWatchServiceRequest.removeAll = this.removeAll;
            return unWatchServiceRequest;
        }
    }
}
