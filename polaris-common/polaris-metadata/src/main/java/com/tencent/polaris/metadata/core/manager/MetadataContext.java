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

package com.tencent.polaris.metadata.core.manager;

import com.tencent.polaris.metadata.core.*;
import com.tencent.polaris.metadata.core.impl.MetadataContainerImpl;

import java.util.ArrayList;
import java.util.List;

public class MetadataContext {

    public static final String DEFAULT_TRANSITIVE_PREFIX = "X-Polaris-Metadata-Transitive-";

    private final MetadataContainer messageMetadataContainer;

    private final MetadataContainer applicationMetadataContainer;

    private final MetadataContainer customMetadataContainer;

    private final String transitivePrefix;

    private final MetadataProvider metadataProvider;

    public MetadataContext(String transitivePrefix, List<MetadataProvider> providers) {
        this(transitivePrefix, providers, null);
    }

    public MetadataContext(String transitivePrefix, List<MetadataProvider> providers, List<String> anotherTransitivePrefixes) {
        this.transitivePrefix = transitivePrefix;
        this.messageMetadataContainer = new MetadataContainerImpl(transitivePrefix);
        this.applicationMetadataContainer = new MetadataContainerImpl(transitivePrefix);
        this.customMetadataContainer = new MetadataContainerImpl(transitivePrefix);
        List<MetadataProvider> metadataProviders = new ArrayList<>();
        if (null != providers) {
            metadataProviders.addAll(providers);
        }
        metadataProviders.add(new DefaultMetadataProvider());
        List<String> prefixes = new ArrayList<>();
        prefixes.add(transitivePrefix);
        if (null != anotherTransitivePrefixes) {
            prefixes.addAll(anotherTransitivePrefixes);
        }
        metadataProvider = new ComposeMetadataProvider(prefixes, metadataProviders);
    }

    public MetadataContext() {
        this(DEFAULT_TRANSITIVE_PREFIX, null);
    }

    public MetadataContainer getMetadataContainer(MetadataType metadataType) {
        switch (metadataType) {
            case MESSAGE:
                return messageMetadataContainer;
            case APPLICATION:
                return applicationMetadataContainer;
            case CUSTOM:
                return customMetadataContainer;
            default:
                return null;
        }
    }

    private class DefaultMetadataProvider implements MetadataProvider {

        @Override
        public String getStringValue(MetadataType metadataType, String key) {
            MetadataContainer metadataContainer = getMetadataContainer(metadataType);
            if (null == metadataContainer) {
                return null;
            }
            MetadataValue metadataValue = metadataContainer.getMetadataValue(key);
            if (metadataValue instanceof MetadataStringValue) {
                return ((MetadataStringValue)metadataValue).getStringValue();
            }
            return null;
        }

        @Override
        public String getMapValue(MetadataType metadataType, String key, String mapKey) {
            MetadataContainer metadataContainer = getMetadataContainer(metadataType);
            if (null == metadataContainer) {
                return null;
            }
            MetadataValue metadataValue = metadataContainer.getMetadataValue(key);
            if (metadataValue instanceof MetadataMapValue) {
                MetadataMapValue metadataMapValue = (MetadataMapValue) metadataValue;
                MetadataStringValue mapValue = metadataMapValue.getMapValue(mapKey);
                if (null != mapValue) {
                    return mapValue.getStringValue();
                }
                return null;
            }
            return null;
        }
    }

    public String getTransitivePrefix() {
        return transitivePrefix;
    }

    public MetadataProvider getMetadataProvider() {
        return metadataProvider;
    }

}
