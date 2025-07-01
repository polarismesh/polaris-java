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

package com.tencent.polaris.test.mock.discovery;

import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceResourceProvider;
import com.tencent.polaris.api.pojo.ServiceRule;
import java.util.HashMap;
import java.util.Map;

public class MockServiceResourceProvider implements ServiceResourceProvider {

    private final Map<ServiceEventKey, ServiceRule> values = new HashMap<>();

    private final Map<ServiceKey, ServiceInstances> services = new HashMap<>();

    public void putServiceRule(ServiceEventKey svcEventKey, ServiceRule serviceRule) {
        values.put(svcEventKey, serviceRule);
    }

    public void putServiceInstances(ServiceKey svcKey, ServiceInstances serviceInstances) {
        services.put(svcKey, serviceInstances);
    }

    @Override
    public ServiceRule getServiceRule(ServiceEventKey svcEventKey) {
        return values.get(svcEventKey);
    }

    @Override
    public ServiceInstances getServiceInstances(ServiceKey serviceKey) {
        return services.get(serviceKey);
    }
}
