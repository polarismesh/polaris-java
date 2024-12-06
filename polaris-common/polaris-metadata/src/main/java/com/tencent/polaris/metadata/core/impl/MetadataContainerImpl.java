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

package com.tencent.polaris.metadata.core.impl;

import com.tencent.polaris.metadata.core.CaseSensitiveMetadataProvider;
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

    private final Map<String, MetadataValue> normalizedValues = new ConcurrentHashMap<>();

    private final String transitivePrefix;

    private final AtomicReference<ComposeMetadataProvider> metadataProviderReference = new AtomicReference<>();

    private final ContainerBasedMetadataProvider containerBasedMetadataProvider = new ContainerBasedMetadataProvider();

    public MetadataContainerImpl(String transitivePrefix) {
        this.transitivePrefix = transitivePrefix;
        metadataProviderReference.set(new ComposeMetadataProvider(
                transitivePrefix, Collections.singletonList(containerBasedMetadataProvider)));
    }

    @Override
    public void putMetadataStringValue(String key, String value, TransitiveType transitiveType) {
        MetadataStringValueImpl metadataStringValue = new MetadataStringValueImpl(transitiveType, value);
        values.put(key, metadataStringValue);
        normalizedValues.put(Utils.normalize(key), metadataStringValue);
    }

    @Override
    public void putMetadataMapValue(String key, String mapKey, String value, TransitiveType transitiveType) {
        MetadataValue metadataValue = normalizedValues.computeIfAbsent(Utils.normalize(key), new Function<String, MetadataValue>() {
            @Override
            public MetadataValue apply(String s) {
                return new MetadataMapValueImpl(transitivePrefix);
            }
        });
        values.put(key, metadataValue);
        MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
        metadataMapValue.putMapStringValue(mapKey, value, transitiveType);
    }

    @Override
    public <T extends MetadataValue> T getMetadataValue(String key) {
        return getMetadataValue(key, Utils.DEFAULT_KEY_CASE_SENSITIVE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends MetadataValue> T getMetadataValue(String key, boolean keyCaseSensitive) {
        Map<String, MetadataValue> theValues;
        String theKey;
        if (keyCaseSensitive) {
            theValues = values;
            theKey = key;
        } else {
            theValues = normalizedValues;
            theKey = Utils.normalize(key);
        }
        MetadataValue metadataValue = theValues.get(theKey);
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
        return getMapTransitiveStringValues(key, Utils.DEFAULT_KEY_CASE_SENSITIVE);
    }

    @Override
    public Map<String, String> getMapTransitiveStringValues(String key, boolean keyCaseSensitive) {
        MetadataMapValue metadataMapValue = getMetadataValue(key, keyCaseSensitive);
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
    public ComposeMetadataProvider getMetadataProvider() {
        return metadataProviderReference.get();
    }

    @Override
    public String getRawMetadataStringValue(String key) {
        MetadataProvider metadataProvider = getMetadataProvider();
        return metadataProvider.getRawMetadataStringValue(key);
    }

    @Override
    public String getRawMetadataStringValue(String key, boolean keyCaseSensitive) {
        ComposeMetadataProvider metadataProvider = getMetadataProvider();
        return metadataProvider.getRawMetadataStringValue(key, keyCaseSensitive);
    }

    @Override
    public String getRawMetadataMapValue(String key, String mapKey) {
        MetadataProvider metadataProvider = getMetadataProvider();
        return metadataProvider.getRawMetadataMapValue(key, mapKey);
    }

    @Override
    public String getRawMetadataMapValue(String key, String mapKey, boolean keyCaseSensitive) {
        ComposeMetadataProvider metadataProvider = getMetadataProvider();
        return metadataProvider.getRawMetadataMapValue(key, mapKey, keyCaseSensitive);
    }

    @Override
    public <T> void putMetadataObjectValue(String key, T value) {
        MetadataObjectValueImpl<T> tMetadataObjectValue = new MetadataObjectValueImpl<>(value);
        values.put(key, tMetadataObjectValue);
        normalizedValues.put(Utils.normalize(key), tMetadataObjectValue);
    }

    @Override
    public <T> void putMetadataMapObjectValue(String key, String mapKey, T value) {
        MetadataValue metadataValue = normalizedValues.computeIfAbsent(Utils.normalize(key), new Function<String, MetadataValue>() {
            @Override
            public MetadataValue apply(String s) {
                return new MetadataMapValueImpl(transitivePrefix);
            }
        });
        values.put(key, metadataValue);
        MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
        metadataMapValue.putMetadataObjectValue(mapKey, value);
    }

    private class ContainerBasedMetadataProvider implements CaseSensitiveMetadataProvider {

        @Override
        public String getRawMetadataStringValue(String key) {
            return getRawMetadataStringValue(key, Utils.DEFAULT_KEY_CASE_SENSITIVE);
        }

        @Override
        public String getRawMetadataMapValue(String key, String mapKey) {
            return getRawMetadataMapValue(key, mapKey, Utils.DEFAULT_KEY_CASE_SENSITIVE);
        }

        @Override
        public String getRawMetadataStringValue(String key, boolean keyCaseSensitive) {
            MetadataValue metadataValue = getMetadataValue(key, keyCaseSensitive);
            if (metadataValue instanceof MetadataStringValue) {
                return ((MetadataStringValue) metadataValue).getStringValue();
            }
            return null;
        }

        @Override
        public String getRawMetadataMapValue(String key, String mapKey, boolean keyCaseSensitive) {
            MetadataValue metadataValue = getMetadataValue(key, keyCaseSensitive);
            if (metadataValue instanceof MetadataMapValue) {
                MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
                MetadataValue mapValue = metadataMapValue.getMapValue(mapKey, keyCaseSensitive);
                if (mapValue instanceof MetadataStringValue) {
                    return ((MetadataStringValue) mapValue).getStringValue();
                }
                return null;
            }
            return null;
        }
    }

}
