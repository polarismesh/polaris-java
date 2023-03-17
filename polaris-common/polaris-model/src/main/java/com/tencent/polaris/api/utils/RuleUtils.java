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


import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString.MatchStringType;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

public class RuleUtils {

    public static final String MATCH_ALL = "*";

    private static final Function<String, Pattern> DEFAULT_REGEX_PATTERN = new Function<String, Pattern>() {
        @Override
        public Pattern apply(String s) {
            return Pattern.compile(s);
        }
    };

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
        return StringUtils.equals(value, MATCH_ALL) || StringUtils.equals(value, "");
    }

    public static boolean matchStringValue(MatchString matchString, String actualValue,
            Function<String, Pattern> regexToPattern) {
        MatchStringType matchType = matchString.getType();
        String matchValue = matchString.getValue().getValue();
        return matchStringValue(matchType, actualValue, matchValue, regexToPattern);
    }

    private static boolean matchStringValue(MatchStringType matchType, String actualValue, String matchValue) {
        return matchStringValue(matchType, actualValue, matchValue, DEFAULT_REGEX_PATTERN);
    }

    private static boolean matchStringValue(MatchStringType matchType, String actualValue, String matchValue,
            Function<String, Pattern> regexToPattern) {
        actualValue = StringUtils.defaultString(actualValue);
        matchValue = StringUtils.defaultString(matchValue);
        if (RuleUtils.isMatchAllValue(matchValue)) {
            return true;
        }
        switch (matchType) {
            case EXACT: {
                return StringUtils.equals(actualValue, matchValue);
            }
            case REGEX: {
                //正则表达式匹配
                Pattern pattern = regexToPattern.apply(matchValue);
                return pattern.matcher(actualValue).find();
            }
            case NOT_EQUALS: {
                return !StringUtils.equals(actualValue, matchValue);
            }
            case IN: {
                String[] tokens = matchValue.split(",");
                for (String token : tokens) {
                    if (StringUtils.equals(token, actualValue)) {
                        return true;
                    }
                }
                return false;
            }
            case NOT_IN: {
                String[] tokens = matchValue.split(",");
                for (String token : tokens) {
                    if (StringUtils.equals(token, actualValue)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    // 匹配metadata
    public static boolean matchMetadata(Map<String, MatchString> ruleMeta, Map<String, String> destMeta) {
        return matchMetadata(ruleMeta, destMeta, false, Collections.emptyMap(), Collections.emptyMap());
    }

    // 匹配metadata
    public static boolean matchMetadata(Map<String, MatchString> ruleMeta, Map<String, String> destMeta,
            boolean isMatchSource, Map<String, String> multiEnvRouterParamMap, Map<String, String> variables) {
        // 如果规则metadata为空, 返回成功
        if (MapUtils.isEmpty(ruleMeta)) {
            return true;
        }
        if (ruleMeta.containsKey(RuleUtils.MATCH_ALL)) {
            return true;
        }
        // 如果规则metadata不为空, 待匹配规则为空, 直接返回失败
        if (MapUtils.isEmpty(destMeta)) {
            return false;
        }

        // metadata是否全部匹配
        boolean allMetaMatched = true;
        // dest中找到的metadata个数, 用于辅助判断是否能匹配成功
        int matchNum = 0;

        for (Map.Entry<String, MatchString> entry : ruleMeta.entrySet()) {
            String ruleMetaKey = entry.getKey();
            MatchString ruleMetaValue = entry.getValue();
            if (RuleUtils.isMatchAllValue(ruleMetaValue)) {
                matchNum++;
                continue;
            }
            if (destMeta.containsKey(ruleMetaKey)) {
                matchNum++;
                if (!ruleMetaValue.hasValue()
                        && ruleMetaValue.getValueType() != MatchString.ValueType.PARAMETER) {
                    continue;
                }

                String destMetaValue = destMeta.get(ruleMetaKey);

                allMetaMatched = isAllMetaMatched(isMatchSource, ruleMetaKey, ruleMetaValue, destMetaValue,
                        multiEnvRouterParamMap, variables);
            }

            if (!allMetaMatched) {
                break;
            }
        }

        // 如果一个metadata未找到, 匹配失败
        if (matchNum == 0) {
            allMetaMatched = false;
        }

        return allMetaMatched;
    }

    private static boolean isAllMetaMatched(boolean isMatchSource, String ruleMetaKey,
            MatchString ruleMetaValue, String destMetaValue, Map<String, String> multiEnvRouterParamMap,
            Map<String, String> variables) {
        if (RuleUtils.MATCH_ALL.equals(destMetaValue)) {
            return true;
        }
        return matchValueByValueType(isMatchSource, ruleMetaKey, ruleMetaValue, destMetaValue,
                multiEnvRouterParamMap, variables);
    }

    private static boolean matchValueByValueType(boolean isMatchSource, String ruleMetaKey,
            MatchString ruleMetaValue, String destMetaValue, Map<String, String> multiEnvRouterParamMap,
            Map<String, String> variables) {
        boolean allMetaMatched = true;

        switch (ruleMetaValue.getValueType()) {
            case PARAMETER:
                // 通过参数传入
                if (isMatchSource) {
                    // 当匹配的是source，记录请求的 K V
                    multiEnvRouterParamMap.put(ruleMetaKey, destMetaValue);
                } else {
                    // 当匹配的是dest， 判断value
                    if (!multiEnvRouterParamMap.containsKey(ruleMetaKey)) {
                        allMetaMatched = false;
                    } else {
                        String ruleValue = multiEnvRouterParamMap.get(ruleMetaKey);
                        // contains key
                        allMetaMatched = matchStringValue(ruleMetaValue.getType(), ruleValue, destMetaValue);
                    }
                }
                break;
            case VARIABLE:
                if (variables.containsKey(ruleMetaKey)) {
                    // 1.先从配置获取
                    String ruleValue = variables.get(ruleMetaKey);
                    allMetaMatched = matchStringValue(ruleMetaValue.getType(), destMetaValue, ruleValue);
                } else {
                    // 2.从环境变量中获取  key从规则中获取
                    String key = ruleMetaValue.getValue().getValue();
                    if (!System.getenv().containsKey(key)) {
                        allMetaMatched = false;
                    } else {
                        String value = System.getenv(key);
                        allMetaMatched = matchStringValue(ruleMetaValue.getType(), destMetaValue, value);
                    }
                    if (!System.getenv().containsKey(key) || !System.getenv(key).equals(destMetaValue)) {
                        allMetaMatched = false;
                    }
                }
                break;
            default:
                allMetaMatched = matchStringValue(ruleMetaValue.getType(), destMetaValue,
                        ruleMetaValue.getValue().getValue());
        }

        return allMetaMatched;
    }

}
