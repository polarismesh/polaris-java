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

package com.tencent.polaris.api.plugin.auth;

import com.tencent.polaris.metadata.core.manager.MetadataContext;

/**
 * 鉴权信息
 *
 * @author Haotian Zhang
 */
public class AuthInfo {

    private String namespace;

    private String service;

    private String path;

    private String protocol;

    private String method;

    private MetadataContext metadataContext;

    public AuthInfo(String namespace, String service, String path, String protocol, String method, MetadataContext metadataContext) {
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

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public MetadataContext getMetadataContext() {
        return metadataContext;
    }

    public void setMetadataContext(MetadataContext metadataContext) {
        this.metadataContext = metadataContext;
    }
}
