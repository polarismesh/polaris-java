/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

    /**
     * 塞入元数据键值对，可以设置透传类型
     * @param key 元数据键
     * @param value 元数据值
     * @param transitiveType 透传类型
     */
    void putMetadataStringValue(String key, String value, TransitiveType transitiveType);

    /**
     * 根据键获取一级字符串型元数据原始值
     * 默认以大小写不敏感进行查询。如果同时存在多个KEY，且多个KEY之间只有大小写区别，则只会返回最晚塞进去的KEY对应的值
     * @param key 元数据键
     * @return 字符串原始值
     */
    String getRawMetadataStringValue(String key);

    /**
     * 根据键获取一级字符串型元数据原始值
     * @param key 元数据键
     * @param keyCaseSensitive 查询时候是否KEY大小写不敏感
     * @return 字符串原始值
     */
    String getRawMetadataStringValue(String key, boolean keyCaseSensitive);

    /**
     * 塞入二级元数据键值对
     * @param key 一级键
     * @param mapKey 二级键
     * @param value 具体的值
     * @param transitiveType 透传类型
     */
    void putMetadataMapValue(String key, String mapKey, String value, TransitiveType transitiveType);

    /**
     * 获取原始二级元数据值
     * 默认以大小写不敏感进行查询。如果同时存在多个KEY，且多个KEY之间只有大小写区别，则只会返回最晚塞进去的KEY对应的值
     * @param key 一级键
     * @param mapKey 二级键
     * @return 值
     */
    String getRawMetadataMapValue(String key, String mapKey);

    /**
     * 获取原始二级元数据值
     * @param key 一级键
     * @param mapKey 二级键
     * @param keyCaseSensitive 查询时候是否KEY大小写不敏感
     * @return 值
     */
    String getRawMetadataMapValue(String key, String mapKey, boolean keyCaseSensitive);

    /**
     * 塞入对象型元数据键值对，对象型元数据不支持透传
     * @param key 键
     * @param value 值
     * @param <T> 对象泛型
     */
    <T> void putMetadataObjectValue(String key, T value);

    /**
     * 塞入二级元数据对象键值对，不支持透传
     * @param key 一级键
     * @param mapKey 二级键
     * @param value 对象值
     * @param <T> 对象类型
     */
    <T> void putMetadataMapObjectValue(String key, String mapKey, T value);

    /**
     * 获取二级元数据值
     * 默认以大小写不敏感进行查询。如果同时存在多个KEY，且多个KEY之间只有大小写区别，则只会返回最晚塞进去的KEY对应的值
     * @param key 一级键
     * @return 一级元数据值
     * @param <T> 类型可以为MetadataMapValue, MetadataObjectValue, MetadataStringValue
     */
    <T extends MetadataValue> T getMetadataValue(String key);

    /**
     * 获取二级元数据值
     * @param key 一级键
     * @param keyCaseSensitive 查询时候是否KEY大小写不敏感
     * @return 一级元数据值
     * @param <T> 类型可以为MetadataMapValue, MetadataObjectValue, MetadataStringValue
     */
    <T extends MetadataValue> T getMetadataValue(String key, boolean keyCaseSensitive);

    /**
     * 遍历元数据列表
     * @param iterator 遍历器
     */
    void iterateMetadataValues(BiConsumer<String, MetadataValue> iterator);

    /**
     * 获取所有一级透传标签列表（包括pass_through和disposal），对于Pass_through会自动带入透传前缀（如有）
     * 只包含一级的透传元数据透传键值对，如需获取二级Map的键值对. 可以使用getMapTransitiveStringValues
     * }
     * @return 透传标签列表
     */
    Map<String, String> getTransitiveStringValues();

    /**
     * 获取二级透传标签列表（包括pass_through和disposal），对于Pass_through会自动带入透传前缀（如有）
     * 默认以大小写不敏感进行查询。如果同时存在多个KEY，且多个KEY之间只有大小写区别，则只会返回最晚塞进去的KEY对应的值
     * @param key 二级Map的键
     * @return 二级透传标签列表
     */
    Map<String, String> getMapTransitiveStringValues(String key);

    /**
     * 获取二级透传标签列表（包括pass_through和disposal），对于Pass_through会自动带入透传前缀（如有）
     * @param key 二级Map的键
     * @param keyCaseSensitive 查询时候是否KEY大小写不敏感
     * @return 二级透传标签列表
     */
    Map<String, String> getMapTransitiveStringValues(String key, boolean keyCaseSensitive);

    /**
     * 设置元数据提供者
     * 设置后，MetadataContainer内部会自动将MetadataProvider使用ComposeMetadataProvider进行包装
     * @param metadataProvider 元数据提供者接口
     */
    void setMetadataProvider(MetadataProvider metadataProvider);

    /**
     * 获取元数据提供者
     * @return 元数据提供者对象
     */
    MetadataProvider getMetadataProvider();


}
