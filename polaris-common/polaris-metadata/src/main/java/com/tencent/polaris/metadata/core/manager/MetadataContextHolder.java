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

package com.tencent.polaris.metadata.core.manager;

import java.util.function.Supplier;

public class MetadataContextHolder {

    private static final ThreadLocal<MetadataContext> THREAD_LOCAL_CONTEXT = new InheritableThreadLocal<>();

    public static MetadataContext get(Supplier<MetadataContext> initialize) {
        MetadataContext metadataContext = THREAD_LOCAL_CONTEXT.get();
        if (null != metadataContext) {
            return metadataContext;
        }
        synchronized (MetadataContextHolder.class) {
            metadataContext = THREAD_LOCAL_CONTEXT.get();
            if (null != metadataContext) {
                return metadataContext;
            }
            if (null != initialize) {
                metadataContext = initialize.get();
            } else {
                metadataContext = new MetadataContext();
            }
            THREAD_LOCAL_CONTEXT.set(metadataContext);
            return metadataContext;
        }
    }

    public static MetadataContext get() {
        return get(null);
    }
}
