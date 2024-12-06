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

package com.tencent.polaris.api.rpc;

import com.tencent.polaris.api.plugin.server.CommonServiceContractRequest;

public class GetServiceContractRequest extends RequestBaseEntity {

    private CommonServiceContractRequest request = new CommonServiceContractRequest();

    public String getName() {
        return request.getName();
    }

    public void setName(String name) {
       this.request.setName(name);
    }

    public String getProtocol() {
       return request.getProtocol();
    }

    public void setProtocol(String protocol) {
        this.request.setProtocol(protocol);
    }

    public String getVersion() {
        return request.getVersion();
    }

    public void setVersion(String version) {
        this.request.setVersion(version);
    }

    public CommonServiceContractRequest getRequest() {
        return request;
    }
}
