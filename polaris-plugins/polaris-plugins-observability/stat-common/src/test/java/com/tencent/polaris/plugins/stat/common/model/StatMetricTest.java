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
