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

package com.tencent.polaris.test.mock.discovery;

import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.api.pojo.ServiceRuleProvider;
import java.util.HashMap;
import java.util.Map;

public class MockServiceRuleProvider implements ServiceRuleProvider {

    private final Map<ServiceEventKey, ServiceRule> values = new HashMap<>();

    public void putServiceRule(ServiceEventKey svcEventKey, ServiceRule serviceRule) {
        values.put(svcEventKey, serviceRule);
    }

    @Override
    public ServiceRule getServiceRule(ServiceEventKey svcEventKey) {
        return values.get(svcEventKey);
    }
}
