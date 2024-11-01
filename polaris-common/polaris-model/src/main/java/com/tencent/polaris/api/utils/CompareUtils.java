/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
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

/**
 * Utils for comparing something.
 *
 * @author Haotian Zhang
 */
public class CompareUtils {

    public static boolean isWildcardMatcherSingle(String name) {
        return StringUtils.equalsIgnoreCase(name, RuleUtils.MATCH_ALL) || StringUtils.isBlank(name);
    }

    public static int compareSingleValue(String value1, String value2) {
        boolean serviceWildcard1 = isWildcardMatcherSingle(value1);
        boolean serviceWildcard2 = isWildcardMatcherSingle(value2);
        if (serviceWildcard1 && serviceWildcard2) {
            return 0;
        }
        if (serviceWildcard1) {
            // 2 before 1
            return 1;
        }
        if (serviceWildcard2) {
            // 1 before 2
            return -1;
        }
        return value1.compareTo(value2);
    }

    public static int compareService(String namespace1, String service1, String namespace2, String service2) {
        int nsResult = CompareUtils.compareSingleValue(namespace1, namespace2);
        if (nsResult != 0) {
            return nsResult;
        }
        return CompareUtils.compareSingleValue(service1, service2);
    }

    /**
     * compare two boolean.
     */
    public static int compareBoolean(boolean b1, boolean b2) {
        if (b1 == b2) {
            return 0;
        }
        return b1 ? -1 : 1;
    }
}
