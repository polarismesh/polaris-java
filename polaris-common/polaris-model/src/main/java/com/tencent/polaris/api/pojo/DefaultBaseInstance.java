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

package com.tencent.polaris.api.pojo;

import java.util.Objects;

public class DefaultBaseInstance implements BaseInstance {

    private String namespace;

    private String service;

    private String host;

    private int port;

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultBaseInstance that = (DefaultBaseInstance) o;
        return port == that.port && Objects.equals(namespace, that.namespace) && Objects.equals(service, that.service) && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, service, host, port);
    }

    @Override
    public String toString() {
        return "DefaultBaseInstance{" +
                "namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
