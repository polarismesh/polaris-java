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

public interface CaseSensitiveMetadataProvider extends MetadataProvider {

    /**
     * 根据键获取一级字符串型元数据原始值
     * @param key 元数据键
     * @param keyCaseSensitive 是否区分大小写
     * @return 字符串原始值
     */
    String getRawMetadataStringValue(String key, boolean keyCaseSensitive);

    /**
     * 获取原始二级元数据值
     * @param key 一级键
     * @param mapKey 二级键
     * @param keyCaseSensitive 是否区分大小写
     * @return 值
     */
    String getRawMetadataMapValue(String key, String mapKey, boolean keyCaseSensitive);
}
