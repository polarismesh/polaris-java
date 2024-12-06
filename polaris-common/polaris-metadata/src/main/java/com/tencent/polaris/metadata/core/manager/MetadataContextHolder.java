/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.metadata.core.manager;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MetadataContextHolder {

    private static final ThreadLocal<MetadataContext> THREAD_LOCAL_CONTEXT = new InheritableThreadLocal<>();

    private static Supplier<MetadataContext> initializer;

    public static MetadataContext getOrCreate() {
        MetadataContext metadataContext = THREAD_LOCAL_CONTEXT.get();
        if (null != metadataContext) {
            return metadataContext;
        }
        if (null != initializer) {
            metadataContext = initializer.get();
        } else {
            metadataContext = new MetadataContext();
        }
        THREAD_LOCAL_CONTEXT.set(metadataContext);
        return metadataContext;
    }

    public static void refresh(Consumer<MetadataContext> consumer) {
        remove();
        MetadataContext metadataManager = getOrCreate();
        if (null != consumer) {
            consumer.accept(metadataManager);
        }
    }

    public static void remove() {
        THREAD_LOCAL_CONTEXT.remove();
    }

    public static void set(MetadataContext metadataManager) {
        THREAD_LOCAL_CONTEXT.set(metadataManager);
    }

    public static void setInitializer(Supplier<MetadataContext> initializer) {
        MetadataContextHolder.initializer = initializer;
    }

}