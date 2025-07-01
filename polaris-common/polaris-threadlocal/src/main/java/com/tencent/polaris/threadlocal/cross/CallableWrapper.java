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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CallableWrapper<U, T> implements Callable<U> {

    private final Callable<U> callable;

    private final AtomicReference<T> contextRef;

    private final Supplier<T> contextGetter;

    private final Consumer<T> contextSetter;

    public CallableWrapper(Callable<U> callable, Supplier<T> contextGetter, Consumer<T> contextSetter) {
        assert null != callable && null != contextGetter && null != contextSetter;
        this.callable = callable;
        this.contextGetter = contextGetter;
        this.contextSetter = contextSetter;
        contextRef = new AtomicReference<>(contextGetter.get());
    }

    @Override
    public U call() throws Exception {
        // preserve
        T latestContext = contextGetter.get();
        // set context
        T contextValue = contextRef.get();
        contextSetter.accept(contextValue);
        try {
            return callable.call();
        } finally {
            // restore
            contextSetter.accept(latestContext);
        }
    }
}
