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

package com.tencent.polaris.circuitbreak.client.api;

import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.FunctionalDecorator;
import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.pojo.FunctionalDecoratorRequest;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DefaultFunctionalDecorator implements FunctionalDecorator {

    private final InvokeHandler invokeHandler;

    public DefaultFunctionalDecorator(FunctionalDecoratorRequest makeDecoratorRequest,
                                      CircuitBreakAPI circuitBreakAPI) {
        this.invokeHandler = new DefaultInvokeHandler(makeDecoratorRequest, circuitBreakAPI);
    }

    @Override
    public <T> Supplier<T> decorateSupplier(Supplier<T> supplier) {
        return new Supplier<T>() {
            @Override
            public T get() {
                invokeHandler.acquirePermission();
                long startTimeMilli = System.currentTimeMillis();
                try {
                    T result = supplier.get();
                    long delay = System.currentTimeMillis() - startTimeMilli;
                    InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                    responseContext.setDuration(delay);
                    responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
                    responseContext.setResult(result);
                    invokeHandler.onSuccess(responseContext);
                    return result;
                } catch (Throwable e) {
                    long delay = System.currentTimeMillis() - startTimeMilli;
                    InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                    responseContext.setDuration(delay);
                    responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
                    responseContext.setError(e);
                    invokeHandler.onError(responseContext);
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
                invokeHandler.acquirePermission();
                long startTimeMilli = System.currentTimeMillis();
                try {
                    consumer.accept(t);
                    long delay = System.currentTimeMillis() - startTimeMilli;
                    InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                    responseContext.setDuration(delay);
                    responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
                    invokeHandler.onSuccess(responseContext);
                } catch (Throwable e) {
                    long delay = System.currentTimeMillis() - startTimeMilli;
                    InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                    responseContext.setDuration(delay);
                    responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
                    responseContext.setError(e);
                    invokeHandler.onError(responseContext);
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
                invokeHandler.acquirePermission();
                long startTimeMilli = System.currentTimeMillis();
                try {
                    R result = function.apply(t);
                    long delay = System.currentTimeMillis() - startTimeMilli;
                    InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                    responseContext.setDuration(delay);
                    responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
                    responseContext.setResult(result);
                    invokeHandler.onSuccess(responseContext);
                    return result;
                } catch (Throwable e) {
                    long delay = System.currentTimeMillis() - startTimeMilli;
                    InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                    responseContext.setDuration(delay);
                    responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
                    responseContext.setError(e);
                    invokeHandler.onError(responseContext);
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
                invokeHandler.acquirePermission();
                long startTimeMilli = System.currentTimeMillis();
                try {
                    boolean result = predicate.test(t);
                    long delay = System.currentTimeMillis() - startTimeMilli;
                    InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                    responseContext.setDuration(delay);
                    responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
                    responseContext.setResult(result);
                    invokeHandler.onSuccess(responseContext);
                    return result;
                } catch (Throwable e) {
                    long delay = System.currentTimeMillis() - startTimeMilli;
                    InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
                    responseContext.setDuration(delay);
                    responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
                    responseContext.setError(e);
                    invokeHandler.onError(responseContext);
                    throw e;
                }
            }
        };
    }

}
