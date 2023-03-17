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

package com.tencent.polaris.plugins.circuitbraker.composite.trigger;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.ServiceResource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugins.circuitbreaker.composite.ResourceHealthChecker;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetectRule.Protocol;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto.FaultDetector;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

public class ResourceHealthCheckerTest {

    @Test
    public void testSelectFdRule() {
        FaultDetector.Builder cbBuilder = FaultDetector.newBuilder();
        // match one service rules
        FaultDetectRule.Builder builder = FaultDetectRule.newBuilder();
        builder.setName("test_cb_default_svc1");
        builder.setTargetService(
                FaultDetectRule.DestinationService.newBuilder().setNamespace("default").setService("svc1").build());
        builder.setProtocol(Protocol.HTTP);
        cbBuilder.addRules(builder);

        builder = FaultDetectRule.newBuilder();
        builder.setName("test_cb_all_ns_all_svc");
        builder.setTargetService(
                FaultDetectRule.DestinationService.newBuilder().setNamespace("*").setService("*").build());
        cbBuilder.addRules(builder);
        builder.setProtocol(Protocol.HTTP);

        cbBuilder.addRules(builder);

        cbBuilder.setRevision("xxxxxyyyyyy");

        FaultDetector allRules = cbBuilder.build();

        ServiceResource svcResource = new ServiceResource(new ServiceKey("default", "svc1"));
        Function<String, Pattern> regexToPattern = new Function<String, Pattern>() {
            @Override
            public Pattern apply(String s) {
                return Pattern.compile(s);
            }
        };
        Map<String, FaultDetectRule> protocolFaultDetectRuleMap = ResourceHealthChecker
                .selectFaultDetectRules(svcResource, allRules, regexToPattern);
        Assert.assertEquals(1, protocolFaultDetectRuleMap.size());
        FaultDetectRule faultDetectRule = protocolFaultDetectRuleMap.get(Protocol.HTTP.name());
        Assert.assertEquals("test_cb_default_svc1", faultDetectRule.getName());

    }
}
