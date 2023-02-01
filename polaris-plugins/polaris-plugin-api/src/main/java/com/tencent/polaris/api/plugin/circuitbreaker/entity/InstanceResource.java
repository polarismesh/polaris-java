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

public class InstanceResource extends AbstractResource {

    private final String host;

    private final int port;

    public InstanceResource(ServiceKey service, String host, int port) {
        this(service, host, port, null);
    }

    public InstanceResource(ServiceKey service, String host, int port, ServiceKey callerService) {
        super(service, callerService);
        CommonValidator.validateService(service);
        CommonValidator.validateNamespaceService(service.getNamespace(), service.getService());
        CommonValidator.validateText(host, "host");
        this.host = host;
        this.port = port;
    }

    @Override
    public Level getLevel() {
        return Level.INSTANCE;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public ServiceKey getService() {
        return service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InstanceResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        InstanceResource that = (InstanceResource) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), host, port);
    }

    @Override
    public String toString() {
        return "InstanceResource{" +
                "host='" + host + '\'' +
                ", port=" + port +
                "} " + super.toString();
    }
}
