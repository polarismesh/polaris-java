package com.tencent.polaris.plugins.connector.consul.service.circuitbreaker.entity;

/**
 * 熔断级别
 *
 * @author zhixinzxliu
 */
public enum TsfCircuitBreakerIsolationLevelEnum {
    // 服务级别熔断
    SERVICE,

    // API级别熔断
    API,

    // 实例级别熔断
    INSTANCE
}
