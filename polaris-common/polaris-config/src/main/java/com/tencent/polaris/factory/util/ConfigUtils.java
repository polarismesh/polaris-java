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

package com.tencent.polaris.factory.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.polaris.api.utils.StringUtils;
import java.util.Map;

/**
 * Toolkits for configuration.
 *
 * @author Haotian Zhang
 */
public class ConfigUtils {

    public static Map<?, ?> objectToMap(Object obj) {
        if (obj == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(obj, Map.class);
    }

    public static void validateInterval(Long interval, String name) {
        if (null == interval || interval == 0) {
            throw new IllegalArgumentException(name + " must not be empty or 0");
        }
    }

    public static void validateTimes(Integer times, String name) {
        if (null == times || times < 0) {
            throw new IllegalArgumentException(name + " must not be empty or 0");
        }
    }

    public static void validateIntervalWithMin(Long interval, long minInterval, String name) {
        if (null == interval || interval == 0 || interval < minInterval) {
            throw new IllegalArgumentException(
                    name + " must not be empty or 0, and must be greater than " + minInterval);
        }
    }

    public static void validateString(String value, String name) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    public static void validateNull(Object value, String name) {
        if (null == value) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
    }

    /**
     * Validate if all Object being null.
     *
     * @param valueMap
     */
    public static void validateAllNull(Map<String, Object> valueMap) {
        StringBuilder nameListStr = null;
        int count = 0;
        for (String key : valueMap.keySet()) {
            if (null == valueMap.get(key)) {
                if (null == nameListStr) {
                    nameListStr = new StringBuilder(key);
                } else {
                    nameListStr.append(", ").append(key);
                }
                count++;
            }
        }
        if (count == valueMap.keySet().size()) {
            throw new IllegalArgumentException(nameListStr + " must not be all null.");
        }
    }

    public static void validatePositive(Integer value, String name) {
        if (null == value || value <= 0) {
            throw new IllegalArgumentException(name + " must not be positive");
        }
    }

    /**
     * Validate if value is true
     *
     * @param value
     * @param name
     */
    public static void validateTrue(Boolean value, String name) {
        if (null == value || !value) {
            throw new IllegalArgumentException(name + " must not be false");
        }
    }
}
