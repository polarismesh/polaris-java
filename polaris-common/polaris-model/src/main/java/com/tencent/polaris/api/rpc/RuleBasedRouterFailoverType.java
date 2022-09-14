package com.tencent.polaris.api.rpc;

/**
 * 规则路由降级策略
 * @author lepdou 2022-07-21
 */
public enum RuleBasedRouterFailoverType {
    /**
     * 默认返回全量地址。优先保证服务调用正常
     */
    all,
    /**
     * 不降级，返回空地址列表
     */
    none
}
