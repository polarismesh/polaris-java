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

package com.tencent.polaris.plugins.circuitbreaker.composite.trigger;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.plugins.circuitbreaker.composite.StatusChangeHandler;
import com.tencent.polaris.plugins.circuitbreaker.composite.trigger.ConsecutiveCounter;
import com.tencent.polaris.plugins.circuitbreaker.composite.trigger.CounterOptions;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.TriggerCondition;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;

public class ConsecutiveCounterTest {

    private static final int MAX_COUNT = 5;

    @Test
    public void testConsecutiveReport() {
        AtomicBoolean triggerOpen = new AtomicBoolean(false);
        CounterOptions counterOptions = new CounterOptions();
        TriggerCondition.Builder builder = TriggerCondition.newBuilder();
        builder.setErrorCount(MAX_COUNT);
        counterOptions.setTriggerCondition(builder.build());
        counterOptions.setResource(
                new MethodResource(new ServiceKey("Test", "testSvc"), "foo"));
        counterOptions.setStatusChangeHandler(new StatusChangeHandler() {
            @Override
            public void closeToOpen(String circuitBreaker) {
                triggerOpen.set(true);
            }

            @Override
            public void openToHalfOpen() {

            }

            @Override
            public void halfOpenToClose() {

            }

            @Override
            public void halfOpenToOpen() {

            }
        });
        ConsecutiveCounter consecutiveCounter = new ConsecutiveCounter("test-rule", counterOptions);
        for (int i = 0; i < MAX_COUNT; i++) {
            consecutiveCounter.report(false);
        }
        Assert.assertTrue(triggerOpen.get());
    }


}
