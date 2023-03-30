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

public class MethodResource extends AbstractResource {

    private final String method;

    public MethodResource(ServiceKey service, String methodName) {
        this(service, methodName, null);
    }

    public MethodResource(ServiceKey service, String methodName, ServiceKey callerService) {
        super(service, callerService);
        CommonValidator.validateNamespaceService(service.getNamespace(), service.getService());
        CommonValidator.validateText(methodName, "method");
        this.method = methodName;
    }

    @Override
    public Level getLevel() {
        return Level.METHOD;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MethodResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MethodResource that = (MethodResource) o;
        return Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), method);
    }

    @Override
    public String toString() {
        return "MethodResource{" +
                "method='" + method + '\'' +
                "} " + super.toString();
    }
}
