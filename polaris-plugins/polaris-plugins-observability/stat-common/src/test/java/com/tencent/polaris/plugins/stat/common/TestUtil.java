/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugins.stat.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TestUtil {
    public static String getRandomString(int minSize, int maxSize) {
        String dictionary = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";
        int dicLength = dictionary.length();
        Random random = new Random();
        StringBuilder sb = new StringBuilder(maxSize);
        int strLength = random.nextInt(maxSize - minSize) + minSize;
        for (int i = 0; i < strLength; i++) {
            int numIndex = random.nextInt(dicLength);
            sb.append(dictionary.charAt(numIndex));
        }
        return sb.toString();
    }

    public static Map<String, String> getRandomLabels() {
        Map<String, String> testLabels = new HashMap<String, String>();
        Random random = new Random();
        String key, value;
        for (int j = 0; j < (random.nextInt(10) + 1); j++) {
            key = getRandomString(3, 10);
            value = getRandomString(3, 10);
            testLabels.put(key, value);
        }
        return testLabels;
    }
}
