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

public class ServiceResource extends AbstractResource {

    protected final ServiceKey service;

    public ServiceResource(ServiceKey service) {
        this(service, null);
    }

    public ServiceResource(ServiceKey service, ServiceKey callerService) {
        super(callerService);
        CommonValidator.validateService(service);
        CommonValidator.validateNamespaceService(service.getNamespace(), service.getService());
        this.service = service;
    }

    @Override
    public Level getLevel() {
        return Level.SERVICE;
    }

    public ServiceKey getService() {
        return service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ServiceResource that = (ServiceResource) o;
        return Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), service);
    }

    @Override
    public String toString() {
        return "ServiceResource{" +
                "service=" + service +
                "} " + super.toString();
    }
}
