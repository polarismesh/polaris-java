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

public interface MetadataMapValue extends MetadataValue {

    /**
     * 根据Map的元数据键，获取对应的元数据值
     * @param key 元数据键
     * @return 对应的元数据值
     */
    MetadataValue getMapValue(String key);

    /**
     * 塞入元数据字符串键值对，用户可以为键值对设置透传类型
     * @param key 元数据键
     * @param value 元数据值
     * @param transitiveType 透传类型
     */
    void putMapStringValue(String key, String value, TransitiveType transitiveType);


    /**
     * 塞入元数据对象键值对，对象键值对不支持透传
     * @param key 元数据键
     * @param value 元数据值
     * @param <T> 元数据值泛型
     */
    <T> void putMetadataObjectValue(String key, T value);

    /**
     * 获取所有的元数据键值对，只读
     * @return 所有元数据键值对
     */
    Map<String, MetadataValue> getMapValues();

    /**
     * 遍历所有的元数据键值对
     * @param iterator 遍历回调函数
     */
    void iterateMapValues(BiConsumer<String, MetadataValue> iterator);

    /**
     * 获取所有的透传标签列表（包括pass_through和disposal），对于Pass_through会自动带入透传前缀（如有）
     * 只获取带透传标识的StringValue
     * @return 透传标签列表
     */
    Map<String, String> getTransitiveStringValues();
}
