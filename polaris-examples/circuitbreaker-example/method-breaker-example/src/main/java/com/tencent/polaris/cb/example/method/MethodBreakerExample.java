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

package com.tencent.polaris.cb.example.method;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import java.util.function.Consumer;

public class MethodBreakerExample {

    public static void main(String[] args) {

        CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPI();
        FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest();
        makeDecoratorRequest.setService(new ServiceKey("default", "testSvc2"));
        makeDecoratorRequest.setMethod("foo");
        FunctionalDecorator decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
        Consumer<Boolean> integerConsumer = decorator.decorateConsumer(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean success) {
                if (!success) {
                    throw new IllegalArgumentException("value divide 2 is zero");
                } else {
                    System.out.println("invoke success");
                }
            }
        });
        boolean success = false;
        int afterCount = 20;
        for (int i = 0; i < 500; i++) {
            try {
                integerConsumer.accept(success);
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
