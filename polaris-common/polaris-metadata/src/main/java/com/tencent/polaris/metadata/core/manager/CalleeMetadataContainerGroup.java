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
import com.tencent.polaris.metadata.core.impl.MessageMetadataContainerImpl;
import com.tencent.polaris.metadata.core.impl.MetadataContainerImpl;

import static com.tencent.polaris.metadata.core.manager.MetadataContext.DEFAULT_TRANSITIVE_PREFIX;

public class CalleeMetadataContainerGroup implements MetadataContainerGroup {

    private final MetadataContainer messageMetadataContainer;

    private final static MetadataContainer applicationMetadataContainer = new MetadataContainerImpl(DEFAULT_TRANSITIVE_PREFIX);

    private final MetadataContainer customMetadataContainer;

    public CalleeMetadataContainerGroup(String transitivePrefix) {
        assert null != transitivePrefix;
        this.messageMetadataContainer = new MessageMetadataContainerImpl(transitivePrefix);
        this.customMetadataContainer = new MetadataContainerImpl(transitivePrefix);
    }

    @Override
    public MetadataContainer getMessageMetadataContainer() {
        return messageMetadataContainer;
    }

    @Override
    public MetadataContainer getApplicationMetadataContainer() {
        return applicationMetadataContainer;
    }

    public static MetadataContainer getStaticApplicationMetadataContainer() {
        return applicationMetadataContainer;
    }

    @Override
    public MetadataContainer getCustomMetadataContainer() {
        return customMetadataContainer;
    }

}
