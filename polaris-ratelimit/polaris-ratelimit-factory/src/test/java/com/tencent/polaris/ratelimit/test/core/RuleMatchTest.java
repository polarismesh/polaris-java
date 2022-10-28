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
import com.tencent.polaris.client.pb.ModelProto.MatchArgument;
import com.tencent.polaris.client.pb.ModelProto.MatchString;
import com.tencent.polaris.client.pb.ModelProto.Operation;
import com.tencent.polaris.client.pb.RateLimitProto.Amount;
import com.tencent.polaris.client.pb.RateLimitProto.RateLimit;
import com.tencent.polaris.client.pb.RateLimitProto.RateLimit.Builder;
import com.tencent.polaris.client.pb.RateLimitProto.Rule;
import com.tencent.polaris.client.pb.RateLimitProto.Rule.AmountMode;
import com.tencent.polaris.client.pb.RateLimitProto.Rule.Type;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.Argument;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode;
import com.tencent.polaris.ratelimit.factory.LimitAPIFactory;
import com.tencent.polaris.test.common.TestUtils;
import com.tencent.polaris.test.mock.discovery.NamingServer;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RuleMatchTest {

    private static final int PORT = 10093;

    private static final String MATCH_REGEX_SERVICE = "match_regex_service";

    private static final String MATCH_NOT_EQUALS_SERVICE = "match_not_equals_service";

    private static final String MATCH_IN_SERVICE = "match_in_service";

    private static final String MATCH_NOT_IN_SERVICE = "match_not_in_service";

    private NamingServer namingServer;

    @Before
    public void before() {
        try {
            namingServer = NamingServer.startNamingServer(PORT);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        ServiceKey serviceKeyRegex = new ServiceKey(Consts.NAMESPACE_TEST, MATCH_REGEX_SERVICE);
        namingServer.getNamingService().addService(serviceKeyRegex);
        Builder rateLimitBuilder = RateLimit.newBuilder();
        Rule.Builder ruleBuilder1 = Rule.newBuilder();
        ruleBuilder1.setType(Type.LOCAL);
        ruleBuilder1.setPriority(UInt32Value.newBuilder().setValue(1).build());
        ruleBuilder1.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder1.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder1.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(1).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder1.setMethod(MatchString.newBuilder().setType(Operation.REGEX).setValue(
                StringValue.newBuilder().setValue("^ca.+$").build()).build());
        ruleBuilder1.setRevision(StringValue.newBuilder().setValue("11111").build());
        rateLimitBuilder.addRules(ruleBuilder1.build());
        rateLimitBuilder.setRevision(StringValue.newBuilder().setValue("xxxxxxx").build());
        namingServer.getNamingService().setRateLimit(serviceKeyRegex, rateLimitBuilder.build());

        ServiceKey serviceKeyNotEquals = new ServiceKey(Consts.NAMESPACE_TEST, MATCH_NOT_EQUALS_SERVICE);
        namingServer.getNamingService().addService(serviceKeyNotEquals);
        rateLimitBuilder = RateLimit.newBuilder();
        ruleBuilder1 = Rule.newBuilder();
        ruleBuilder1.setType(Type.LOCAL);
        ruleBuilder1.setPriority(UInt32Value.newBuilder().setValue(1).build());
        ruleBuilder1.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder1.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder1.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(1).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder1.setMethod(MatchString.newBuilder().setType(Operation.NOT_EQUALS).setValue(
                StringValue.newBuilder().setValue("cash").build()).build());
        ruleBuilder1.setRevision(StringValue.newBuilder().setValue("22222").build());
        rateLimitBuilder.addRules(ruleBuilder1.build());
        rateLimitBuilder.setRevision(StringValue.newBuilder().setValue("yyyyyy").build());
        namingServer.getNamingService().setRateLimit(serviceKeyNotEquals, rateLimitBuilder.build());

        ServiceKey serviceKeyIn = new ServiceKey(Consts.NAMESPACE_TEST, MATCH_IN_SERVICE);
        namingServer.getNamingService().addService(serviceKeyIn);
        rateLimitBuilder = RateLimit.newBuilder();
        ruleBuilder1 = Rule.newBuilder();
        ruleBuilder1.setType(Type.LOCAL);
        ruleBuilder1.setPriority(UInt32Value.newBuilder().setValue(1).build());
        ruleBuilder1.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder1.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder1.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(1).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder1.addArguments(
                MatchArgument.newBuilder().setType(MatchArgument.Type.HEADER).setKey(Consts.HEADER_KEY)
                        .setValue(MatchString.newBuilder().setType(Operation.IN).setValue(
                                StringValue.newBuilder().setValue("pay,pay1").build()).build()));
        ruleBuilder1.setRevision(StringValue.newBuilder().setValue("33333").build());
        rateLimitBuilder.addRules(ruleBuilder1.build());
        rateLimitBuilder.setRevision(StringValue.newBuilder().setValue("zzzzzz").build());
        namingServer.getNamingService().setRateLimit(serviceKeyIn, rateLimitBuilder.build());

        ServiceKey serviceKeyNotIn = new ServiceKey(Consts.NAMESPACE_TEST, MATCH_NOT_IN_SERVICE);
        namingServer.getNamingService().addService(serviceKeyNotIn);
        rateLimitBuilder = RateLimit.newBuilder();
        ruleBuilder1 = Rule.newBuilder();
        ruleBuilder1.setType(Type.LOCAL);
        ruleBuilder1.setPriority(UInt32Value.newBuilder().setValue(1).build());
        ruleBuilder1.setAction(StringValue.newBuilder().setValue("reject").build());
        ruleBuilder1.setAmountMode(AmountMode.GLOBAL_TOTAL);
        ruleBuilder1.addAmounts(
                Amount.newBuilder().setMaxAmount(UInt32Value.newBuilder().setValue(1).build()).setValidDuration(
                        Duration.newBuilder().setSeconds(1).build()));
        ruleBuilder1.addArguments(
                MatchArgument.newBuilder().setType(MatchArgument.Type.HEADER).setKey(Consts.HEADER_KEY)
                        .setValue(MatchString.newBuilder().setType(Operation.NOT_IN).setValue(
                                StringValue.newBuilder().setValue("pay,pay1").build()).build()));
        ruleBuilder1.setRevision(StringValue.newBuilder().setValue("44444").build());
        rateLimitBuilder.addRules(ruleBuilder1.build());
        rateLimitBuilder.setRevision(StringValue.newBuilder().setValue("aaaaaa").build());
        namingServer.getNamingService().setRateLimit(serviceKeyNotIn, rateLimitBuilder.build());
    }

    @After
    public void after() {
        if (null != namingServer) {
            namingServer.terminate();
        }
    }

    private QuotaResponse quotaAcquire(LimitAPI limitAPI, String service, String method, String headerValue) {
        QuotaRequest payRequest = new QuotaRequest();
        payRequest.setNamespace(Consts.NAMESPACE_TEST);
        payRequest.setService(service);
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
    public void testGetQuotaRegex() {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();
            // first query header
            boolean hasLimited = false;
            boolean hasPassed = false;
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_REGEX_SERVICE, Consts.METHOD_CASH, null);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertTrue(hasLimited);
            Assert.assertTrue(hasPassed);
            hasLimited = false;
            hasPassed = false;
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_REGEX_SERVICE, Consts.METHOD_PAY, null);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertFalse(hasLimited);
            Assert.assertTrue(hasPassed);
            System.out.println("start to wait expired");
            Utils.sleepUninterrupted(5 * 1000);
        }
    }

    @Test
    public void testGetQuotaNotEqual() {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();
            // first query header
            boolean hasLimited = false;
            boolean hasPassed = false;
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_NOT_EQUALS_SERVICE, Consts.METHOD_PAY,
                        null);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertTrue(hasLimited);
            Assert.assertTrue(hasPassed);
            hasLimited = false;
            hasPassed = false;
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_NOT_EQUALS_SERVICE, Consts.METHOD_CASH,
                        null);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertFalse(hasLimited);
            Assert.assertTrue(hasPassed);
            System.out.println("start to wait expired");
            Utils.sleepUninterrupted(5 * 1000);
        }
    }

    @Test
    public void testGetQuotaIn() {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();
            // first query header
            boolean hasLimited = false;
            boolean hasPassed = false;
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_IN_SERVICE, null, Consts.METHOD_PAY);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertTrue(hasLimited);
            Assert.assertTrue(hasPassed);
            hasLimited = false;
            hasPassed = false;
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_IN_SERVICE, null, "pay1");
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertTrue(hasLimited);
            Assert.assertTrue(hasPassed);
            hasLimited = false;
            hasPassed = false;
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_IN_SERVICE, null, Consts.METHOD_CASH);
                QuotaResultCode code = quotaResponse.getCode();
                if (code == QuotaResultCode.QuotaResultLimited) {
                    hasLimited = true;
                } else if (code == QuotaResultCode.QuotaResultOk) {
                    hasPassed = true;
                }
            }
            Assert.assertFalse(hasLimited);
            Assert.assertTrue(hasPassed);
            System.out.println("start to wait expired");
            Utils.sleepUninterrupted(5 * 1000);
        }
    }

    @Test
    public void testGetQuotaNotIn() {
        Configuration configuration = TestUtils.createSimpleConfiguration(PORT);
        try (LimitAPI limitAPI = LimitAPIFactory.createLimitAPIByConfig(configuration)) {
            RateLimitUtils.adjustTime();
            // first query header
            boolean hasLimited = false;
            boolean hasPassed = false;
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_NOT_IN_SERVICE, null, Consts.METHOD_PAY);
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
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_NOT_IN_SERVICE, null, "pay1");
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
            for (int i = 0; i < 5; i++) {
                QuotaResponse quotaResponse = quotaAcquire(limitAPI, MATCH_NOT_IN_SERVICE, null, Consts.METHOD_CASH);
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
