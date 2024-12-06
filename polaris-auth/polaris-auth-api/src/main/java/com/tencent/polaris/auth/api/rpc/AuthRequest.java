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

package com.tencent.polaris.auth.api.rpc;

import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.metadata.core.manager.MetadataContext;

/**
 * 鉴权请求
 *
 * @author Haotian Zhang
 */
public class AuthRequest extends RequestBaseEntity {

    private final String namespace;

    private final String service;

    private final String path;

    private final String protocol;

    private final String method;

    private final MetadataContext metadataContext;

    public AuthRequest(String namespace, String service, String path, String protocol, String method, MetadataContext metadataContext) {
        this.namespace = namespace;
        this.service = service;
        this.path = path;
        this.protocol = protocol;
        this.method = method;
        this.metadataContext = metadataContext;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getService() {
        return service;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getMethod() {
        return method;
    }

    public MetadataContext getMetadataContext() {
        return metadataContext;
    }

    @Override
    public String toString() {
        return "AuthRequest{" +
                "namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", path='" + path + '\'' +
                ", protocol='" + protocol + '\'' +
                ", method='" + method + '\'' +
                ", metadataContext=" + metadataContext +
                '}';
    }
}
