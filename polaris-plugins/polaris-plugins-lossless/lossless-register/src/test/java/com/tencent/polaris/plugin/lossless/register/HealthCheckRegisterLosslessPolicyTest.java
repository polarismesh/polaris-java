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

package com.tencent.polaris.plugin.lossless.register;

import com.tencent.polaris.api.plugin.lossless.RegisterStatus;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.pojo.DefaultBaseInstance;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class HealthCheckRegisterLosslessPolicyTest {

    @Test
    public void testCheckRegisterStatus() {
        // empty instances
        RegisterStatus registerStatus = HealthCheckRegisterLosslessPolicy.checkRegisterStatus(
                Collections.emptySet(), Collections.emptyMap());
        Assert.assertEquals(RegisterStatus.UNREGISTERED, registerStatus);

        List<BaseInstance> instances = getInstances();

        // empty statuses
        registerStatus = HealthCheckRegisterLosslessPolicy.checkRegisterStatus(instances, Collections.emptyMap());
        Assert.assertEquals(RegisterStatus.UNREGISTERED, registerStatus);

        // not all registered, some empty
        Map<BaseInstance, RegisterStatus> registerStatusMap = new HashMap<>();
        registerStatusMap.put(instances.get(0), RegisterStatus.REGISTERED);
        registerStatus = HealthCheckRegisterLosslessPolicy.checkRegisterStatus(instances, registerStatusMap);
        Assert.assertEquals(RegisterStatus.UNREGISTERED, registerStatus);

        registerStatusMap.put(instances.get(1), RegisterStatus.REGISTERED);
        registerStatus = HealthCheckRegisterLosslessPolicy.checkRegisterStatus(instances, registerStatusMap);
        Assert.assertEquals(RegisterStatus.REGISTERED, registerStatus);

        registerStatusMap.put(instances.get(0), RegisterStatus.UNREGISTERED);
        registerStatus = HealthCheckRegisterLosslessPolicy.checkRegisterStatus(instances, registerStatusMap);
        Assert.assertEquals(RegisterStatus.UNREGISTERED, registerStatus);
    }

    private static List<BaseInstance> getInstances() {
        List<BaseInstance> instances = new ArrayList<>();
        DefaultBaseInstance instance1 = new DefaultBaseInstance();
        instance1.setNamespace("test");
        instance1.setService("TestSvc");
        instance1.setHost("127.0.0.1");
        instance1.setPort(1111);
        DefaultBaseInstance instance2 = new DefaultBaseInstance();
        instance2.setNamespace("test");
        instance2.setService("TestSvc");
        instance2.setHost("127.0.0.1");
        instance2.setPort(1112);
        instances.add(instance1);
        instances.add(instance2);
        return instances;
    }
}
