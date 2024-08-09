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

package com.tencent.polaris.plugins.circuitbreaker.composite.utils;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.MethodResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.utils.RuleUtils;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;

import java.util.function.Function;
import java.util.regex.Pattern;

public class MatchUtils {

    public static boolean matchMethod(Resource resource, MatchString matchString,
                                      Function<String, Pattern> regexToPattern) {
        if (resource.getLevel() != Level.METHOD) {
            return true;
        }
        String method = ((MethodResource) resource).getMethod();
        return RuleUtils.matchStringValue(matchString, method, regexToPattern);
    }
}
