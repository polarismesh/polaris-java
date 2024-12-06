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

package com.tencent.polaris.plugins.ratelimiter.reject.concurrency;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.ratelimiter.InitCriteria;
import com.tencent.polaris.api.plugin.ratelimiter.QuotaBucket;
import com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter;

/**
 * @author Haotian Zhang
 */
public class RejectConcurrencyRateLimiter implements ServiceRateLimiter {

    @Override
    public QuotaBucket initQuota(InitCriteria criteria) {
        return new ConcurrencyQuotaBucket(criteria);
    }

    @Override
    public String getName() {
        return ServiceRateLimiter.LIMITER_CONCURRENCY;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.SERVICE_LIMITER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
    }

    @Override
    public void postContextInit(Extensions extensions) throws PolarisException {
    }

    @Override
    public void destroy() {
    }
}
