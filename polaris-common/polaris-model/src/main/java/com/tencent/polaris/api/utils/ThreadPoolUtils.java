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

package com.tencent.polaris.api.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadPoolUtils {

    /**
     * 等待所有线程池结束
     *
     * @param services 线程池
     */
    public static void waitAndStopThreadPools(ExecutorService[] services) {
        for (ExecutorService service : services) {
            if (null == service) {
                continue;
            }
            service.shutdown();
        }
        for (ExecutorService service : services) {
            if (null == service) {
                continue;
            }
            try {
                service.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
