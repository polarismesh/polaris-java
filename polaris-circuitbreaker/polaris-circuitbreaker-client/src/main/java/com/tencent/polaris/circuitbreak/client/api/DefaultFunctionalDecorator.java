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

package com.tencent.polaris.circuitbreak.client.api;

import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DefaultFunctionalDecorator extends DefaultInvokeHandler implements FunctionalDecorator {

    private final InvokeContext invokeContext;

    public DefaultFunctionalDecorator(FunctionalDecoratorRequest makeDecoratorRequest,
            CircuitBreakAPI circuitBreakAPI) {
        super(circuitBreakAPI);
        this.invokeContext = new InvokeContext(makeDecoratorRequest, null);
    }

    @Override
    public <T> Supplier<T> decorateSupplier(Supplier<T> supplier) {
        return new Supplier<T>() {
            @Override
            public T get() {
                acquirePermission(invokeContext);
                long startTimeMilli = System.currentTimeMillis();
                try {
                    T result = supplier.get();
                    invokeContext.setResponseContext(new InvokeContext.ResponseContext(System.currentTimeMillis() - startTimeMilli, TimeUnit.MILLISECONDS, result, null));
                    onSuccess(invokeContext);
                    return result;
                } catch (Throwable e) {
                    invokeContext.setResponseContext(new InvokeContext.ResponseContext(System.currentTimeMillis() - startTimeMilli, TimeUnit.MILLISECONDS, null, e));
                    onError(invokeContext);
                    throw e;
                }
            }
        };
    }

    @Override
    public <T> Consumer<T> decorateConsumer(Consumer<T> consumer) {
        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                acquirePermission(invokeContext);
                long startTimeMilli = System.currentTimeMillis();
                try {
                    consumer.accept(t);
                    invokeContext.setResponseContext(new InvokeContext.ResponseContext(System.currentTimeMillis() - startTimeMilli, TimeUnit.MILLISECONDS, null, null));
                    onSuccess(invokeContext);
                } catch (Throwable e) {
                    invokeContext.setResponseContext(new InvokeContext.ResponseContext(System.currentTimeMillis() - startTimeMilli, TimeUnit.MILLISECONDS, null, e));
                    onError(invokeContext);
                    throw e;
                }
            }
        };
    }

    @Override
    public <T, R> Function<T, R> decorateFunction(Function<T, R> function) {
        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                acquirePermission(invokeContext);
                long startTimeMilli = System.currentTimeMillis();
                try {
                    R result = function.apply(t);
                    invokeContext.setResponseContext(new InvokeContext.ResponseContext(System.currentTimeMillis() - startTimeMilli, TimeUnit.MILLISECONDS, result, null));
                    onSuccess(invokeContext);
                    return result;
                } catch (Throwable e) {
                    invokeContext.setResponseContext(new InvokeContext.ResponseContext(System.currentTimeMillis() - startTimeMilli, TimeUnit.MILLISECONDS, null, e));
                    onError(invokeContext);
                    throw e;
                }
            }
        };
    }

    @Override
    public <T> Predicate<T> decoratePredicate(Predicate<T> predicate) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                acquirePermission(invokeContext);
                long startTimeMilli = System.currentTimeMillis();
                try {
                    boolean result = predicate.test(t);
                    invokeContext.setResponseContext(new InvokeContext.ResponseContext(System.currentTimeMillis() - startTimeMilli, TimeUnit.MILLISECONDS, result, null));
                    onSuccess(invokeContext);
                    return result;
                } catch (Throwable e) {
                    invokeContext.setResponseContext(new InvokeContext.ResponseContext(System.currentTimeMillis() - startTimeMilli, TimeUnit.MILLISECONDS, null, e));
                    onError(invokeContext);
                    throw e;
                }
            }
        };
    }

}
