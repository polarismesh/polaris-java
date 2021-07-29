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

import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceRule;
import java.util.Map;

/**
 * 获取资源的应答
 */
public class GetResourcesResponse {

    private final Map<ServiceEventKey, ServiceInstances> services;

    private final Map<ServiceEventKey, ServiceRule> rules;

    public GetResourcesResponse(
            Map<ServiceEventKey, ServiceInstances> services, Map<ServiceEventKey, ServiceRule> rules) {
        this.services = services;
        this.rules = rules;
    }

    public Map<ServiceEventKey, ServiceInstances> getServices() {
        return services;
    }

    public Map<ServiceEventKey, ServiceRule> getRules() {
        return rules;
    }
}
