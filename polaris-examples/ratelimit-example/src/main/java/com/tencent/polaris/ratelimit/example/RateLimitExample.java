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

package com.tencent.polaris.ratelimit.example;

import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.example.utils.LimitExampleUtils;
import com.tencent.polaris.ratelimit.example.utils.LimitExampleUtils.InitResult;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RateLimitExample {

    public static void main(String[] args) throws Exception {
        InitResult initResult = LimitExampleUtils.initRateLimitConfiguration(args);
        String namespace = initResult.getNamespace();
        String service = initResult.getService();
        int concurrency = initResult.getConcurrency();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(concurrency);
        //注意：使用本地限流时，限流阈值计数器会存放在LimitAPI实例内部，无法跨实例共享，因此LimitAPI建议通过进程单例模式使用
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPI()) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    QuotaRequest quotaRequest = new QuotaRequest();
                    quotaRequest.setNamespace(namespace);
                    quotaRequest.setService(service);
                    quotaRequest.setMethod("echo");
                    quotaRequest.setCount(1);
                    QuotaResponse quotaResponse = limitAPI.getQuota(quotaRequest);
                    System.out.println("quotaResponse is " + quotaResponse.getCode());
                }
            };
            List<ScheduledFuture<?>> futures = new ArrayList<>();
            for (int i = 0; i < concurrency; i++) {
                ScheduledFuture<?> scheduledFuture = executorService
                        .scheduleWithFixedDelay(runnable, 10 + i, 500, TimeUnit.MILLISECONDS);
                futures.add(scheduledFuture);
            }
            Thread.sleep(500000);
            for (ScheduledFuture<?> future : futures) {
                future.cancel(true);
            }
        }
        executorService.shutdown();
    }
}
