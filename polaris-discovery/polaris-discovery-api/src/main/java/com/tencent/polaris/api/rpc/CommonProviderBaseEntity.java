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

import com.tencent.polaris.api.plugin.server.CommonProviderRequest;

public class CommonProviderBaseEntity extends RequestBaseEntity {

    protected final CommonProviderRequest request = new CommonProviderRequest();

    @Override
    public String getService() {
        return request.getService();
    }

    @Override
    public void setService(String service) {
        request.setService(service);
    }

    @Override
    public String getNamespace() {
        return request.getNamespace();
    }

    @Override
    public void setNamespace(String namespace) {
        request.setNamespace(namespace);
    }

    public String getToken() {
        return request.getToken();
    }

    public void setToken(String token) {
        request.setToken(token);
    }

    public String getHost() {
        return request.getHost();
    }

    public void setHost(String host) {
        request.setHost(host);
    }

    public Integer getPort() {
        return request.getPort();
    }

    public void setPort(Integer port) {
        request.setPort(port);
    }
}
