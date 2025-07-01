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

import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.util.CommonValidator;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;

import java.util.Objects;

public class MethodResource extends AbstractResource {

    private final String protocol;

    private final String method;

    private final String path;

    @JustForTest
    public MethodResource(ServiceKey service, String path) {
        this(service, "*", "*", path, null);
    }

    public MethodResource(ServiceKey service, String protocol, String method, String path, ServiceKey callerService) {
        super(service, callerService);
        CommonValidator.validateNamespaceService(service.getNamespace(), service.getService());
        CommonValidator.validateText(path, "path");
        if (StringUtils.isBlank(protocol)) {
            protocol = "*";
        }
        if (StringUtils.isBlank(method)) {
            method = "*";
        }
        this.protocol = protocol;
        this.method = method;
        this.path = path;
    }

    @Override
    public Level getLevel() {
        return Level.METHOD;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        MethodResource that = (MethodResource) object;
        return Objects.equals(getProtocol(), that.getProtocol()) && Objects.equals(getMethod(), that.getMethod()) && Objects.equals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getProtocol(), getMethod(), getPath());
    }

    @Override
    public String toString() {
        return "MethodResource{" +
                "protocol='" + protocol + '\'' +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
