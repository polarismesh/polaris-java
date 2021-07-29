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
