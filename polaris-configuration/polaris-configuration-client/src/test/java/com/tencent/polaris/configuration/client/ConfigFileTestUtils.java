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
