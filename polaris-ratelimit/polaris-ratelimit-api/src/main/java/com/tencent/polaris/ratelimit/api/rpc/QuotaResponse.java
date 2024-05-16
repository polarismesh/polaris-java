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

package com.tencent.polaris.ratelimit.api.rpc;

import com.tencent.polaris.api.plugin.ratelimiter.QuotaResult;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;

public class QuotaResponse {

    private final QuotaResult quotaResult;

    private RateLimitProto.Rule activeRule;

    public QuotaResponse(QuotaResult quotaResult) {
        this.quotaResult = quotaResult;
    }

    public QuotaResultCode getCode() {
        return QuotaResultCode.values()[quotaResult.getCode().ordinal()];
    }

    public long getWaitMs() {
        return quotaResult.getWaitMs();
    }

    public String getInfo() {
        return quotaResult.getInfo();
    }

    public void setActiveRule(Rule activeRule) {
        this.activeRule = activeRule;
    }

    public String getActiveRuleName() {
        String activeRuleName = this.getActiveRule().getName().getValue();

        return activeRuleName;
    }

    public Rule getActiveRule() {
        return activeRule;
    }
}
