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
 * 基线泳道选择模式
 * @author Yuwei Fu
 */
public enum BaseLaneMode {
    // 仅选择无泳道标签的实例（排除所有带泳道标签的实例）
    ONLY_UNTAGGED_INSTANCE,
    // 仅选择不属于任何已启用泳道的实例（排除属于已启用泳道的实例）
    EXCLUDE_ENABLED_LANE_INSTANCE
}
