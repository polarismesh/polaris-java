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

import java.util.List;

import com.tencent.polaris.metadata.core.MetadataProvider;
import com.tencent.polaris.metadata.core.Utils;

public class ComposeMetadataProvider implements MetadataProvider {

    private final List<MetadataProvider> metadataProviders;

    private final String transitivePrefix;

    public ComposeMetadataProvider(String transitivePrefix, List<MetadataProvider> metadataProviders) {
        assert null != metadataProviders && !metadataProviders.isEmpty();
        this.transitivePrefix = transitivePrefix;
        this.metadataProviders = metadataProviders;
    }

    @Override
    public String getRawMetadataStringValue(String key) {
        // 先获取透传标签
        if (null != transitivePrefix && !transitivePrefix.isEmpty()) {
            String value = getRawStringValue(Utils.encapsulateMetadataKey(transitivePrefix, key));
            if (null != value) {
                return value;
            }
        }
        // 透传标签获取失败，则获取原始标签
        return getRawStringValue(key);
    }

    @Override
    public String getRawMetadataMapValue(String key, String mapKey) {
        // 先获取透传标签
        if (null != transitivePrefix && !transitivePrefix.isEmpty()) {
            String value = getRawMapValue(key, Utils.encapsulateMetadataKey(transitivePrefix, mapKey));
            if (null != value) {
                return value;
            }
        }
        // 透传标签获取失败，则获取原始标签
        return getRawMapValue(key, mapKey);
    }

    private String getRawStringValue(String key) {
        for (MetadataProvider metadataProvider : metadataProviders) {
            String rawMetadataStringValue = metadataProvider.getRawMetadataStringValue(key);
            if (null != rawMetadataStringValue) {
                return rawMetadataStringValue;
            }
        }
        return null;
    }

    private String getRawMapValue(String key, String mapKey) {
        for (MetadataProvider metadataProvider : metadataProviders) {
            String rawMetadataMapValue = metadataProvider.getRawMetadataMapValue(key, mapKey);
            if (null != rawMetadataMapValue) {
                return rawMetadataMapValue;
            }
        }
        return null;
    }


}
