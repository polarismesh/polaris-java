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

package com.tencent.polaris.api.plugin.circuitbreaker.entity;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.util.CommonValidator;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;

import java.util.Objects;

public class InstanceResource extends AbstractResource {

    private final Node node;

    private final String protocol;

    public InstanceResource(ServiceKey service, String host, int port, ServiceKey callerService) {
        this(service, host, port, callerService, "");
    }

    public InstanceResource(ServiceKey service, String host, int port, ServiceKey callerService, String protocol) {
        super(service, callerService);
        CommonValidator.validateText(host, "host");
        this.node = new Node(IPAddressUtils.getIpCompatible(host), port);
        this.protocol = protocol;
    }

    @Override
    public Level getLevel() {
        return Level.INSTANCE;
    }

    public String getHost() {
        return node.getHost();
    }

    public int getPort() {
        return node.getPort();
    }

    public Node getNode() {
        return node;
    }

    public String getProtocol() {
        return protocol;
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
        return Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), node);
    }

    @Override
    public String toString() {
        return "InstanceResource{" +
                "node=" + node +
                ", protocol='" + protocol + '\'' +
                "} " + super.toString();
    }
}
