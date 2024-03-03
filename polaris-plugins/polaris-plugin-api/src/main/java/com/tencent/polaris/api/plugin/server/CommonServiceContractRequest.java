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

package com.tencent.polaris.api.plugin.server;

import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceContractProto;

public class CommonServiceContractRequest extends RequestBaseEntity {

    private String name;

    private String protocol;

    private String version;

    private TargetServer targetServer;

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(TargetServer targetServer) {
        this.targetServer = targetServer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ServiceContractProto.ServiceContract toQuerySpec() {
        ServiceContractProto.ServiceContract.Builder serviceContractBuilder =
                ServiceContractProto.ServiceContract.newBuilder();
        serviceContractBuilder.setName(StringUtils.defaultString(getName()));
        serviceContractBuilder.setService(StringUtils.defaultString(getService()));
        serviceContractBuilder.setNamespace(StringUtils.defaultString(getNamespace()));
        serviceContractBuilder.setProtocol(StringUtils.defaultString(getProtocol()));
        serviceContractBuilder.setVersion(StringUtils.defaultString(getVersion()));
        return serviceContractBuilder.build();
    }
}
