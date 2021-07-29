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
