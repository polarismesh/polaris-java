package com.tencent.polaris.configuration.client;

import com.tencent.polaris.configuration.client.internal.DefaultConfigFileMetadata;

import java.util.Map;

/**
 * @author lepdou 2022-03-08
 */
public class ConfigFileTestUtils {

    public static String testNamespace = "testNamespace";
    public static String testGroup     = "testGroup";
    public static String testFileName  = "testFile";

    public static DefaultConfigFileMetadata assembleDefaultConfigFileMeta() {
        return new DefaultConfigFileMetadata(testNamespace, testGroup, testFileName);
    }

    static class User {

        private String              name;
        private int                 age;
        private Map<String, String> labels;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public void setLabels(Map<String, String> labels) {
            this.labels = labels;
        }
    }

    enum MyType {
        T1, T2
    }
}
