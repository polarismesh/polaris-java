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

import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MetadataContext {

    public static final String DEFAULT_TRANSITIVE_PREFIX = "X-Polaris-Metadata-Transitive-";

    private final MetadataContainerGroup callerMetadataContainerGroup;

    private final MetadataContainerGroup calleeMetadataContainerGroup;

    private final String transitivePrefix;

    public MetadataContext(List<String> transitivePrefixes) {
        if (transitivePrefixes == null || transitivePrefixes.isEmpty()) {
            transitivePrefixes = new ArrayList<>();
            transitivePrefixes.add("");
        }
        this.transitivePrefix = transitivePrefixes.get(0);
        callerMetadataContainerGroup = new MetadataContainerGroup(transitivePrefixes);
        calleeMetadataContainerGroup = new MetadataContainerGroup(transitivePrefixes);
    }

    public MetadataContext() {
        this(Collections.singletonList(DEFAULT_TRANSITIVE_PREFIX));
    }

    @SuppressWarnings("unchecked")
    public <T extends MetadataContainer> T getMetadataContainer(MetadataType metadataType, boolean caller) {
        MetadataContainerGroup metadataContainer;
        if (caller) {
            metadataContainer = callerMetadataContainerGroup;
        } else {
            metadataContainer = calleeMetadataContainerGroup;
        }
        switch (metadataType) {
            case MESSAGE:
                return (T) metadataContainer.getMessageMetadataContainer();
            case APPLICATION:
                return (T) metadataContainer.getApplicationMetadataContainer();
            case CUSTOM:
                return (T) metadataContainer.getCustomMetadataContainer();
            default:
                return null;
        }
    }

    public String getTransitivePrefix() {
        return transitivePrefix;
    }

}
