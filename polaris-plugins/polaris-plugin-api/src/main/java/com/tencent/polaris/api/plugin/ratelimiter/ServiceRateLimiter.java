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

package com.tencent.polaris.api.plugin.ratelimiter;

import com.tencent.polaris.api.plugin.Plugin;

/**
 * Interface of rate-limiter.
 *
 * @author Haotian Zhang
 */
public interface ServiceRateLimiter extends Plugin {

    String LIMITER_REJECT = "reject";

    /**
     * 匀速排队
     */
    String LIMITER_UNIRATE = "UNIRATE";

    /**
     * 并发数限流
     */
    String LIMITER_CONCURRENCY = "CONCURRENCY";

    /**
     * TSF限流
     */
    String LIMITER_TSF = "TSF";

    /**
     * 初始化并创建令牌桶/漏桶, 主流程会在首次调用，以及规则对象变更的时候，调用该方法
     *
     * @param criteria 参数对象
     * @return 限流令牌桶
     */
    QuotaBucket initQuota(InitCriteria criteria);
}
