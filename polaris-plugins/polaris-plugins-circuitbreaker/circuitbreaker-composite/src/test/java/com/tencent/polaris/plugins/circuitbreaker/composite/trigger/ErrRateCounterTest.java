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
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.TriggerCondition;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

public class ErrRateCounterTest {

    private static final int ERR_RATE_PERCENT = 40;

    @Test
    public void testErrorRateReport() {
        AtomicBoolean triggerOpen = new AtomicBoolean(false);
        CounterOptions counterOptions = new CounterOptions();
        TriggerCondition.Builder builder = TriggerCondition.newBuilder();
        builder.setErrorPercent(ERR_RATE_PERCENT);
        builder.setInterval(5);
        builder.setMinimumRequest(5);
        counterOptions.setTriggerCondition(builder.build());
        counterOptions.setStatusChangeHandler(new StatusChangeHandler() {
            @Override
            public void closeToOpen(String circuitBreaker, String reason) {
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
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        counterOptions.setExecutorService(scheduledExecutorService);
        counterOptions.setResource(
                new MethodResource(new ServiceKey("Test", "testSvc"), "foo"));
        ErrRateCounter counter = new ErrRateCounter("errRateTest", counterOptions);
        Thread reportThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    counter.report(i % 2 != 0, Pattern::compile);
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        reportThread.setDaemon(true);
        reportThread.start();
        System.out.println("start to wait 6000");
        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        scheduledExecutorService.shutdown();
        Assert.assertTrue(triggerOpen.get());

    }
}
