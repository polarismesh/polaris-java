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

package com.tencent.polaris.assembly.api.pojo;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.MetadataProvider;
import com.tencent.polaris.api.rpc.RequestContext;

public class BeforeRequest extends AttachmentBaseEntity {

    private ServiceKey targetService;

    private RequestContext requestContext;

    private MetadataProvider metadataProvider;

    private Capability[] capabilities;

    public ServiceKey getTargetService() {
        return targetService;
    }

    public void setTargetService(ServiceKey targetService) {
        this.targetService = targetService;
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

    public void setMetadataProvider(MetadataProvider metadataProvider) {
        this.metadataProvider = metadataProvider;
    }

    public Capability[] getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Capability[] capabilities) {
        this.capabilities = capabilities;
    }
}
