/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.polaris.metadata.core.manager.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class MetadataMapValueImpl implements MetadataMapValue {

    private final Map<String, MetadataStringValue> mapValues = new ConcurrentHashMap<>();

    private final String transitivePrefix;

    public MetadataMapValueImpl(String transitivePrefix) {
        this.transitivePrefix = transitivePrefix;
    }

    @Override
    public MetadataStringValue getMapValue(String key) {
        return mapValues.get(key);
    }

    @Override
    public void putMapValue(String key, MetadataStringValue value) {
        mapValues.put(key, value);
    }

    @Override
    public MetadataStringValue removeMapValue(String key) {
        return mapValues.remove(key);
    }

    @Override
    public Map<String, MetadataStringValue> getMapValues() {
        return Collections.unmodifiableMap(mapValues);
    }

    @Override
    public void iterateMapValues(BiConsumer<String, MetadataStringValue> iterator) {
        mapValues.forEach(iterator);
    }

    @Override
    public Map<String, String> getAllTransitiveKeyValues() {
        Map<String, String> values = new HashMap<>();
        iterateMapValues(new BiConsumer<String, MetadataStringValue>() {
            @Override
            public void accept(String key, MetadataStringValue metadataStringValue) {
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
        });
        return values;
    }
}
