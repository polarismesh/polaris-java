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

public enum MetadataType {

    /**
     * 消息标签：标签来源于消息，并且后续透传也会在原始消息中直接透传，不会进行封装。
     */
    MESSAGE,

    /**
     * 应用标签：标签来源于应用自身，比如主调IP，主调服务名等信息。
     */
    APPLICATION,

    /**
     * 自定义标签：标签来源于用户业务代码自己设置。
     */
    CUSTOM
}
