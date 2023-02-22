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

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceBreakerExample {

    public static void main(String[] args) {

        CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPI();
        FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest();
        makeDecoratorRequest.setService(new ServiceKey("default", "polaris-circuitbreaker-example-b"));
        makeDecoratorRequest.setMethod("info");
        FunctionalDecorator decorator = circuitBreakAPI.makeFunctionalDecorator(makeDecoratorRequest);
        ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPI();
        // 封装函数接口
        Supplier<String> decoratedFunction = decorator.decorateSupplier(() -> Consumer.invokeByNameResolution(consumerAPI));

        // 通过执行函数接口，进行服务调用
        // 在调用过程中，如果出现熔断，会抛出CallAbortedException异常
        for (int i = 0; i < 100; i++) {
            try {
                System.out.println("index: " + i);
                String msg = decoratedFunction.get();
                System.out.println("msg: " + msg);
            } catch(Exception e) {
                e.printStackTrace();
            }finally {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {

                }
            }
        }
    }
}

