package com.tencent.polaris.plugins.loadbalancer.shortestresponsetime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

public class ShortestResponseTimeLoadBalanceConfig implements Verifier {

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private long slidePeriod;

    public long getSlidePeriod() {
        return slidePeriod;
    }

    public void setSlidePeriod(long slidePeriod) {
        this.slidePeriod = slidePeriod;
    }

    @Override
    public void verify() {
        ConfigUtils.validateInterval(slidePeriod, "slidePeriod");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof ShortestResponseTimeLoadBalanceConfig) {
            ShortestResponseTimeLoadBalanceConfig defaultConfig = (ShortestResponseTimeLoadBalanceConfig) defaultObject;
            if (slidePeriod == 0) {
                slidePeriod = defaultConfig.getSlidePeriod();
            }
        }
    }
}
