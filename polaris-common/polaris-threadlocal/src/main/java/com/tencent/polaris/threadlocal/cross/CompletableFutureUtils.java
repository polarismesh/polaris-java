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


import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utils for CompletableFuture.
 *
 * @author Haotian Zhang
 */
public class CompletableFutureUtils {

    public static <U, T> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Supplier<T> contextGetter,
                                                          Consumer<T> contextSetter) {
        Supplier<U> polarisSupplier = new SupplierWrapper<>(supplier, contextGetter, contextSetter);
        return CompletableFuture.supplyAsync(polarisSupplier);
    }

    public static <T> CompletableFuture<Void> runAsync(Runnable runnable, Supplier<T> contextGetter,
                                                       Consumer<T> contextSetter) {
        Runnable polarisRunnable = new RunnableWrapper<>(runnable, contextGetter, contextSetter);
        return CompletableFuture.runAsync(polarisRunnable);
    }
}
