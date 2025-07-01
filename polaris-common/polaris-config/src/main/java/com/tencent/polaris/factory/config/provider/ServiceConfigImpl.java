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

package com.tencent.polaris.factory.config.provider;

import com.tencent.polaris.api.config.provider.ServiceConfig;

public class ServiceConfigImpl implements ServiceConfig {

    private String namespace;

    private String name;

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void verify() {

    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            ServiceConfig serviceConfig = (ServiceConfig) defaultObject;
            if (null == namespace) {
                setNamespace(serviceConfig.getNamespace());
            }
            if (null == name) {
                setName(serviceConfig.getName());
            }
        }
    }

    @Override
    public String toString() {
        return "ServiceConfigImpl{" +
                "namespace='" + namespace + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
