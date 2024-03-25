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

import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataMapValue;
import com.tencent.polaris.metadata.core.MetadataProvider;
import com.tencent.polaris.metadata.core.MetadataStringValue;
import com.tencent.polaris.metadata.core.MetadataValue;
import com.tencent.polaris.metadata.core.TransitiveType;
import com.tencent.polaris.metadata.core.Utils;
import com.tencent.polaris.metadata.core.manager.ComposeMetadataProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class MetadataContainerImpl implements MetadataContainer {

    private final Map<String, MetadataValue> values = new ConcurrentHashMap<>();

    private final String transitivePrefix;

    private final boolean keyCaseSensitive;

    private final AtomicReference<MetadataProvider> metadataProviderReference = new AtomicReference<>();

    private final ContainerBasedMetadataProvider containerBasedMetadataProvider = new ContainerBasedMetadataProvider();

    public MetadataContainerImpl(String transitivePrefix, boolean keyCaseSensitive) {
        this.transitivePrefix = transitivePrefix;
        this.keyCaseSensitive = keyCaseSensitive;
        metadataProviderReference.set(new ComposeMetadataProvider(
                transitivePrefix, Collections.singletonList(containerBasedMetadataProvider)));
    }

    @Override
    public void putMetadataStringValue(String key, String value, TransitiveType transitiveType) {
        values.put(normalizeKey(key), new MetadataStringValueImpl(transitiveType, value));
    }

    @Override
    public void putMetadataMapValue(String key, String mapKey, String value, TransitiveType transitiveType) {
        MetadataValue metadataValue = values.computeIfAbsent(normalizeKey(key), new Function<String, MetadataValue>() {
            @Override
            public MetadataValue apply(String s) {
                return new MetadataMapValueImpl(transitivePrefix, keyCaseSensitive);
            }
        });
        MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
        metadataMapValue.putMapStringValue(mapKey, value, transitiveType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends MetadataValue> T getMetadataValue(String key) {
        MetadataValue metadataValue = values.get(normalizeKey(key));
        if (null == metadataValue) {
            return null;
        }
        return (T) metadataValue;
    }

    public void iterateMetadataValues(BiConsumer<String, MetadataValue> iterator) {
        values.forEach(iterator);
    }

    @Override
    public Map<String, String> getTransitiveStringValues() {
        Map<String, String> values = new HashMap<>();
        iterateMetadataValues(new BiConsumer<String, MetadataValue>() {
            @Override
            public void accept(String key, MetadataValue metadataValue) {
                String realKey = null;
                String realValue = null;
                if (metadataValue instanceof MetadataStringValue) {
                    MetadataStringValue metadataStringValue = (MetadataStringValue) metadataValue;
                    switch (metadataStringValue.getTransitiveType()) {
                        case PASS_THROUGH:
                            realKey = Utils.encapsulateMetadataKey(transitivePrefix, key);
                            realValue = metadataStringValue.getStringValue();
                            break;
                        case DISPOSABLE:
                            realKey = key;
                            realValue = metadataStringValue.getStringValue();
                            break;
                        default:
                            break;
                    }
                }
                if (null != realKey && null != realValue) {
                    values.put(realKey, realValue);
                }

            }
        });
        return values;
    }

    @Override
    public Map<String, String> getMapTransitiveStringValues(String key) {
        MetadataMapValue metadataMapValue = getMetadataValue(key);
        if (null == metadataMapValue) {
            return Collections.emptyMap();
        }
        return metadataMapValue.getTransitiveStringValues();
    }

    @Override
    public void setMetadataProvider(MetadataProvider metadataProvider) {
        List<MetadataProvider> metadataProviders = new ArrayList<>();
        if (null != metadataProvider) {
            metadataProviders.add(metadataProvider);
        }
        metadataProviders.add(containerBasedMetadataProvider);
        metadataProviderReference.set(new ComposeMetadataProvider(transitivePrefix, metadataProviders));
    }

    @Override
    public MetadataProvider getMetadataProvider() {
        return metadataProviderReference.get();
    }

    @Override
    public String getRawMetadataStringValue(String key) {
        MetadataProvider metadataProvider = getMetadataProvider();
        return metadataProvider.getRawMetadataStringValue(key);
    }

    @Override
    public String getRawMetadataMapValue(String key, String mapKey) {
        MetadataProvider metadataProvider = getMetadataProvider();
        return metadataProvider.getRawMetadataMapValue(key, mapKey);
    }

    @Override
    public <T> void putMetadataObjectValue(String key, T value) {
        values.put(normalizeKey(key), new MetadataObjectValueImpl<>(value));
    }

    @Override
    public <T> void putMetadataMapObjectValue(String key, String mapKey, T value) {
        MetadataValue metadataValue = values.computeIfAbsent(normalizeKey(key), new Function<String, MetadataValue>() {
            @Override
            public MetadataValue apply(String s) {
                return new MetadataMapValueImpl(transitivePrefix, keyCaseSensitive);
            }
        });
        MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
        metadataMapValue.putMetadataObjectValue(normalizeKey(mapKey), value);
    }

    private class ContainerBasedMetadataProvider implements MetadataProvider {

        @Override
        public String getRawMetadataStringValue(String key) {
            MetadataValue metadataValue = getMetadataValue(key);
            if (metadataValue instanceof MetadataStringValue) {
                return ((MetadataStringValue) metadataValue).getStringValue();
            }
            return null;
        }

        @Override
        public String getRawMetadataMapValue(String key, String mapKey) {
            MetadataValue metadataValue = getMetadataValue(key);
            if (metadataValue instanceof MetadataMapValue) {
                MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
                MetadataValue mapValue = metadataMapValue.getMapValue(mapKey);
                if (mapValue instanceof MetadataStringValue) {
                    return ((MetadataStringValue) mapValue).getStringValue();
                }
                return null;
            }
            return null;
        }
    }

    private String normalizeKey(String key) {
        return keyCaseSensitive ? key : Utils.normalize(key);
    }
}
