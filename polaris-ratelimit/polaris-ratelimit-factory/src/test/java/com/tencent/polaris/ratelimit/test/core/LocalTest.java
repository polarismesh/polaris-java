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

package com.tencent.polaris.ratelimit.test.core;

import com.google.protobuf.Duration;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString.MatchStringType;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Amount;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.MatchArgument;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.RateLimit;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.RateLimit.Builder;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule.AmountMode;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto.Rule.Type;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LocalTest {

    private static final int PORT = 10091;

    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(PORT);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, Consts.LOCAL_LIMIT_SERVICE);
        namingServer.getNamingService().addService(serviceKey);
        Builder rateLimitBuilder = RateLimit.newBuilder();

        Rule.Builder ruleBuilder2 = Rule.newBuilder();
        ruleBuilder2.setType(Type.LOCAL);
        ruleBuilder2.setPriority(UInt32Value.newBuilder().setValue(0).build());
        ruleBuilder2.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder2.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder2.addArguments(
                MatchArgument.newBuilder().setType(MatchArgument.Type.CUSTOM).setKey(Consts.LABEL_METHOD)
                        .setValue(MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(
                                StringValue.newBuilder().setValue(Consts.METHOD_CASH).build()).build()));
        ruleBuilder2.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(19).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder2.setRevision(StringValue.newBuilder().setValue("22222").build());
        rateLimitBuilder.addRules(ruleBuilder2.build());

        Rule.Builder ruleBuilder3 = Rule.newBuilder();
        ruleBuilder3.setType(Type.LOCAL);
        ruleBuilder3.setPriority(UInt32Value.newBuilder().setValue(0).build());
        ruleBuilder3.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder3.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder3.addArguments(
                MatchArgument.newBuilder().setType(MatchArgument.Type.CUSTOM).setKey(Consts.LABEL_METHOD)
                        .setValue(MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(
                                StringValue.newBuilder().setValue(Consts.METHOD_PAY).build()).build()));
        ruleBuilder3.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(9).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder3.setRevision(StringValue.newBuilder().setValue("33333").build());
        rateLimitBuilder.addRules(ruleBuilder3.build());
        rateLimitBuilder.setRevision(StringValue.newBuilder().setValue("xxxxxxx").build());
        namingServer.getNamingService().setRateLimit(serviceKey, rateLimitBuilder.build());
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    private static void testQuotaAcquire(LimitAPI limitAPI, Map<String, String> labels, int maxCount) {
        QuotaRequest payRequest = new QuotaRequest();
        payRequest.setNamespace(Consts.NAMESPACE_TEST);
        payRequest.setService(Consts.LOCAL_LIMIT_SERVICE);
        Set<Argument> matchArgumentSet = new HashSet<>();
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            Argument matchArgument = Argument
                    .buildCustom(entry.getKey(), entry.getValue());
            matchArgumentSet.add(matchArgument);
        }
        if (!CollectionUtils.isEmpty(matchArgumentSet)) {
            payRequest.setArguments(matchArgumentSet);
        }
        boolean payLimit = false;
        boolean payPass = false;
        for (int i = 0; i < maxCount; i++) {
            QuotaResponse response = limitAPI.getQuota(payRequest);
            if (response.getCode() == QuotaResultCode.QuotaResultOk) {
                payPass = true;
            } else if (response.getCode() == QuotaResultCode.QuotaResultLimited) {
                payLimit = true;
            }
        }
        Assert.assertTrue(payPass);
        Assert.assertTrue(payLimit);
    }

    @Test
    public void testSingleThreadLimit() {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();
            testQuotaAcquire(limitAPI,
                    Consts.createSingleValueMap(new String[]{Consts.LABEL_METHOD}, new String[]{Consts.METHOD_PAY}),
                    Consts.MAX_PAY_COUNT);
            testQuotaAcquire(limitAPI,
                    Consts.createSingleValueMap(new String[]{Consts.LABEL_METHOD}, new String[]{Consts.METHOD_CASH}),
                    Consts.MAX_CASH_COUNT);
            System.out.println("start to wait expired");
            Utils.sleepUninterrupted(5 * 1000);
        }
    }
}
