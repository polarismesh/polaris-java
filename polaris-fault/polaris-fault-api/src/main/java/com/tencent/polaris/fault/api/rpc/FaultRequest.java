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

package com.tencent.polaris.fault.api.rpc;

import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.metadata.core.manager.MetadataContext;

/**
 * 故障注入请求
 *
 * @author Haotian Zhang
 */
public class FaultRequest extends RequestBaseEntity {

    private final String namespace;

    private final String service;

    private final MetadataContext metadataContext;

    public FaultRequest(String namespace, String service, MetadataContext metadataContext) {
        this.namespace = namespace;
        this.service = service;
        this.metadataContext = metadataContext;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getService() {
        return service;
    }

    public MetadataContext getMetadataContext() {
        return metadataContext;
    }

    @Override
    public String toString() {
        return "FaultRequest{" +
                "namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", metadataContext=" + metadataContext +
                '}';
    }
}
