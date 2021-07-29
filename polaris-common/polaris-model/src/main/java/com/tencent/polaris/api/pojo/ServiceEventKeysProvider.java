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

package com.tencent.polaris.api.pojo;

import java.util.Set;

public interface ServiceEventKeysProvider {

    /**
     * 是否优先使用本地缓存
     *
     * @return boolean
     */
    boolean isUseCache();

    /**
     * 获取eventKeys集合
     *
     * @return 集合
     */
    Set<ServiceEventKey> getSvcEventKeys();

    /**
     * 获取单个eventKey
     *
     * @return 单个
     */
    ServiceEventKey getSvcEventKey();
}
