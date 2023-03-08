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

package com.tencent.polaris.cb.example.service;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import java.util.function.Consumer;

public class ServiceBreakerExample {

    private static class Condition {

        final boolean success;
        final int count;

        public Condition(boolean success, int count) {
            this.success = success;
            this.count = count;
        }
    }

    public static void main(String[] args) {
        CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPI();
        FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(new ServiceKey("default", "testService1"), "");
        FunctionalDecorator decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
        Consumer<Condition> integerConsumer = decorator.decorateConsumer(new Consumer<Condition>() {
            @Override
            public void accept(Condition condition) {
                if (!condition.success) {
                    if (condition.count % 2 == 0) {
                        throw new IllegalArgumentException("value divide 2 is zero");
                    }
                }
                System.out.println("invoke success");
            }
        });
        boolean success = false;
        int afterCount = 20;
        for (int i = 0; i < 500; i++) {
            try {
                integerConsumer.accept(new Condition(success, i + 1));
                afterCount--;
                if (afterCount == 0) {
                    success = false;
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                if (e instanceof CallAbortedException) {
                    success = true;
                    afterCount = 20;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

