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

package com.tencent.polaris.api.rpc;

/**
 * Contain basic properties for an request/response.
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class BaseEntity {

    private String service;

    private String namespace;

    private String token;

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "BaseEntity{" +
                ", service='" + service + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
