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

package com.tencent.polaris.api.plugin.circuitbreaker.entity;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.util.CommonValidator;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import java.util.Objects;

public class ServiceResource implements Resource {

    private final ServiceKey service;

    public ServiceResource(String namespace, String serviceName) {
        CommonValidator.validateNamespaceService(namespace, serviceName);
        this.service = new ServiceKey(namespace, serviceName);
    }

    public ServiceResource(ServiceKey service) {
        CommonValidator.validateNamespaceService(service.getNamespace(), service.getService());
        this.service = service;
    }

    @Override
    public Level getLevel() {
        return Level.SERVICE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceResource)) {
            return false;
        }
        ServiceResource that = (ServiceResource) o;
        return Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service);
    }

    @Override
    public String toString() {
        return "ServiceResource{" +
                "service=" + service +
                '}';
    }

    public ServiceKey getService() {
        return service;
    }
}
