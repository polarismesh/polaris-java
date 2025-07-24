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

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.metadata.core.manager.MetadataContext;

/**
 * 故障注入请求
 *
 * @author Haotian Zhang
 */
public class FaultRequest {

    private final ServiceKey sourceService;

    private final ServiceKey targetService;

    private final MetadataContext metadataContext;

    public FaultRequest(String sourceNamespace, String sourceService, String targetNamespace, String targetService, MetadataContext metadataContext) {
        this.sourceService = new ServiceKey(sourceNamespace, sourceService);
        this.targetService = new ServiceKey(targetNamespace, targetService);
        this.metadataContext = metadataContext;
    }

    public ServiceKey getSourceService() {
        return sourceService;
    }

    public ServiceKey getTargetService() {
        return targetService;
    }

    public MetadataContext getMetadataContext() {
        return metadataContext;
    }

    @Override
    public String toString() {
        return "FaultRequest{" +
                "sourceService=" + sourceService +
                ", targetService=" + targetService +
                ", metadataContext=" + metadataContext +
                '}';
    }
}
