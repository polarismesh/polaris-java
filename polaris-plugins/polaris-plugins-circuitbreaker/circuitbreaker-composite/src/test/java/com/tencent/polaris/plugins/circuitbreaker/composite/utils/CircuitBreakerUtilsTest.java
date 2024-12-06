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

package com.tencent.polaris.plugins.circuitbreaker.composite.utils;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

/**
 * Test for {@link CircuitBreakerUtils}.
 *
 * @author Haotian Zhang
 */
public class CircuitBreakerUtilsTest {

    @Test
    public void testParseStatus() {
        CircuitBreakerProto.CircuitBreakerRule.Builder builder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_rule");
        builder.setEnable(true);
        builder.setLevel(CircuitBreakerProto.Level.METHOD);
        CircuitBreakerProto.BlockConfig.Builder blockConfigBuilder = CircuitBreakerProto.BlockConfig.newBuilder();
        blockConfigBuilder.addTriggerConditions(
                CircuitBreakerProto.TriggerCondition.newBuilder().setTriggerType(CircuitBreakerProto.TriggerCondition.TriggerType.CONSECUTIVE_ERROR).setErrorCount(5).build());
        blockConfigBuilder.addErrorConditions(CircuitBreakerProto.ErrorCondition.newBuilder().setInputType(CircuitBreakerProto.ErrorCondition.InputType.RET_CODE).setCondition(
                ModelProto.MatchString.newBuilder().setType(ModelProto.MatchString.MatchStringType.EXACT)
                        .setValue(StringValue.newBuilder().setValue("500").build()).build()).build());
        blockConfigBuilder.addErrorConditions(CircuitBreakerProto.ErrorCondition.newBuilder().setInputType(CircuitBreakerProto.ErrorCondition.InputType.DELAY).setCondition(
                ModelProto.MatchString.newBuilder().setType(ModelProto.MatchString.MatchStringType.EXACT)
                        .setValue(StringValue.newBuilder().setValue("600").build()).build()).build());
        builder.addBlockConfigs(blockConfigBuilder.build());
        builder.setRecoverCondition(CircuitBreakerProto.RecoverCondition.newBuilder().setConsecutiveSuccess(2).setSleepWindow(5).build());
        Resource resource = new InstanceResource(
                new ServiceKey("test", "TestSvc"), "127.0.0.1", 8088, null, "http");

        CircuitBreakerProto.CircuitBreakerRule rule = builder.build();
        ResourceStat stat1 = new ResourceStat(resource, 500, 100, RetStatus.RetUnknown);
        RetStatus nextStatus = CircuitBreakerUtils.parseRetStatus(stat1, rule.getBlockConfigsList().get(0).getErrorConditionsList(), Pattern::compile);
        Assert.assertEquals(RetStatus.RetFail, nextStatus);
        ResourceStat stat2 = new ResourceStat(resource, 502, 100, RetStatus.RetFail);
        nextStatus = CircuitBreakerUtils.parseRetStatus(stat2, rule.getBlockConfigsList().get(0).getErrorConditionsList(), Pattern::compile);
        Assert.assertEquals(RetStatus.RetSuccess, nextStatus);
        ResourceStat stat3 = new ResourceStat(resource, 200, 1000, RetStatus.RetSuccess);
        nextStatus = CircuitBreakerUtils.parseRetStatus(stat3, rule.getBlockConfigsList().get(0).getErrorConditionsList(), Pattern::compile);
        Assert.assertEquals(RetStatus.RetTimeout, nextStatus);

        builder = CircuitBreakerProto.CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_rule");
        builder.setEnable(true);
        builder.setLevel(CircuitBreakerProto.Level.METHOD);
        blockConfigBuilder = CircuitBreakerProto.BlockConfig.newBuilder();
        blockConfigBuilder.addTriggerConditions(
                CircuitBreakerProto.TriggerCondition.newBuilder().setTriggerType(CircuitBreakerProto.TriggerCondition.TriggerType.CONSECUTIVE_ERROR).setErrorCount(5).build());
        builder.addBlockConfigs(blockConfigBuilder.build());
        builder.setRecoverCondition(CircuitBreakerProto.RecoverCondition.newBuilder().setConsecutiveSuccess(2).setSleepWindow(5).build());
        rule = builder.build();
        nextStatus = CircuitBreakerUtils.parseRetStatus(stat3, rule.getBlockConfigsList().get(0).getErrorConditionsList(), Pattern::compile);
        Assert.assertEquals(RetStatus.RetSuccess, nextStatus);
    }
}
