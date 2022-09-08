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

package com.tencent.polaris.api.utils;

import com.tencent.polaris.client.pb.ModelProto.MatchString;

public class RuleUtils {

    public static final String MATCH_ALL = "*";

    /**
     * 是否全匹配的规则
     *
     * @param ruleMetaValue 规则匹配条件
     * @return 是否全匹配，全匹配则忽略该规则
     */
    public static boolean isMatchAllValue(MatchString ruleMetaValue) {
        return isMatchAllValue(ruleMetaValue.getValue().getValue());
    }

    /**
     * 是否全匹配的规则
     *
     * @param value 规则匹配键
     * @return 是否全匹配，全匹配则忽略该规则
     */
    public static boolean isMatchAllValue(String value) {
        return StringUtils.isEmpty(value) || StringUtils.equals(value, MATCH_ALL);
    }
}
