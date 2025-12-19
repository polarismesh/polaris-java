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

package com.tencent.polaris.plugins.circuitbreaker.composite;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.StringValue;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.ServiceResource;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.Status;
import com.tencent.polaris.api.pojo.ServiceEventKey;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.pojo.ServiceRule;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.*;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.ErrorCondition.InputType;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.TriggerCondition.TriggerType;
import com.tencent.polaris.specification.api.v1.model.ModelProto;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString.MatchStringType;
import com.tencent.polaris.test.common.MockInitContext;
import com.tencent.polaris.test.mock.discovery.MockServiceResourceProvider;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

public class PolarisCircuitBreakerTest {

    public CircuitBreaker buildRules() {
        CircuitBreakerRule.Builder builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_rule");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        BlockConfig.Builder blockConfigBuilder = BlockConfig.newBuilder();
        blockConfigBuilder.addTriggerConditions(TriggerCondition.newBuilder().setTriggerType(TriggerType.CONSECUTIVE_ERROR).setErrorCount(10).build());
        blockConfigBuilder.addErrorConditions(ErrorCondition.newBuilder().setInputType(InputType.RET_CODE).setCondition(
                MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(StringValue.newBuilder().setValue("500").build()).build()).build());
        builder.addBlockConfigs(blockConfigBuilder.build());
        builder.setRecoverCondition(RecoverCondition.newBuilder().setConsecutiveSuccess(2).setSleepWindow(5).build());
        builder.setRevision("111");
        CircuitBreaker.Builder cbBuilder = CircuitBreaker.newBuilder();
        cbBuilder.addRules(builder);
        cbBuilder.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy").build());
        return cbBuilder.build();
    }

    public CircuitBreaker buildAnotherRules() {
        CircuitBreakerRule.Builder builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_rule1");
        builder.setEnable(true);
        builder.setLevel(Level.SERVICE);
        BlockConfig.Builder blockConfigBuilder = BlockConfig.newBuilder();
        blockConfigBuilder.addTriggerConditions(TriggerCondition.newBuilder().setTriggerType(TriggerType.CONSECUTIVE_ERROR).setErrorCount(3).build());
        blockConfigBuilder.addErrorConditions(ErrorCondition.newBuilder().setInputType(InputType.RET_CODE).setCondition(
                MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(StringValue.newBuilder().setValue("500").build()).build()).build());
        builder.addBlockConfigs(blockConfigBuilder.build());
        builder.setRecoverCondition(RecoverCondition.newBuilder().setConsecutiveSuccess(2).setSleepWindow(5).build());
        builder.setRevision("222");
        CircuitBreaker.Builder cbBuilder = CircuitBreaker.newBuilder();
        cbBuilder.addRules(builder);
        cbBuilder.setRevision(StringValue.newBuilder().setValue("xxxxxyyyyyy11").build());
        return cbBuilder.build();
    }

    @Test
    public void testCheckResource() {
        MockServiceResourceProvider mockServiceRuleProvider = new MockServiceResourceProvider();
        PolarisCircuitBreaker polarisCircuitBreaker = new PolarisCircuitBreaker();
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        InitContext initContext = new MockInitContext(configuration);
        polarisCircuitBreaker.init(initContext);
        polarisCircuitBreaker.setServiceRuleProvider(mockServiceRuleProvider);
        polarisCircuitBreaker.setCircuitBreakerRuleDictionary(new CircuitBreakerRuleDictionary(Pattern::compile, null));
        ServiceKey serviceKey = new ServiceKey("Test", "testSvc");
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.CIRCUIT_BREAKING);
        CircuitBreaker circuitBreaker = buildRules();
        ServiceRule serviceRule = new ServiceRuleByProto(circuitBreaker, circuitBreaker.getRevision().getValue(), false,
                EventType.CIRCUIT_BREAKING);
        mockServiceRuleProvider.putServiceRule(serviceEventKey, serviceRule);
        Resource svcResource = new ServiceResource(serviceKey);
        for (int i = 0; i < 11; i++) {
            ResourceStat resourceStat = new ResourceStat(svcResource, 500, 1000);
            polarisCircuitBreaker.report(resourceStat);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        CircuitBreakerStatus circuitBreakerStatus = polarisCircuitBreaker.checkResource(svcResource);
        assertThat(circuitBreakerStatus).isNotNull();
        Status status = circuitBreakerStatus.getStatus();
        assertThat(status).isEqualTo(Status.OPEN);
    }

    @Test
    public void testRuleChanged() {
        MockServiceResourceProvider mockServiceRuleProvider = new MockServiceResourceProvider();
        PolarisCircuitBreaker polarisCircuitBreaker = new PolarisCircuitBreaker();
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        InitContext initContext = new MockInitContext(configuration);
        polarisCircuitBreaker.init(initContext);
        polarisCircuitBreaker.setServiceRuleProvider(mockServiceRuleProvider);
        polarisCircuitBreaker.setCircuitBreakerRuleDictionary(new CircuitBreakerRuleDictionary(Pattern::compile, null));
        polarisCircuitBreaker.setFaultDetectRuleDictionary(new FaultDetectRuleDictionary());
        ServiceKey serviceKey = new ServiceKey("Test", "testSvc");
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.CIRCUIT_BREAKING);
        CircuitBreaker circuitBreaker = buildRules();
        ServiceRuleByProto serviceRule = new ServiceRuleByProto(circuitBreaker, circuitBreaker.getRevision().getValue(),
                false,
                EventType.CIRCUIT_BREAKING);
        mockServiceRuleProvider.putServiceRule(serviceEventKey, serviceRule);
        Resource svcResource = new ServiceResource(serviceKey);
        ResourceStat resourceStat = new ResourceStat(svcResource, 500, 1000);
        polarisCircuitBreaker.report(resourceStat);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        CircuitBreaker circuitBreaker1 = buildAnotherRules();
        ServiceRuleByProto serviceRule1 = new ServiceRuleByProto(circuitBreaker1,
                circuitBreaker1.getRevision().getValue(), false,
                EventType.CIRCUIT_BREAKING);
        mockServiceRuleProvider.putServiceRule(serviceEventKey, serviceRule1);
        CircuitBreakerRuleListener listener = new CircuitBreakerRuleListener(polarisCircuitBreaker);
        listener.onResourceUpdated(serviceEventKey, serviceRule, serviceRule1);
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 3; i++) {
            ResourceStat resourceStat1 = new ResourceStat(svcResource, 500, 1000);
            polarisCircuitBreaker.report(resourceStat1);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        CircuitBreakerStatus circuitBreakerStatus = polarisCircuitBreaker.checkResource(svcResource);
        Status status = circuitBreakerStatus.getStatus();
        assertThat(status).isEqualTo(Status.OPEN);
    }

    @Test
    public void testCache() {
        Cache<String, Optional<String>> cache = CacheBuilder.newBuilder().build();
        cache.put("1111", Optional.of("ssss"));
        Optional<String> value = cache.getIfPresent("2222");
        System.out.println(value);
    }

    public CircuitBreaker buildMethodRules() {
        CircuitBreakerRule.Builder builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_method_rule");
        builder.setEnable(true);
        builder.setLevel(Level.METHOD);
        builder.setRuleMatcher(
                CircuitBreakerProto.RuleMatcher.newBuilder().
                        setSource(CircuitBreakerProto.RuleMatcher.SourceService.newBuilder().setNamespace("*").setService("*").build()).
                        setDestination(CircuitBreakerProto.RuleMatcher.DestinationService.newBuilder().
                                setNamespace("Test").setService("testMethodSvc").build()));
        BlockConfig.Builder blockConfigBuilder = BlockConfig.newBuilder();
        blockConfigBuilder.setApi(ModelProto.API.newBuilder().setProtocol("*").setMethod("*").setPath(MatchString.newBuilder().
                setValue(StringValue.newBuilder().setValue("^/d/customers/base/.+$")).
                setType(MatchStringType.REGEX)).build());
        blockConfigBuilder.addTriggerConditions(TriggerCondition.newBuilder().setTriggerType(TriggerType.CONSECUTIVE_ERROR).setErrorCount(10).build());
        blockConfigBuilder.addErrorConditions(ErrorCondition.newBuilder().setInputType(InputType.RET_CODE).setCondition(
                MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(StringValue.newBuilder().setValue("500").build()).build()).build()).build();
        builder.addBlockConfigs(blockConfigBuilder.build());
        builder.setRecoverCondition(RecoverCondition.newBuilder().setConsecutiveSuccess(2).setSleepWindow(5).build());
        builder.setRevision("222");
        CircuitBreaker.Builder cbBuilder = CircuitBreaker.newBuilder();
        cbBuilder.addRules(builder);
        cbBuilder.setRevision(StringValue.newBuilder().setValue("xxxxxy1yyyx").build());
        return cbBuilder.build();
    }

    @Test
    public void testCircuitBreakerMethodRules() {
        MockServiceResourceProvider mockServiceRuleProvider = new MockServiceResourceProvider();
        PolarisCircuitBreaker polarisCircuitBreaker = new PolarisCircuitBreaker();
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
        configurationImpl.getConsumer().getCircuitBreaker().setCountersExpireInterval(5000);
        InitContext initContext = new MockInitContext(configuration);
        polarisCircuitBreaker.init(initContext);
        polarisCircuitBreaker.setServiceRuleProvider(mockServiceRuleProvider);
        polarisCircuitBreaker.setCircuitBreakerRuleDictionary(new CircuitBreakerRuleDictionary(Pattern::compile, null));
        polarisCircuitBreaker.setFaultDetectRuleDictionary(new FaultDetectRuleDictionary());
        ServiceKey serviceKey = new ServiceKey("Test", "testMethodSvc");
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.CIRCUIT_BREAKING);
        CircuitBreaker circuitBreaker = buildMethodRules();
        ServiceRuleByProto serviceRule = new ServiceRuleByProto(circuitBreaker, circuitBreaker.getRevision().getValue(),
                false,
                EventType.CIRCUIT_BREAKING);
        mockServiceRuleProvider.putServiceRule(serviceEventKey, serviceRule);

        for (int i = 0; i < 1000; i++) {
            Resource methodResource = new MethodResource(serviceKey, String.format("/d/customers/base/%d", i));
            ResourceStat resourceStat = new ResourceStat(methodResource, 500, 1000);
            polarisCircuitBreaker.report(resourceStat);
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 1000; i++) {
            Resource methodResource = new MethodResource(serviceKey, String.format("/d/customers/base/%d", i));
            CircuitBreakerStatus circuitBreakerStatus = polarisCircuitBreaker.checkResource(methodResource);
            assertThat(circuitBreakerStatus.getStatus()).isEqualTo(Status.OPEN);
        }
        assertThat(polarisCircuitBreaker.getCountersCache().get(Level.METHOD).size()).isEqualTo(1);
        assertThat(polarisCircuitBreaker.getResourceMappingSize()).isEqualTo(1000);

        //check cleanup
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        polarisCircuitBreaker.cleanupExpiredResources();
        assertThat(polarisCircuitBreaker.getResourceMappingSize()).isEqualTo(0);
    }

    @Test
    public void testCircuitBreakerRuleExpiration() throws InterruptedException {
        MockServiceResourceProvider mockServiceRuleProvider = new MockServiceResourceProvider();
        PolarisCircuitBreaker polarisCircuitBreaker = new PolarisCircuitBreaker();
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        ConfigurationImpl configurationImpl = (ConfigurationImpl) configuration;
        configurationImpl.getConsumer().getCircuitBreaker().setDefaultRuleEnable(false);
        InitContext initContext = new MockInitContext(configuration);
        polarisCircuitBreaker.init(initContext);
        polarisCircuitBreaker.setServiceRuleProvider(mockServiceRuleProvider);
        polarisCircuitBreaker.setCircuitBreakerRuleDictionary(new CircuitBreakerRuleDictionary(Pattern::compile, null));
        polarisCircuitBreaker.setFaultDetectRuleDictionary(new FaultDetectRuleDictionary());
        ServiceKey serviceKey = new ServiceKey("Test", "testMethodSvc");
        ServiceEventKey serviceEventKey = new ServiceEventKey(serviceKey, EventType.CIRCUIT_BREAKING);
        CircuitBreaker circuitBreaker = buildRules();
        ServiceRuleByProto serviceRule = new ServiceRuleByProto(circuitBreaker, circuitBreaker.getRevision().getValue(),
                false,
                EventType.CIRCUIT_BREAKING);
        mockServiceRuleProvider.putServiceRule(serviceEventKey, serviceRule);

        // 创建熔断器
        Resource svcResource = new ServiceResource(serviceKey);
        ResourceStat resourceStat = new ResourceStat(svcResource, 500, 1000);
        polarisCircuitBreaker.report(resourceStat);
        Map<Level, Cache<Resource, Optional<ResourceCounters>>> countersCache = polarisCircuitBreaker.getCountersCache();
        Cache<Resource, Optional<ResourceCounters>> cacheValue = countersCache.get(Level.SERVICE);
        assertThat(cacheValue.getIfPresent(svcResource)).isNotNull();

        // 熔断规则过期清理熔断器
        polarisCircuitBreaker.onCircuitBreakerRuleChanged(serviceKey);
        assertThat(cacheValue.getIfPresent(svcResource)).isNull();

        // 重新创建熔断器
        polarisCircuitBreaker.report(resourceStat);
        assertThat(cacheValue.getIfPresent(svcResource)).isNotNull();
    }
}
