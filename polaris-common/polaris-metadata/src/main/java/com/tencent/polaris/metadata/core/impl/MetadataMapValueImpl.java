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

package com.tencent.polaris.metadata.core.impl;

import com.tencent.polaris.metadata.core.MetadataMapValue;
import com.tencent.polaris.metadata.core.MetadataStringValue;
import com.tencent.polaris.metadata.core.MetadataValue;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class MetadataMapValueImpl implements MetadataMapValue {

    private final Map<String, MetadataValue> mapValues = new ConcurrentHashMap<>();

    private final Map<String, MetadataValue> normalizedMapValues = new ConcurrentHashMap<>();

    private final String transitivePrefix;

    public MetadataMapValueImpl(String transitivePrefix) {
        this.transitivePrefix = transitivePrefix;
    }

    @Override
    public MetadataValue getMapValue(String key) {
        return getMapValue(key, Utils.DEFAULT_KEY_CASE_SENSITIVE);
    }

    @Override
    public MetadataValue getMapValue(String key, boolean keyCaseSensitive) {
        if (keyCaseSensitive) {
            return mapValues.get(key);
        }
        return normalizedMapValues.get(Utils.normalize(key));
    }

    @Override
    public void putMapStringValue(String key, String value, TransitiveType transitiveType) {
        MetadataStringValueImpl metadataStringValue = new MetadataStringValueImpl(transitiveType, value);
        mapValues.put(key, metadataStringValue);
        normalizedMapValues.put(Utils.normalize(key), metadataStringValue);
    }

    @Override
    public <T> void putMetadataObjectValue(String key, T value) {
        MetadataObjectValueImpl<T> tMetadataObjectValue = new MetadataObjectValueImpl<>(value);
        mapValues.put(key, tMetadataObjectValue);
        normalizedMapValues.put(Utils.normalize(key), tMetadataObjectValue);
    }

    @Override
    public Map<String, MetadataValue> getMapValues() {
        return Collections.unmodifiableMap(mapValues);
    }

    @Override
    public void iterateMapValues(BiConsumer<String, MetadataValue> iterator) {
        mapValues.forEach(iterator);
    }

    @Override
    public Map<String, String> getTransitiveStringValues() {
        Map<String, String> values = new HashMap<>();
        iterateMapValues(new BiConsumer<String, MetadataValue>() {
            @Override
            public void accept(String key, MetadataValue metadataValue) {
                if (metadataValue instanceof MetadataStringValue) {
                    MetadataStringValue metadataStringValue = (MetadataStringValue) metadataValue;
                    switch (metadataStringValue.getTransitiveType()) {
                        case PASS_THROUGH:
                            values.put(Utils.encapsulateMetadataKey(transitivePrefix, key), metadataStringValue.getStringValue());
                            break;
                        case DISPOSABLE:
                            values.put(key, metadataStringValue.getStringValue());
                            break;
                        default:
                            break;
                    }
                }
            }
        });
        return values;
    }

}
