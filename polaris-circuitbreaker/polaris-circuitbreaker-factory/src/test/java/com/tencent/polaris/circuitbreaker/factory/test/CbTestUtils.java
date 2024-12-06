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

package com.tencent.polaris.circuitbreaker.factory.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.google.protobuf.util.JsonFormat;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.FaultDetectorProto;
import org.junit.Assert;

public class CbTestUtils {

    public static CircuitBreakerProto.CircuitBreakerRule loadCbRule(String fileName) throws IOException {
        CircuitBreakerProto.CircuitBreakerRule.Builder circuitBreakerRuleBuilder = CircuitBreakerProto.CircuitBreakerRule
                .newBuilder();
        InputStream inputStream = CbTestUtils.class.getClassLoader().getResourceAsStream(fileName);
        Assert.assertNotNull(inputStream);
        String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining(""));
        JsonFormat.parser().ignoringUnknownFields().merge(json, circuitBreakerRuleBuilder);
        return circuitBreakerRuleBuilder.build();
    }

    public static FaultDetectorProto.FaultDetectRule loadFdRule(String fileName) throws IOException {
        FaultDetectorProto.FaultDetectRule.Builder builder = FaultDetectorProto.FaultDetectRule.newBuilder();
        InputStream inputStream = CbTestUtils.class.getClassLoader().getResourceAsStream(fileName);
        Assert.assertNotNull(inputStream);
        String json = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining(""));
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        return builder.build();
    }
}
