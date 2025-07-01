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

package com.tencent.polaris.plugins.router.rule;

/**
 * 路由规则匹配类型
 *
 * @author starkwen
 * @date 2020/11/27 11:22 上午
 */
enum RuleMatchType {
    /**
     * 主调服务规则匹配
     */
    sourceRouteRuleMatch,

    /**
     * 被调服务匹配
     */
    destRouteRuleMatch,
}
