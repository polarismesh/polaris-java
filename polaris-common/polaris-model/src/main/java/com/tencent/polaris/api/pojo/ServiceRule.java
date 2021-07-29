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

/**
 * 服务规则封装类
 */
public interface ServiceRule {

    /**
     * 获取具体的规则值
     *
     * @return 规则对象
     */
    Object getRule();

    /**
     * 服务实例列表是否已经加载
     *
     * @return 加载标识
     */
    boolean isInitialized();

    /**
     * 获取唯一标识信息
     *
     * @return revision
     */
    String getRevision();
}
