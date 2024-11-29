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

import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * Utils for class.
 *
 * @author Haotian Zhang
 */
public class ClassUtils {

    private static final Logger LOG = LoggerFactory.getLogger(RuleUtils.class);

    /**
     * Check if class is present.
     *
     * @param className class name
     * @return true if present, false otherwise
     */
    public static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable throwable) {
            LOG.warn("Failed to check class present", throwable);
            return false;
        }
    }
}
