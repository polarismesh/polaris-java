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

import java.util.function.Function;
import java.util.regex.Pattern;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.ServiceResource;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.plugins.circuitbreaker.composite.CircuitBreakerRuleDictionary;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.CircuitBreaker;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.CircuitBreakerRule;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher.DestinationService;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.RuleMatcher.SourceService;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import org.junit.Assert;
import org.junit.Test;

public class CircuitBreakerRuleContainerTest {

	@Test
	public void testSelectRuleService() {

		// match one service rules
		CircuitBreakerRule.Builder builderDefaultSvc1 = CircuitBreakerRule.newBuilder();
		builderDefaultSvc1.setName("test_cb_default_svc1");
		builderDefaultSvc1.setEnable(true);
		builderDefaultSvc1.setLevel(Level.SERVICE);
		RuleMatcher.Builder rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc1").build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
		builderDefaultSvc1.setRuleMatcher(rmBuilder.build());

		CircuitBreakerRule.Builder builderAllNsAllSvc = CircuitBreakerRule.newBuilder();
		builderAllNsAllSvc.setName("test_cb_all_ns_all_svc");
		builderAllNsAllSvc.setEnable(true);
		builderAllNsAllSvc.setLevel(Level.SERVICE);
		rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("*").setService("*").build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
		builderAllNsAllSvc.setRuleMatcher(rmBuilder.build());

		CircuitBreakerRule.Builder builderDefaultSvc2 = CircuitBreakerRule.newBuilder();
		builderDefaultSvc2.setName("test_cb_default_svc2");
		builderDefaultSvc2.setEnable(true);
		builderDefaultSvc2.setLevel(Level.SERVICE);
		rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc2").build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
		builderDefaultSvc2.setRuleMatcher(rmBuilder.build());

		CircuitBreakerRule.Builder builderDefaultAllSvc = CircuitBreakerRule.newBuilder();
		builderDefaultAllSvc.setName("test_cb_default_all_svc");
		builderDefaultAllSvc.setEnable(true);
		builderDefaultAllSvc.setLevel(Level.SERVICE);
		rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("*").build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
		builderDefaultAllSvc.setRuleMatcher(rmBuilder.build());

		CircuitBreakerRule.Builder builderAllNsSvc2 = CircuitBreakerRule.newBuilder();
		builderAllNsSvc2.setName("test_cb_all_ns_svc2");
		builderAllNsSvc2.setEnable(true);
		builderAllNsSvc2.setLevel(Level.SERVICE);
		rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("*").setService("svc2").build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
		builderAllNsSvc2.setRuleMatcher(rmBuilder.build());

		CircuitBreakerRule.Builder builderAllSvcDefaultSvc3 = CircuitBreakerRule.newBuilder();
		builderAllSvcDefaultSvc3.setName("test_cb_all_ns_all_svc_default_svc3");
		builderAllSvcDefaultSvc3.setEnable(true);
		builderAllSvcDefaultSvc3.setLevel(Level.SERVICE);
		rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("*").setService("*").build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("default").setService("svc3").build());
		builderAllSvcDefaultSvc3.setRuleMatcher(rmBuilder.build());

		CircuitBreakerRule.Builder builderDefaultSvc2DefaultSvc3 = CircuitBreakerRule.newBuilder();
		builderDefaultSvc2DefaultSvc3.setName("test_cb_default_svc2_default_svc3");
		builderDefaultSvc2DefaultSvc3.setEnable(true);
		builderDefaultSvc2DefaultSvc3.setLevel(Level.SERVICE);
		rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc2").build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("default").setService("svc3").build());
		builderDefaultSvc2DefaultSvc3.setRuleMatcher(rmBuilder.build());

		Function<String, Pattern> regexToPattern = new Function<String, Pattern>() {
			@Override
			public Pattern apply(String s) {
				return Pattern.compile(s);
			}
		};

		CircuitBreakerRuleDictionary circuitBreakerRuleDictionary = new CircuitBreakerRuleDictionary(regexToPattern);

		ServiceKey svc1 = new ServiceKey("default", "svc1");
		ServiceKey svc2 = new ServiceKey("default", "svc2");
		ServiceKey svc4 = new ServiceKey("default", "svc4");
		ServiceKey svc14 = new ServiceKey("default1", "svc4");

		CircuitBreaker.Builder cbBuilderSvc1 = CircuitBreaker.newBuilder();
		cbBuilderSvc1.addRules(builderDefaultSvc1);
		cbBuilderSvc1.addRules(builderAllNsAllSvc);
		cbBuilderSvc1.addRules(builderDefaultAllSvc);
		cbBuilderSvc1.addRules(builderAllSvcDefaultSvc3);
		cbBuilderSvc1.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy-svc1").build());
		CircuitBreaker svc1Rules = cbBuilderSvc1.build();
		ServiceRule svcRule1 = new ServiceRuleByProto(svc1Rules, svc1Rules.getRevision().getValue(), false,
				EventType.CIRCUIT_BREAKING);
		circuitBreakerRuleDictionary.putServiceRule(svc1, svcRule1);

		CircuitBreaker.Builder cbBuilderSvc2 = CircuitBreaker.newBuilder();
		cbBuilderSvc2.addRules(builderAllNsAllSvc);
		cbBuilderSvc2.addRules(builderDefaultSvc2);
		cbBuilderSvc2.addRules(builderDefaultAllSvc);
		cbBuilderSvc2.addRules(builderAllNsSvc2);
		cbBuilderSvc2.addRules(builderAllSvcDefaultSvc3);
		cbBuilderSvc2.addRules(builderDefaultSvc2DefaultSvc3);
		cbBuilderSvc2.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy-svc2").build());
		CircuitBreaker svc2Rules = cbBuilderSvc2.build();
		ServiceRule svcRule2 = new ServiceRuleByProto(svc2Rules, svc2Rules.getRevision().getValue(), false,
				EventType.CIRCUIT_BREAKING);
		circuitBreakerRuleDictionary.putServiceRule(svc2, svcRule2);

		CircuitBreaker.Builder cbBuilderSvc4 = CircuitBreaker.newBuilder();
		cbBuilderSvc4.addRules(builderAllNsAllSvc);
		cbBuilderSvc4.addRules(builderDefaultAllSvc);
		cbBuilderSvc4.addRules(builderAllSvcDefaultSvc3);
		cbBuilderSvc4.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy-svc4").build());
		CircuitBreaker svc4Rules = cbBuilderSvc4.build();
		ServiceRule svcRule4 = new ServiceRuleByProto(svc4Rules, svc4Rules.getRevision().getValue(), false,
				EventType.CIRCUIT_BREAKING);
		circuitBreakerRuleDictionary.putServiceRule(svc4, svcRule4);

		CircuitBreaker.Builder cbBuilderSvc14 = CircuitBreaker.newBuilder();
		cbBuilderSvc14.addRules(builderAllNsAllSvc);
		cbBuilderSvc14.addRules(builderAllSvcDefaultSvc3);
		cbBuilderSvc14.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy-svc14").build());
		CircuitBreaker svc14Rules = cbBuilderSvc2.build();
		ServiceRule svcRule14 = new ServiceRuleByProto(svc14Rules, svc14Rules.getRevision().getValue(), false,
				EventType.CIRCUIT_BREAKING);
		circuitBreakerRuleDictionary.putServiceRule(svc14, svcRule14);

		Resource resource = new ServiceResource(new ServiceKey("default", "svc1"));
		CircuitBreakerRule rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_default_svc1", rule.getName());

		resource = new ServiceResource(new ServiceKey("default", "svc1"),
				new ServiceKey("default", "svc4"));
		rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_default_svc1", rule.getName());

		resource = new ServiceResource(new ServiceKey("default", "svc2"));
		rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_default_svc2", rule.getName());

		resource = new ServiceResource(new ServiceKey("default", "svc2"),
				new ServiceKey("default", "svc3"));
		rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_default_svc2_default_svc3", rule.getName());

		resource = new ServiceResource(new ServiceKey("default1", "svc4"),
				new ServiceKey("default", "svc3"));
		rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_all_ns_all_svc_default_svc3", rule.getName());

		resource = new ServiceResource(new ServiceKey("default", "svc4"),
				new ServiceKey("default", "svc3"));
		rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_default_all_svc", rule.getName());
	}

	@Test
	public void testSelectRuleMethod() {
		// match one service rules
		CircuitBreakerRule.Builder builderDefaultSvc1 = CircuitBreakerRule.newBuilder();
		builderDefaultSvc1.setName("test_cb_default_svc1_foo");
		builderDefaultSvc1.setEnable(true);
		builderDefaultSvc1.setLevel(Level.METHOD);
		RuleMatcher.Builder rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc1").setMethod(
				MatchString.newBuilder().setValue(StringValue.newBuilder().setValue("foo").build()).build()).build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
		builderDefaultSvc1.setRuleMatcher(rmBuilder.build());

		CircuitBreakerRule.Builder builderDefaultAllSvcFoo1 = CircuitBreakerRule.newBuilder();
		builderDefaultAllSvcFoo1.setName("test_cb_default_all_svc_foo1");
		builderDefaultAllSvcFoo1.setEnable(true);
		builderDefaultAllSvcFoo1.setLevel(Level.METHOD);
		rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("*").setMethod(
				MatchString.newBuilder().setValue(StringValue.newBuilder().setValue("foo1").build()).build()).build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
		builderDefaultAllSvcFoo1.setRuleMatcher(rmBuilder.build());

		CircuitBreakerRule.Builder builderDefaultSvc2 = CircuitBreakerRule.newBuilder();
		builderDefaultSvc2.setName("test_cb_default_svc2_all");
		builderDefaultSvc2.setEnable(true);
		builderDefaultSvc2.setLevel(Level.METHOD);
		rmBuilder = RuleMatcher.newBuilder();
		rmBuilder.setDestination(DestinationService.newBuilder().setNamespace("default").setService("svc2").setMethod(
				MatchString.newBuilder().setValue(StringValue.newBuilder().setValue("*").build()).build()).build());
		rmBuilder.setSource(SourceService.newBuilder().setNamespace("*").setService("*").build());
		builderDefaultSvc2.setRuleMatcher(rmBuilder.build());

		ServiceKey svc1 = new ServiceKey("default", "svc1");
		ServiceKey svc2 = new ServiceKey("default", "svc2");

		Function<String, Pattern> regexToPattern = new Function<String, Pattern>() {
			@Override
			public Pattern apply(String s) {
				return Pattern.compile(s);
			}
		};
		CircuitBreakerRuleDictionary circuitBreakerRuleDictionary = new CircuitBreakerRuleDictionary(regexToPattern);

		CircuitBreaker.Builder cbBuilderSvc1 = CircuitBreaker.newBuilder();
		cbBuilderSvc1.addRules(builderDefaultSvc1);
		cbBuilderSvc1.addRules(builderDefaultAllSvcFoo1);
		cbBuilderSvc1.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy-svc1").build());
		CircuitBreaker svc1Rules = cbBuilderSvc1.build();
		ServiceRule svcRule1 = new ServiceRuleByProto(svc1Rules, svc1Rules.getRevision().getValue(), false,
				EventType.CIRCUIT_BREAKING);
		circuitBreakerRuleDictionary.putServiceRule(svc1, svcRule1);

		CircuitBreaker.Builder cbBuilderSvc2 = CircuitBreaker.newBuilder();
		cbBuilderSvc2.addRules(builderDefaultAllSvcFoo1);
		cbBuilderSvc2.addRules(builderDefaultSvc2);
		cbBuilderSvc2.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy-svc2").build());
		CircuitBreaker svc2Rules = cbBuilderSvc2.build();
		ServiceRule svcRule2 = new ServiceRuleByProto(svc2Rules, svc2Rules.getRevision().getValue(), false,
				EventType.CIRCUIT_BREAKING);
		circuitBreakerRuleDictionary.putServiceRule(svc2, svcRule2);

		Resource resource = new MethodResource(new ServiceKey("default", "svc1"), "foo");
		CircuitBreakerRule rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_default_svc1_foo", rule.getName());

		resource = new MethodResource(new ServiceKey("default", "svc1"), "foo1");
		rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_default_all_svc_foo1", rule.getName());

		resource = new MethodResource(new ServiceKey("default", "svc2"), "foo2");
		rule = circuitBreakerRuleDictionary.lookupCircuitBreakerRule(resource);
		Assert.assertNotNull(rule);
		Assert.assertEquals("test_cb_default_svc2_all", rule.getName());
	}
}
