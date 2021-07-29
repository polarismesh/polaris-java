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
