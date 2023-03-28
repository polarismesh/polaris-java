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

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.ServiceResource;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.plugins.circuitbreaker.composite.CircuitBreakerRuleContainer;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.CircuitBreaker;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.CircuitBreakerRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher.DestinationService;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher.SourceService;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;

public class CircuitBreakerRuleContainerTest {

    @Test
    public void testSelectRuleService() {
        CircuitBreaker.Builder cbBuilder = CircuitBreaker.newBuilder();
        // match one service rules
        CircuitBreakerRule.Builder builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_default_svc1");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        RuleMatcher.Builder rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc1").build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_all_ns_all_svc");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("*").setService("*").build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_default_svc2");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc2").build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_default_all_svc");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("*").build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_all_ns_svc2");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("*").setService("svc2").build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_all_ns_all_svc_default_svc3");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("*").setService("*").build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("default").setService("svc3").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_default_svc2_default_svc3");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc2").build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("default").setService("svc3").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        cbBuilder.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy").build());

        CircuitBreaker allRules = cbBuilder.build();
        ServiceRule svcRule = new ServiceRuleByProto(allRules, allRules.getRevision().getValue(), false,
                EventType.CIRCUIT_BREAKING);
        Function<String, Pattern> regexToPattern = new Function<String, Pattern>() {
            @Override
            public Pattern apply(String s) {
                return Pattern.compile(s);
            }
        };
        Resource resource = new ServiceResource(new ServiceKey("default", "svc2"));
        CircuitBreakerRule rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_default_svc2", rule.getName());

        resource = new ServiceResource(new ServiceKey("default", "svc1"));
        rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_default_svc1", rule.getName());

        resource = new ServiceResource(new ServiceKey("default", "svc2"),
                new ServiceKey("default", "svc3"));
        rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_default_svc2_default_svc3", rule.getName());

        resource = new ServiceResource(new ServiceKey("default1", "svc4"),
                new ServiceKey("default", "svc3"));
        rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_all_ns_all_svc_default_svc3", rule.getName());

        resource = new ServiceResource(new ServiceKey("default", "svc1"),
                new ServiceKey("default", "svc4"));
        rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_default_svc1", rule.getName());

        resource = new ServiceResource(new ServiceKey("default", "svc4"),
                new ServiceKey("default", "svc3"));
        rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_default_all_svc", rule.getName());
    }

    @Test
    public void testSelectRuleMethod() {
        CircuitBreaker.Builder cbBuilder = CircuitBreaker.newBuilder();
        // match one service rules
        CircuitBreakerRule.Builder builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_default_svc1_foo");
        builder.setEnable(true);
        builder.setLevel(Level.METHOD);
        RuleMatcher.Builder rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc1").setMethod(
                MatchString.newBuilder().setValue(StringValue.newBuilder().setValue("foo").build()).build()).build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_default_all_svc_foo1");
        builder.setEnable(true);
        builder.setLevel(Level.METHOD);
        rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("*").setMethod(
                MatchString.newBuilder().setValue(StringValue.newBuilder().setValue("foo1").build()).build()).build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_default_svc2_all");
        builder.setEnable(true);
        builder.setLevel(Level.METHOD);
        rmBuilder = RuleMatcher.newBuilder();
        rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc2").setMethod(
                MatchString.newBuilder().setValue(StringValue.newBuilder().setValue("*").build()).build()).build());
        rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
        builder.setRuleMatcher(rmBuilder.build());
        cbBuilder.addRules(builder);

        cbBuilder.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy").build());

        CircuitBreaker allRules = cbBuilder.build();
        ServiceRule svcRule = new ServiceRuleByProto(allRules, allRules.getRevision().getValue(), false,
                EventType.CIRCUIT_BREAKING);
        Function<String, Pattern> regexToPattern = new Function<String, Pattern>() {
            @Override
            public Pattern apply(String s) {
                return Pattern.compile(s);
            }
        };

        Resource resource = new MethodResource(new ServiceKey("default", "svc1"), "foo");
        CircuitBreakerRule rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_default_svc1_foo", rule.getName());

        resource = new MethodResource(new ServiceKey("default", "svc1"), "foo1");
        rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_default_all_svc_foo1", rule.getName());

        resource = new MethodResource(new ServiceKey("default", "svc2"), "foo2");
        rule = CircuitBreakerRuleContainer.selectRule(resource, svcRule, regexToPattern);
        Assert.assertNotNull(rule);
        Assert.assertEquals("test_cb_default_svc2_all", rule.getName());
    }
}
