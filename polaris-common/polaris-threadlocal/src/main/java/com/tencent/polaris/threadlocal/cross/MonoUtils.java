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

package com.tencent.polaris.threadlocal.cross;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utils for Mono with ThreadLocal context propagation.
 * Requires {@code io.projectreactor:reactor-core} on the classpath at runtime.
 *
 * @author Haotian Zhang
 */
public class MonoUtils {

    private static final boolean REACTOR_PRESENT;

    private static final String REACTOR_ABSENT_MSG =
            "reactor-core is not on the classpath. Please add io.projectreactor:reactor-core as a dependency.";

    static {
        boolean present;
        try {
            Class.forName("reactor.core.publisher.Mono");
            present = true;
        } catch (ClassNotFoundException ex) {
            present = false;
        }
        REACTOR_PRESENT = present;
    }

    private static void checkReactorPresent() {
        if (!REACTOR_PRESENT) {
            throw new IllegalStateException(REACTOR_ABSENT_MSG);
        }
    }

    /**
     * Create a {@link reactor.core.publisher.Mono} that propagates the current ThreadLocal context into the async
     * execution of the given {@link Supplier}.
     * Requires reactor-core on the classpath.
     *
     * @param supplier      the supplier to execute
     * @param contextGetter getter for the ThreadLocal value to propagate
     * @param contextSetter setter for the ThreadLocal value in the async thread
     * @param <U>           the result type
     * @param <T>           the context type
     * @return a {@link reactor.core.publisher.Mono} that emits the value returned by the supplier
     * @throws IllegalStateException if reactor-core is not on the classpath
     */
    public static <U, T> reactor.core.publisher.Mono<U> fromSupplier(Supplier<U> supplier,
                                                                      Supplier<T> contextGetter,
                                                                      Consumer<T> contextSetter) {
        checkReactorPresent();
        Supplier<U> wrappedSupplier = new SupplierWrapper<>(supplier, contextGetter, contextSetter);
        return reactor.core.publisher.Mono.fromSupplier(wrappedSupplier);
    }

    /**
     * Create a {@link reactor.core.publisher.Mono} that propagates the current ThreadLocal context into the async
     * execution of the given {@link Callable}.
     * Requires reactor-core on the classpath.
     *
     * @param callable      the callable to execute
     * @param contextGetter getter for the ThreadLocal value to propagate
     * @param contextSetter setter for the ThreadLocal value in the async thread
     * @param <U>           the result type
     * @param <T>           the context type
     * @return a {@link reactor.core.publisher.Mono} that emits the value returned by the callable
     * @throws IllegalStateException if reactor-core is not on the classpath
     */
    public static <U, T> reactor.core.publisher.Mono<U> fromCallable(Callable<U> callable,
                                                                      Supplier<T> contextGetter,
                                                                      Consumer<T> contextSetter) {
        checkReactorPresent();
        Callable<U> wrappedCallable = new CallableWrapper<>(callable, contextGetter, contextSetter);
        return reactor.core.publisher.Mono.fromCallable(wrappedCallable);
    }

    /**
     * Create a {@link reactor.core.publisher.Mono} that propagates the current ThreadLocal context into the async
     * execution of the given {@link Runnable}.
     * Requires reactor-core on the classpath.
     *
     * @param runnable      the runnable to execute
     * @param contextGetter getter for the ThreadLocal value to propagate
     * @param contextSetter setter for the ThreadLocal value in the async thread
     * @param <T>           the context type
     * @return a {@link reactor.core.publisher.Mono} that completes empty after the runnable finishes
     * @throws IllegalStateException if reactor-core is not on the classpath
     */
    public static <T> reactor.core.publisher.Mono<Void> fromRunnable(Runnable runnable,
                                                                      Supplier<T> contextGetter,
                                                                      Consumer<T> contextSetter) {
        checkReactorPresent();
        Runnable wrappedRunnable = new RunnableWrapper<>(runnable, contextGetter, contextSetter);
        return reactor.core.publisher.Mono.fromRunnable(wrappedRunnable);
    }

    /**
     * Create a {@link reactor.core.publisher.Mono} that propagates the current ThreadLocal context into the async
     * execution of the given supplier via a {@link CompletableFuture}.
     * Requires reactor-core on the classpath.
     *
     * @param supplier      the supplier to execute asynchronously with context propagation
     * @param contextGetter getter for the ThreadLocal value to propagate
     * @param contextSetter setter for the ThreadLocal value in the async thread
     * @param <U>           the result type
     * @param <T>           the context type
     * @return a {@link reactor.core.publisher.Mono} that emits the value returned by the supplier
     * @throws IllegalStateException if reactor-core is not on the classpath
     */
    public static <U, T> reactor.core.publisher.Mono<U> fromFuture(Supplier<U> supplier,
                                                                    Supplier<T> contextGetter,
                                                                    Consumer<T> contextSetter) {
        checkReactorPresent();
        CompletableFuture<U> future = CompletableFutureUtils.supplyAsync(supplier, contextGetter, contextSetter);
        return reactor.core.publisher.Mono.fromFuture(future);
    }
}
