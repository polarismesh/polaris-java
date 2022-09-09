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


package com.tencent.polaris.plugins.router.rule;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.pb.ModelProto.MatchString;
import com.tencent.polaris.client.util.Utils;

/**
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public final class MatchFunctions {

    private static final String SPLIT_TOKEN = ",";

    /**
     * 根据 {@link MatchString.MatchStringType} 执行不同的规则计算任务
     *
     * @param matchType match 规则类型
     * @param destMetaValue 实际准备参与计算的数据
     * @param ruleValue 规则数据
     * @return {@link boolean} 匹配是否成功
     */
    public static boolean match(MatchString.MatchStringType matchType, String destMetaValue, String ruleValue) {
        switch (matchType) {
            case EXACT: {
                return StringUtils.equals(destMetaValue, ruleValue);
            }
            case REGEX: {
                //正则表达式匹配
                return Utils.regMatch(ruleValue, destMetaValue);
            }
            case NOT_EQUALS: {
                return !StringUtils.equals(destMetaValue, ruleValue);
            }
            case IN: {
                String[] tokens = ruleValue.split(SPLIT_TOKEN);
                for (String token : tokens) {
                    if (StringUtils.equals(token, destMetaValue)) {
                        return true;
                    }
                }
                return false;
            }
            case NOT_IN: {
                String[] tokens = ruleValue.split(SPLIT_TOKEN);
                for (String token : tokens) {
                    if (StringUtils.equals(token, destMetaValue)) {
                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }

}
