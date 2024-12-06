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

package com.tencent.polaris.plugins.stat.common.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static com.tencent.polaris.plugins.stat.common.TestUtil.getRandomString;

public class SignatureUtilTest {
    @Test
    public void testLabelsToSignature1() {
        Set<Long> signatures = new HashSet<>();
        for (int i = 0; i < 10000000; i++) {
            Random random = new Random();
            String key, value;
            Map<String, String> testLabels = new HashMap<String, String>();
            for (int j = 0; j < (random.nextInt(10) + 1); j++) {
                key = getRandomString(3, 10);
                value = getRandomString(3, 10);
                testLabels.put(key, value);
            }

            Assert.assertTrue(signatures.add(SignatureUtil.labelsToSignature(testLabels)));
        }
    }
}
