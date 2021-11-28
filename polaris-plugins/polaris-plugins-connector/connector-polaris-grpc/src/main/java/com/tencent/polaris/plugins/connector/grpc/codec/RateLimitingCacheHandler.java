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

package com.tencent.polaris.plugins.connector.grpc.codec;

import com.google.protobuf.StringValue;
import com.tencent.polaris.api.pojo.RegistryCacheValue;
import com.tencent.polaris.api.pojo.ServiceEventKey.EventType;
import com.tencent.polaris.client.pb.RateLimitProto.RateLimit;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.client.pb.ResponseProto.DiscoverResponse;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import java.util.ArrayList;
import java.util.List;

public class RateLimitingCacheHandler extends AbstractCacheHandler {

    @Override
    public EventType getTargetEventType() {
        return EventType.RATE_LIMITING;
    }

    @Override
    protected String getRevision(DiscoverResponse discoverResponse) {
        RateLimit rateLimit = discoverResponse.getRateLimit();
        if (null == rateLimit) {
            return "";
        }
        return rateLimit.getRevision().getValue();
    }

    @Override
    public RegistryCacheValue messageToCacheValue(RegistryCacheValue oldValue, Object newValue, boolean isCacheLoaded) {
        DiscoverResponse discoverResponse = (DiscoverResponse) newValue;
        RateLimit rateLimit = discoverResponse.getRateLimit();
        String revision = getRevision(discoverResponse);
        List<Rule> rulesList = rateLimit.getRulesList();
        //需要做一次排序,PB中的数据不可变，需要单独构建一份
        List<Rule> sortedRules = new ArrayList<>(rulesList);
        sortedRules.sort((o1, o2) -> {
            if (o1.getPriority().getValue() != o2.getPriority().getValue()) {
                return o1.getPriority().getValue() - o2.getPriority().getValue();
            }
            return o1.getId().getValue().compareTo(o2.getId().getValue());
        });
        RateLimit newRateLimit = RateLimit.newBuilder().addAllRules(sortedRules)
                .setRevision(StringValue.newBuilder().setValue(revision).build()).build();
        return new ServiceRuleByProto(newRateLimit, revision, isCacheLoaded, getTargetEventType());
    }
}
