package com.tencent.polaris.plugins.router.rule;

/**
 * 路由执行结果
 *
 * @author starkwen
 * @date 2020/11/27 11:22 上午
 */
enum RuleStatus {
    /**
     * 无路由策略
     */
    noRule,
    /**
     * 被调服务路由策略匹配成功
     */
    destRuleSucc,
    /**
     * 被调服务路由策略匹配失败
     */
    destRuleFail,
    /**
     * 主调服务路由策略匹配成功
     */
    sourceRuleSucc,
    /**
     * 主调服务路由策略匹配失败
     */
    sourceRuleFail,
}
