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


package com.tencent.polaris.plugins.circuitbreaker.common;

import com.tencent.polaris.api.pojo.Service;
import java.util.Objects;

public class RuleIdentifier {

    private final String namespace;

    private final String service;

    private final Service callerService;

    private final String method;

    public RuleIdentifier(String namespace, String service, Service callerService, String method) {
        this.namespace = namespace;
        this.service = service;
        this.callerService = callerService;
        this.method = method;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getService() {
        return service;
    }

    public Service getCallerService() {
        return callerService;
    }

    public String getMethod() {
        return method;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RuleIdentifier that = (RuleIdentifier) o;
        return Objects.equals(namespace, that.namespace) &&
                Objects.equals(service, that.service) &&
                Objects.equals(callerService, that.callerService) &&
                Objects.equals(method, that.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, service, callerService, method);
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "RuleIdentifier{" +
                "namespace=" + namespace +
                "service=" + service +
                ", callerService=" + callerService +
                ", method='" + method + '\'' +
                '}';
    }
}
