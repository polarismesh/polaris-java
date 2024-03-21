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

package com.tencent.polaris.metadata.core;

import java.util.Map;
import java.util.function.BiConsumer;

public interface MetadataContainer {

    void putMetadataStringValue(String key, String value, TransitiveType transitiveType);

    String getRawMetadataStringValue(String key);

    void putMetadataMapValue(String key, String mapKey, String value, TransitiveType transitiveType);

    String getRawMetadataMapValue(String key, String mapKey);

    <T> void putMetadataObjectValue(String key, T value);

    <T> void putMetadataMapObjectValue(String key, String mapKey, T value);

    MetadataValue getMetadataValue(String key);

    void iterateMetadataValues(BiConsumer<String, MetadataValue> iterator);

    /**
     * 获取所有的透传标签列表（包括pass_through和disposal），对于Pass_through会自动带入透传前缀（如有）
     * @return 透传标签列表
     */
    Map<String, String> getAllTransitiveKeyValues();

    void setMetadataProvider(MetadataProvider metadataProvider);

    MetadataProvider getMetadataProvider();


}
