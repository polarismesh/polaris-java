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
import com.tencent.polaris.metadata.core.impl.MessageMetadataContainerImpl;
import com.tencent.polaris.metadata.core.impl.MetadataContainerImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetadataContext {

    public static final String DEFAULT_TRANSITIVE_PREFIX = "X-Polaris-Metadata-Transitive-";

    private final MetadataContainer messageMetadataContainer;

    private final MetadataContainer applicationMetadataContainer;

    private final MetadataContainer customMetadataContainer;

    private final String transitivePrefix;

    public MetadataContext(String transitivePrefix, Map<MetadataType, MetadataProvider> providers) {
        this(transitivePrefix, providers, null);
    }

    public MetadataContext(String transitivePrefix, Map<MetadataType, MetadataProvider> providers, List<String> anotherTransitivePrefixes) {
        this.transitivePrefix = transitivePrefix;
        List<String> prefixes = new ArrayList<>();
        prefixes.add(transitivePrefix);
        if (null != anotherTransitivePrefixes) {
            prefixes.addAll(anotherTransitivePrefixes);
        }
        MetadataProvider messageProvider = null != providers ? providers.get(MetadataType.MESSAGE) : null;
        if (null != messageProvider) {
            messageProvider = new ComposeMetadataProvider(prefixes, messageProvider);
        }
        this.messageMetadataContainer = new MessageMetadataContainerImpl(transitivePrefix, messageProvider);

        MetadataProvider applicationProvider = null != providers ? providers.get(MetadataType.APPLICATION) : null;
        if (null != applicationProvider) {
            applicationProvider = new ComposeMetadataProvider(prefixes, applicationProvider);
        }
        this.applicationMetadataContainer = new MetadataContainerImpl(transitivePrefix, applicationProvider);

        MetadataProvider customProvider = null != providers ? providers.get(MetadataType.CUSTOM) : null;
        if (null != customProvider) {
            customProvider = new ComposeMetadataProvider(prefixes, customProvider);
        }
        this.customMetadataContainer = new MetadataContainerImpl(transitivePrefix, customProvider);
    }

    public MetadataContext() {
        this(DEFAULT_TRANSITIVE_PREFIX, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends MetadataContainer> T getMetadataContainer(MetadataType metadataType) {
        switch (metadataType) {
            case MESSAGE:
                return (T) messageMetadataContainer;
            case APPLICATION:
                return (T) applicationMetadataContainer;
            case CUSTOM:
                return (T) customMetadataContainer;
            default:
                return null;
        }
    }

    public String getTransitivePrefix() {
        return transitivePrefix;
    }

}
