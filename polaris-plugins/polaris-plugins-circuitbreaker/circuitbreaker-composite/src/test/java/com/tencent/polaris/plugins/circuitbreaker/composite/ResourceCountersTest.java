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

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus.Status;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.*;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.ErrorCondition.InputType;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.TriggerCondition.TriggerType;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString.MatchStringType;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ResourceCountersTest {

    private static class CheckSet {

        boolean hasOpen = false;
        boolean hasHalfOpen = false;
        boolean hasClose = false;
    }

    @Test
    public void testStatusChanged() throws InterruptedException {
        CircuitBreakerRule.Builder builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_rule");
        builder.setEnable(true);
        builder.setLevel(Level.METHOD);
        CircuitBreakerProto.BlockConfig.Builder blockConfigBuilder = CircuitBreakerProto.BlockConfig.newBuilder();
        blockConfigBuilder.addTriggerConditions(TriggerCondition.newBuilder().setTriggerType(TriggerType.CONSECUTIVE_ERROR).setErrorCount(5).build());
        blockConfigBuilder.addErrorConditions(ErrorCondition.newBuilder().setInputType(InputType.RET_CODE).setCondition(
                MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(StringValue.newBuilder().setValue("500").build()).build()).build());
        builder.addBlockConfigs(blockConfigBuilder.build());
        builder.setRecoverCondition(RecoverCondition.newBuilder().setConsecutiveSuccess(2).setSleepWindow(5).build());
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        Resource resource = new MethodResource(new ServiceKey("test", "TestSvc"), "foo");
        PolarisCircuitBreaker polarisCircuitBreaker = new PolarisCircuitBreaker();
        polarisCircuitBreaker.setCircuitBreakerConfig(ConfigAPIFactory.defaultConfig().getConsumer().getCircuitBreaker());
        ResourceCounters resourceCounters = new ResourceCounters(resource, builder.build(), scheduledExecutorService, polarisCircuitBreaker);
        CheckSet checkSet = new CheckSet();
        Resource methodResource = new MethodResource(new ServiceKey("test", "TestSvc"), "foo");
        for (int i = 0; i < 5; i++) {
            ResourceStat resourceStat = new ResourceStat(methodResource, 500, 1000, RetStatus.RetUnknown);
            resourceCounters.report(resourceStat);
            Thread.sleep(100);
        }
        Thread checkThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    CircuitBreakerStatus circuitBreakerStatus = resourceCounters.getCircuitBreakerStatus();
                    if (circuitBreakerStatus.getStatus() == Status.OPEN) {
                        checkSet.hasOpen = true;
                    } else if (circuitBreakerStatus.getStatus() == Status.HALF_OPEN) {
                        checkSet.hasHalfOpen = true;
                    } else if (circuitBreakerStatus.getStatus() == Status.CLOSE) {
                        checkSet.hasClose = true;
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        checkThread.setDaemon(true);
        checkThread.start();
        Thread.sleep(10 * 1000);
        Assert.assertTrue(checkSet.hasOpen);
        Assert.assertTrue(checkSet.hasHalfOpen);
    }

    @Test
    public void testDestroy() {
        CircuitBreakerRule.Builder builder = CircuitBreakerRule.newBuilder();
        builder.setName("test_cb_rule");
        builder.setEnable(true);
        builder.setLevel(Level.METHOD);
        BlockConfig.Builder blockConfigBuilder = BlockConfig.newBuilder();
        blockConfigBuilder.addTriggerConditions(TriggerCondition.newBuilder().setTriggerType(TriggerType.CONSECUTIVE_ERROR).setErrorCount(5).build());
        blockConfigBuilder.addErrorConditions(ErrorCondition.newBuilder().setCondition(MatchString.newBuilder().setValue(
                StringValue.newBuilder().setValue("500").build())).build());
        builder.addBlockConfigs(blockConfigBuilder.build());
        builder.setRecoverCondition(RecoverCondition.newBuilder().setConsecutiveSuccess(1).setSleepWindow(5).build());
        Resource resource = new MethodResource(new ServiceKey("test", "TestSvc"), "foo");

        PolarisCircuitBreaker polarisCircuitBreaker = new PolarisCircuitBreaker();
        polarisCircuitBreaker.setCircuitBreakerConfig(ConfigAPIFactory.defaultConfig().getConsumer().getCircuitBreaker());
        ResourceCounters resourceCounters = new ResourceCounters(resource, builder.build(), null, polarisCircuitBreaker);
        resourceCounters.setDestroyed(true);
        resourceCounters.report(new ResourceStat(resource, 500, 1000));
        resourceCounters.closeToOpen("cb_rule_status", "");
        resourceCounters.openToHalfOpen();
        resourceCounters.halfOpenToClose();
        resourceCounters.halfOpenToOpen();
        Assert.assertEquals(Status.CLOSE, resourceCounters.getCircuitBreakerStatus().getStatus());
    }
}
