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

package com.tencent.polaris.ratelimit.test.core;

import com.google.protobuf.Duration;
import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.pojo.ServiceKey;
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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MultiRuleTest {

    private static final int PORT = 10092;

    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(PORT);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        ServiceKey serviceKey = new ServiceKey(Consts.NAMESPACE_TEST, Consts.MULTI_LIMIT_SERVICE);
        namingServer.getNamingService().addService(serviceKey);
        Builder rateLimitBuilder = RateLimit.newBuilder();
        Rule.Builder ruleBuilder1 = Rule.newBuilder();
        ruleBuilder1.setType(Type.LOCAL);
        ruleBuilder1.setPriority(UInt32Value.newBuilder().setValue(1).build());
        ruleBuilder1.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder1.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder1.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(20).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder1.setRevision(StringValue.newBuilder().setValue("11111").build());
        rateLimitBuilder.addRules(ruleBuilder1.build());

        Rule.Builder ruleBuilder2 = Rule.newBuilder();
        ruleBuilder2.setType(Type.LOCAL);
        ruleBuilder2.setPriority(UInt32Value.newBuilder().setValue(0).build());
        ruleBuilder2.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder2.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder2.setMethod(MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(
                StringValue.newBuilder().setValue(Consts.METHOD_CASH).build()).build());
        ruleBuilder2.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(15).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder2.setRevision(StringValue.newBuilder().setValue("22222").build());
        rateLimitBuilder.addRules(ruleBuilder2.build());

        Rule.Builder ruleBuilder3 = Rule.newBuilder();
        ruleBuilder3.setType(Type.LOCAL);
        ruleBuilder3.setPriority(UInt32Value.newBuilder().setValue(0).build());
        ruleBuilder3.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder3.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder3.setMethod(MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(
                StringValue.newBuilder().setValue(Consts.METHOD_PAY).build()).build());
        ruleBuilder3.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(15).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder3.setRevision(StringValue.newBuilder().setValue("33333").build());
        rateLimitBuilder.addRules(ruleBuilder3.build());

        Rule.Builder ruleBuilder4 = Rule.newBuilder();
        ruleBuilder4.setType(Type.LOCAL);
        ruleBuilder4.setPriority(UInt32Value.newBuilder().setValue(0).build());
        ruleBuilder4.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder4.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder4.setMethod(MatchString.newBuilder().setType(MatchStringType.EXACT).setValue(
                StringValue.newBuilder().setValue(Consts.METHOD_PAY).build()).build());
        ruleBuilder4.addArguments(
                MatchArgument.newBuilder().setType(MatchArgument.Type.HEADER).setKey(Consts.HEADER_KEY).setValue(
                        MatchString.newBuilder().setValue(StringValue.newBuilder().setValue(Consts.HEADER_VALUE))
                                .setType(MatchStringType.EXACT).build()).build());
        ruleBuilder4.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(30).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder4.setRevision(StringValue.newBuilder().setValue("44444").build());
        rateLimitBuilder.addRules(ruleBuilder4.build());
        rateLimitBuilder.setRevision(StringValue.newBuilder().setValue("xxxxxxx").build());
        namingServer.getNamingService().setRateLimit(serviceKey, rateLimitBuilder.build());
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    private QuotaResponse quotaAcquire(LimitAPI limitAPI, String method, String headerValue) {
        QuotaRequest payRequest = new QuotaRequest();
        payRequest.setNamespace(Consts.NAMESPACE_TEST);
        payRequest.setService(Consts.MULTI_LIMIT_SERVICE);
        if (null != method) {
            payRequest.setMethod(method);
        }
        if (null != headerValue) {
            Set<Argument> matchArguments = new HashSet<>();
            matchArguments.add(Argument
                    .buildHeader(Consts.HEADER_KEY, headerValue));
            payRequest.setArguments(matchArguments);
        }
        return limitAPI.getQuota(payRequest);
    }

    @Test
    public void testGetQuotaLayer2() {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();
            // first query header
            boolean hasLimited = false;
            boolean hasPassed = false;
            for (int i = 0; i < 16; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, Consts.METHOD_PAY, Consts.HEADER_VALUE);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertTrue(hasLimited);
            Assert.assertTrue(hasPassed);
            System.out.println("start to wait expired");
            Utils.sleepUninterrupted(5 * 1000);
        }
    }

    @Test
    public void testGetQuotaLayer3() {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();
            // first query header
            boolean hasLimited = false;
            boolean hasPassed = false;
            for (int i = 0; i < 13; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, Consts.METHOD_PAY, Consts.HEADER_VALUE);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertFalse(hasLimited);
            Assert.assertTrue(hasPassed);

            hasLimited = false;
            hasPassed = false;
            for (int i = 0; i < 10; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, Consts.METHOD_CASH, null);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertTrue(hasLimited);
            Assert.assertTrue(hasPassed);
            System.out.println("start to wait expired");
            Utils.sleepUninterrupted(5 * 1000);
        }
    }

}
