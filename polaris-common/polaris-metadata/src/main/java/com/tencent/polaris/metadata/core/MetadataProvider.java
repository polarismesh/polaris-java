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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MetadataProvider {

    Logger LOG = LoggerFactory.getLogger(MetadataProvider.class);

    /**
     * 根据键获取一级字符串型元数据原始值
     * @param key 元数据键
     * @return 字符串原始值
     */
    default String getRawMetadataStringValue(String key) {
        try {
            return doGetRawMetadataStringValue(key);
        } catch (Throwable throwable) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("[{}] get raw metadata string value with key {} failed.", this.getClass(), key, throwable);
            } else {
                LOG.warn("[{}] get raw metadata string value with key {} failed. Caused by: {}",
                        this.getClass(), key, throwable.getMessage());
            }
            return null;
        }
    }

    /**
     * 根据键获取一级字符串型元数据原始值
     *
     * @param key 元数据键
     * @return 字符串原始值
     */
    String doGetRawMetadataStringValue(String key);

    /**
     * 获取原始二级元数据值
     * @param key 一级键
     * @param mapKey 二级键
     * @return 值
     */
    default String getRawMetadataMapValue(String key, String mapKey) {
        try {
            return doGetRawMetadataMapValue(key, mapKey);
        } catch (Throwable throwable) {
            if (LOG.isDebugEnabled()) {
                LOG.warn("[{}] get raw metadata map value with key {} and mapKey {} failed.",
                        this.getClass(), key, mapKey, throwable);
            } else {
                LOG.warn("[{}] get raw metadata map value with key {} and mapKey {} failed. Caused by: {}",
                        this.getClass(), key, mapKey, throwable.getMessage());
            }
            return null;
        }
    }

    /**
     * 获取原始二级元数据值
     *
     * @param key    一级键
     * @param mapKey 二级键
     * @return 值
     */
    String doGetRawMetadataMapValue(String key, String mapKey);

}
