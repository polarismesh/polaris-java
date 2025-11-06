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

package com.tencent.polaris.metadata.core.test;

import com.tencent.polaris.metadata.core.*;
import com.tencent.polaris.metadata.core.impl.MessageMetadataContainerImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TestMetadataContainer {

    @Test
    public void testPutMetadataStringValue() {
        MetadataContainer metadataContainer = new MessageMetadataContainerImpl("test-prefix");
        metadataContainer.putMetadataStringValue("KEY", "11", TransitiveType.NONE);
        metadataContainer.putMetadataStringValue("key", "22", TransitiveType.NONE);
        Assert.assertEquals("22", metadataContainer.getRawMetadataStringValue("KEY"));
        Assert.assertEquals("11", metadataContainer.getRawMetadataStringValue("KEY", true));
        MetadataStringValue metadataStringValue = metadataContainer.getMetadataValue("KEY");
        Assert.assertEquals("22", metadataStringValue.getStringValue());
        metadataStringValue = metadataContainer.getMetadataValue("KEY", true);
        Assert.assertEquals("11", metadataStringValue.getStringValue());
    }

    @Test
    public void testPutMetadataMapValue() {
        MetadataContainer metadataContainer = new MessageMetadataContainerImpl("test-prefix");
        metadataContainer.putMetadataMapValue("KEY", "MAPKey", "11", TransitiveType.NONE);
        metadataContainer.putMetadataMapValue("key", "mapkey", "22", TransitiveType.NONE);
        Assert.assertEquals("22", metadataContainer.getRawMetadataMapValue("KEY", "MAPKey"));
        Assert.assertEquals("11", metadataContainer.getRawMetadataMapValue("KEY", "MAPKey", true));
        MetadataMapValue metadataMapValue = metadataContainer.getMetadataValue("KEY");
        MetadataStringValue metadataStringValue = (MetadataStringValue) metadataMapValue.getMapValue("MAPKey");
        Assert.assertEquals("22", metadataStringValue.getStringValue());
        metadataMapValue = metadataContainer.getMetadataValue("KEY", true);
        metadataStringValue = (MetadataStringValue) metadataMapValue.getMapValue("MAPKey", true);
        Assert.assertEquals("11", metadataStringValue.getStringValue());
    }

    @Test
    public void testPutMetadataObjectValue() {
        MetadataContainer metadataContainer = new MessageMetadataContainerImpl("test-prefix");
        metadataContainer.putMetadataObjectValue("KEY", new Value("11"));
        metadataContainer.putMetadataObjectValue("key", new Value("22"));
        MetadataObjectValue<Value> metadataObjectValue = metadataContainer.getMetadataValue("KEY");
        Value value = null;
        if (metadataObjectValue.getObjectValue().isPresent()) {
            value = metadataObjectValue.getObjectValue().get();
        }
        Assert.assertNotNull(value);
        Assert.assertEquals("22", value.iValue);

        metadataObjectValue = metadataContainer.getMetadataValue("KEY", true);
        value = null;
        if (metadataObjectValue.getObjectValue().isPresent()) {
            value = metadataObjectValue.getObjectValue().get();
        }
        Assert.assertNotNull(value);
        Assert.assertEquals("11", value.iValue);
    }

    @Test
    public void testPutMetadataMapObjectValue() {
        MetadataContainer metadataContainer = new MessageMetadataContainerImpl("test-prefix");
        metadataContainer.putMetadataMapObjectValue("KEY", "MAPKey", new Value("11"));
        metadataContainer.putMetadataMapObjectValue("key", "MAPKEY", new Value("22"));
        MetadataMapValue metadataMapValue = metadataContainer.getMetadataValue("KEY");
        MetadataObjectValue<?> metadataObjectValue = (MetadataObjectValue<?>) metadataMapValue.getMapValue("MAPKey");
        Value value = null;
        if (metadataObjectValue.getObjectValue().isPresent()) {
            value = (Value) metadataObjectValue.getObjectValue().get();
        }
        Assert.assertNotNull(value);
        Assert.assertEquals("22", value.iValue);

        metadataMapValue = metadataContainer.getMetadataValue("KEY", true);
        metadataObjectValue = (MetadataObjectValue<?>) metadataMapValue.getMapValue("MAPKey", true);
        value = null;
        if (metadataObjectValue.getObjectValue().isPresent()) {
            value = (Value) metadataObjectValue.getObjectValue().get();
        }
        Assert.assertNotNull(value);
        Assert.assertEquals("11", value.iValue);
    }

    @Test
    public void testMetadataProvider() {
        MetadataContainer metadataContainer = new MessageMetadataContainerImpl("test-prefix");
        metadataContainer.putMetadataStringValue("KEY", "11", TransitiveType.NONE);
        metadataContainer.putMetadataStringValue("key", "22", TransitiveType.NONE);
        metadataContainer.putMetadataMapValue("KEY1", "MAPKey", "111", TransitiveType.NONE);
        metadataContainer.putMetadataMapValue("key1", "mapkey", "222", TransitiveType.NONE);
        MetadataProvider metadataProvider1 = new MetadataProvider() {
            @Override
            public String doGetRawMetadataStringValue(String key) {
                if (key.equalsIgnoreCase("KEY")) {
                    return "33";
                }
                return null;
            }

            @Override
            public String doGetRawMetadataMapValue(String key, String mapKey) {
                if (key.equalsIgnoreCase("KEY1") && mapKey.equalsIgnoreCase("MAPKEY")) {
                    return "333";
                }
                return null;
            }
        };
        metadataContainer.setMetadataProvider(metadataProvider1);
        Assert.assertEquals("33", metadataContainer.getRawMetadataStringValue("KEY"));
        Assert.assertEquals("11", metadataContainer.getRawMetadataStringValue("KEY", true));
        Assert.assertEquals("333", metadataContainer.getRawMetadataMapValue("KEY1", "MAPKey"));
        Assert.assertEquals("111", metadataContainer.getRawMetadataMapValue("KEY1", "MAPKey", true));
    }

    @Test
    public void testCaseSensitiveMetadataProvider() {
        MetadataContainer metadataContainer = new MessageMetadataContainerImpl("test-prefix");
        metadataContainer.putMetadataStringValue("KEY", "11", TransitiveType.NONE);
        metadataContainer.putMetadataStringValue("key", "22", TransitiveType.NONE);
        metadataContainer.putMetadataMapValue("KEY1", "MAPKey", "111", TransitiveType.NONE);
        metadataContainer.putMetadataMapValue("key1", "mapkey", "222", TransitiveType.NONE);
        CaseSensitiveMetadataProvider metadataProvider1 = new CaseSensitiveMetadataProvider() {
            @Override
            public String doGetRawMetadataStringValue(String key, boolean keyCaseSensitive) {
                if (key.equals("KEY")) {
                    return "44";
                }
                return null;
            }

            @Override
            public String doGetRawMetadataMapValue(String key, String mapKey, boolean keyCaseSensitive) {
                if (key.equals("KEY1") && mapKey.equals("MAPKey")) {
                    return "444";
                }
                return null;
            }

            @Override
            public String doGetRawMetadataStringValue(String key) {
                if (key.equalsIgnoreCase("KEY")) {
                    return "33";
                }
                return null;
            }

            @Override
            public String doGetRawMetadataMapValue(String key, String mapKey) {
                if (key.equalsIgnoreCase("KEY1") && mapKey.equalsIgnoreCase("MAPKEY")) {
                    return "333";
                }
                return null;
            }
        };
        metadataContainer.setMetadataProvider(metadataProvider1);
        Assert.assertEquals("33", metadataContainer.getRawMetadataStringValue("KEY"));
        Assert.assertEquals("44", metadataContainer.getRawMetadataStringValue("KEY", true));
        Assert.assertEquals("333", metadataContainer.getRawMetadataMapValue("KEY1", "MAPKey"));
        Assert.assertEquals("444", metadataContainer.getRawMetadataMapValue("KEY1", "MAPKey", true));
    }

    @Test
    public void testGetTransitiveStringValues() {
        MetadataContainer metadataContainer = new MessageMetadataContainerImpl("test-prefix-");
        metadataContainer.putMetadataStringValue("KEY", "11", TransitiveType.PASS_THROUGH);
        metadataContainer.putMetadataStringValue("key", "22", TransitiveType.DISPOSABLE);
        metadataContainer.putMetadataStringValue("keykey", "22", TransitiveType.NONE);
        Set<String> expects = new HashSet<>();
        expects.add("test-prefix-KEY");
        expects.add("key");
        Map<String, String> transitiveStringValues = metadataContainer.getTransitiveStringValues();
        Assert.assertEquals(expects, transitiveStringValues.keySet());
    }

    @Test
    public void testGetMapTransitiveStringValues() {
        MetadataContainer metadataContainer = new MessageMetadataContainerImpl("test-prefix-");
        metadataContainer.putMetadataMapValue("KEY1", "MAPKey", "111", TransitiveType.DISPOSABLE);
        metadataContainer.putMetadataMapValue("key1", "mapkey", "222", TransitiveType.PASS_THROUGH);
        Set<String> expects = new HashSet<>();
        expects.add("test-prefix-mapkey");
        expects.add("MAPKey");
    }

    private static class Value {
        final String iValue;

        public Value(String iValue) {
            this.iValue = iValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Value value = (Value) o;
            return Objects.equals(iValue, value.iValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(iValue);
        }
    }

}

