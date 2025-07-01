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

package com.tencent.polaris.plugins.stat.common.model;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static com.tencent.polaris.plugins.stat.common.TestUtil.getRandomLabels;
import static com.tencent.polaris.plugins.stat.common.TestUtil.getRandomString;

public class StatMetricTest {
    @Test
    public void testIncValue() {
        String metricName = getRandomString(3, 10);
        Map<String, String> testLabels = getRandomLabels();
        StatMetric statMetric = new StatMetric(metricName, testLabels);

        int count = 10;
        CountDownLatch latch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            new Thread(() -> {
                try {
                    statMetric.incValue();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(statMetric.getValue(), count, 0);
    }

    @Test
    public void testEquals() {
        String metricName = getRandomString(3, 10);
        Map<String, String> testLabels = getRandomLabels();
        Assert.assertEquals(new StatMetric(metricName, testLabels), new StatMetric(metricName, testLabels));
    }
}
