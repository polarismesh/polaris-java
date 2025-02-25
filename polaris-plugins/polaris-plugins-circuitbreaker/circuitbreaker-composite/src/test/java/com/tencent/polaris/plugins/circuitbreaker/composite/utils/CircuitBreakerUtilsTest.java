/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.polaris.api.config.consumer.CircuitBreakerConfig;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    public void testFillDefaultCircuitBreakerRuleInNeeded() {
        // 初始化测试依赖
        Resource resource = mock(Resource.class);
        ServiceKey callerService = mock(ServiceKey.class);
        ServiceKey targetService = mock(ServiceKey.class);

        when(resource.getCallerService()).thenReturn(callerService);
        when(resource.getService()).thenReturn(targetService);
        when(callerService.getNamespace()).thenReturn("testNamespace");
        when(callerService.getService()).thenReturn("testCallerService");
        when(targetService.getNamespace()).thenReturn("testNamespace");
        when(targetService.getService()).thenReturn("testTargetService");

        CircuitBreakerConfig circuitBreakerConfig = mock(CircuitBreakerConfig.class);

        when(circuitBreakerConfig.getSleepWindow()).thenReturn(30000L);
        when(circuitBreakerConfig.getSuccessCountAfterHalfOpen()).thenReturn(3);
        when(circuitBreakerConfig.isDefaultRuleEnable()).thenReturn(true);
        when(circuitBreakerConfig.getDefaultErrorCount()).thenReturn(10);
        when(circuitBreakerConfig.getDefaultErrorPercent()).thenReturn(50);
        when(circuitBreakerConfig.getDefaultInterval()).thenReturn(60000);
        when(circuitBreakerConfig.getDefaultMinimumRequest()).thenReturn(10);

        // 准备空规则
        CircuitBreakerProto.CircuitBreaker emptyCircuitBreaker = CircuitBreakerProto.CircuitBreaker.newBuilder().build();
        ServiceRuleByProto originalRule = new ServiceRuleByProto(emptyCircuitBreaker, "revision", false, null);

        // 执行方法
        ServiceRule result = CircuitBreakerUtils.fillDefaultCircuitBreakerRuleInNeeded(resource, originalRule, circuitBreakerConfig);

        // 验证结果
        assertThat(result).isInstanceOf(ServiceRuleByProto.class);
        CircuitBreakerProto.CircuitBreaker resultRule = (CircuitBreakerProto.CircuitBreaker) result.getRule();
        assertThat(resultRule.getRulesCount()).isEqualTo(1);

        CircuitBreakerProto.CircuitBreakerRule addedRule = resultRule.getRules(0);
        // 验证规则基本信息
        assertThat(addedRule.getName()).isEqualTo("default-polaris-instance-circuit-breaker");
        assertThat(addedRule.getLevel()).isEqualTo(CircuitBreakerProto.Level.INSTANCE);
        // 验证匹配规则
        assertThat(addedRule.getRuleMatcher().getSource().getNamespace()).isEqualTo("testNamespace");
        assertThat(addedRule.getRuleMatcher().getSource().getService()).isEqualTo("testCallerService");
        // 验证错误条件
        assertThat(addedRule.getBlockConfigs(0).getErrorConditions(0).getCondition().getValue().getValue())
                .isEqualTo("500,501,502,503,504,505,506,507,508,509,510,511,512,513,514,515,516,517,518,519,520,521,522,523,524,525,526,527,528,529,530,531,532,533,534,535,536,537,538,539,540,541,542,543,544,545,546,547,548,549,550,551,552,553,554,555,556,557,558,559,560,561,562,563,564,565,566,567,568,569,570,571,572,573,574,575,576,577,578,579,580,581,582,583,584,585,586,587,588,589,590,591,592,593,594,595,596,597,598,599");
        // 验证触发条件
        assertThat(addedRule.getBlockConfigs(0).getTriggerConditions(0).getTriggerType()).isEqualTo(CircuitBreakerProto.TriggerCondition.TriggerType.ERROR_RATE);
        assertThat(addedRule.getBlockConfigs(0).getTriggerConditions(0).getErrorPercent()).isEqualTo(50);
        assertThat(addedRule.getBlockConfigs(0).getTriggerConditions(0).getInterval()).isEqualTo(60);
        assertThat(addedRule.getBlockConfigs(0).getTriggerConditions(0).getMinimumRequest()).isEqualTo(10);
        assertThat(addedRule.getBlockConfigs(0).getTriggerConditions(1).getTriggerType()).isEqualTo(CircuitBreakerProto.TriggerCondition.TriggerType.CONSECUTIVE_ERROR);
        assertThat(addedRule.getBlockConfigs(0).getTriggerConditions(1).getErrorCount()).isEqualTo(10);
        // 验证恢复条件
        assertThat(addedRule.getRecoverCondition().getSleepWindow()).isEqualTo(30);
        assertThat(addedRule.getRecoverCondition().getConsecutiveSuccess()).isEqualTo(3);

        // 改为不开启默认规则
        when(circuitBreakerConfig.isDefaultRuleEnable()).thenReturn(false);
        // 执行方法
        ServiceRule emptyResult = CircuitBreakerUtils.fillDefaultCircuitBreakerRuleInNeeded(resource, originalRule, circuitBreakerConfig);
        // 验证结果
        assertThat(emptyResult).isInstanceOf(ServiceRuleByProto.class);
        CircuitBreakerProto.CircuitBreaker emptyResultRule = (CircuitBreakerProto.CircuitBreaker) emptyResult.getRule();
        assertThat(emptyResultRule.getRulesCount()).isEqualTo(0);
    }
}
