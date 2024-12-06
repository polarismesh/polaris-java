/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.api.plugin.lossless;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InstanceProperties {

    /**
     * 权重设置，类型为int
     */
    public static final String CTX_KEY_WEIGHT = "weight";

    /**
     * 健康状态设置，类型为boolean
     */
    public static final String CTX_KEY_HEALTHY = "healthy";

    /**
     * 隔离状态设置，类型为boolean
     */
    public static final String CTX_KEY_ISOLATED = "isolated";

    private final Map<String, Object> properties = new HashMap<>();

    /**
     * 设置实例属性
     * @param key 属性KEY
     * @param value 属性值
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * 获取实例属性
     * @param key 属性KEY
     * @return 属性值
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * 获取所有的实例属性
     * @return 属性列表
     */
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

}
