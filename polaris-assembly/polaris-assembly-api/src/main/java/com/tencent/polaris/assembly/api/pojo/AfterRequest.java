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

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.MetadataProvider;
import com.tencent.polaris.api.rpc.RequestContext;
import com.tencent.polaris.api.rpc.ResponseContext;
import java.util.Map;

public class AfterRequest extends AttachmentBaseEntity {

    private ServiceKey targetService;

    private Instance targetInstance;

    private RequestContext requestContext;

    private ResponseContext responseContext;

    private MetadataProvider metadataProvider;

    private Map<String, String> routeLabels;

    private long delay;

    private Capability[] capabilities;

    public ServiceKey getTargetService() {
        return targetService;
    }

    public void setTargetService(ServiceKey targetService) {
        this.targetService = targetService;
    }

    public Instance getTargetInstance() {
        return targetInstance;
    }

    public void setTargetInstance(Instance targetInstance) {
        this.targetInstance = targetInstance;
    }

    public ResponseContext getResponseContext() {
        return responseContext;
    }

    public void setResponseContext(ResponseContext responseContext) {
        this.responseContext = responseContext;
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

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public Map<String, String> getRouteLabels() {
        return routeLabels;
    }

    public void setRouteLabels(Map<String, String> routeLabels) {
        this.routeLabels = routeLabels;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }
}
