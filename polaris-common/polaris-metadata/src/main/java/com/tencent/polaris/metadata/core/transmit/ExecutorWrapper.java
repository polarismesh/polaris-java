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

package com.tencent.polaris.metadata.core.transmit;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tencent.polaris.metadata.core.manager.MetadataContext;
import com.tencent.polaris.metadata.core.manager.MetadataContextHolder;

public class ExecutorWrapper<T> implements Executor {

    private final Executor executor;

    private final Supplier<T> contextGetter;

    private final Consumer<T> contextSetter;

    public ExecutorWrapper(Executor executor, Supplier<T> contextGetter, Consumer<T> contextSetter) {
        this.executor = executor;
        this.contextGetter = contextGetter;
        this.contextSetter = contextSetter;
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(new RunnableWrapper<>(command, contextGetter, contextSetter));
    }

    public static ExecutorWrapper<MetadataContext> buildDefault(Executor executor) {
        return new ExecutorWrapper<>(executor, MetadataContextHolder::getOrCreate, MetadataContextHolder::set);
    }
}
