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

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.cb.example.common.Utils;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.client.api.DefaultCircuitBreakAPI;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.circuitbreak.factory.CircuitBreakAPIFactory;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import java.util.function.Consumer;

public class MethodBreakerExample {

    private static final int PORT_NORMAL = 10885;

    private static final int PORT_ABNORMAL = 10886;

    private static final String NAMESPACE = "test";

    private static final String SERVICE = "methodCbService";

    private static final String LOCAL_HOST = "127.0.0.1";

    public static void main(String[] args) throws Exception {
        Utils.createHttpServers(PORT_NORMAL, PORT_ABNORMAL);
        int[] ports = new int[]{PORT_NORMAL, PORT_ABNORMAL};
        CircuitBreakAPI circuitBreakAPI = CircuitBreakAPIFactory.createCircuitBreakAPI();
        ConsumerAPI consumerAPI = DiscoveryAPIFactory
                .createConsumerAPIByContext(((DefaultCircuitBreakAPI) circuitBreakAPI).getSDKContext());
        FunctionalDecoratorRequest makeDecoratorRequest = new FunctionalDecoratorRequest(
                new ServiceKey(NAMESPACE, SERVICE), "echo");
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
            boolean hasError = false;
            try {
                integerConsumer.accept(success);
                afterCount--;
                if (afterCount == 0) {
                    success = false;
                }
            } catch (Exception e) {
                hasError = true;
                System.out.println(e.getMessage());
                if (e instanceof CallAbortedException) {
                    success = true;
                    afterCount = 20;
                }
            } finally {
                // report to active health check
                ServiceCallResult serviceCallResult = new ServiceCallResult();
                serviceCallResult.setNamespace(NAMESPACE);
                serviceCallResult.setService(SERVICE);
                serviceCallResult.setHost(LOCAL_HOST);
                serviceCallResult.setPort(ports[i % 2]);
                serviceCallResult.setProtocol("http");
                serviceCallResult.setRetCode(hasError ? 500 : 200);
                serviceCallResult.setDelay(10);
                consumerAPI.updateServiceCallResult(serviceCallResult);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
