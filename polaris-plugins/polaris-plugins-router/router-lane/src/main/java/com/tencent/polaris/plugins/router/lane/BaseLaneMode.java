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

package com.tencent.polaris.plugins.router.lane;

/**
 * 基线泳道判断模式
 * @author Yuwei Fu
 */
public enum BaseLaneMode {
    // 实例没有lane标签
    LANE_TAG_NOT_EXIST,
    // 实例lane标签没匹配到已启用的泳道
    LANE_TAG_NOT_MATCH
}
