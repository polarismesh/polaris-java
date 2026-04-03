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

package com.tencent.polaris.api.plugin.server;

import com.tencent.polaris.specification.api.v1.service.manage.ServiceContractProto;

/**
 * Feature of service, such as MCP tools, resources and prompts.
 *
 * @author Haotian Zhang
 */
public class ServiceFeature {

    private String id;

    private ServiceContractProto.ServiceFeatureType type;

    private String name;

    private String description;

    private String content;

    private ServiceContractProto.ServiceFeatureStatus status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ServiceContractProto.ServiceFeatureType getType() {
        return type;
    }

    public void setType(ServiceContractProto.ServiceFeatureType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ServiceContractProto.ServiceFeatureStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceContractProto.ServiceFeatureStatus status) {
        this.status = status;
    }
}
