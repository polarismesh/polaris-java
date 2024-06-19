package com.tencent.polaris.plugins.router.rule;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.rpc.RuleBasedRouterFailoverType;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * @author lepdou 2022-07-21
 */
public class RuleBasedRouterConfig implements Verifier {

    @JsonProperty
    private RuleBasedRouterFailoverType failoverType;

    @Override
    public void verify() {
        ConfigUtils.validateNull(failoverType, "ruleBasedRouterFailoverType");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof RuleBasedRouterConfig) {
            RuleBasedRouterConfig ruleBasedRouterConfig = (RuleBasedRouterConfig) defaultObject;
            if (failoverType == null) {
                setFailoverType(ruleBasedRouterConfig.getFailoverType());
            }
        }
    }

    public RuleBasedRouterFailoverType getFailoverType() {
        return failoverType;
    }

    public void setFailoverType(RuleBasedRouterFailoverType failoverType) {
        this.failoverType = failoverType;
    }

    @Override
    public String toString() {
        return "RuleBasedRouterConfig{" +
                "failoverType=" + failoverType +
                '}';
    }
}
