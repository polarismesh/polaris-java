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

package com.tencent.polaris.api.plugin.circuitbreaker.entity;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.util.CommonValidator;
import java.util.Objects;

public abstract class AbstractResource implements Resource {

    protected final ServiceKey service;

    protected final ServiceKey callerService;

    public AbstractResource(ServiceKey service, ServiceKey callerService) {
        CommonValidator.validateService(service);
        CommonValidator.validateNamespaceService(service.getNamespace(), service.getService());
        this.service = service;
        this.callerService = callerService;
    }

    @Override
    public ServiceKey getService() {
        return service;
    }

    @Override
    public ServiceKey getCallerService() {
        return callerService;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractResource)) {
            return false;
        }
        AbstractResource that = (AbstractResource) o;
        return Objects.equals(service, that.service) &&
                Objects.equals(callerService, that.callerService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service, callerService);
    }

    @Override
    public String toString() {
        return "AbstractResource{" +
                "service=" + service +
                ", callerService=" + callerService +
                '}';
    }
}
