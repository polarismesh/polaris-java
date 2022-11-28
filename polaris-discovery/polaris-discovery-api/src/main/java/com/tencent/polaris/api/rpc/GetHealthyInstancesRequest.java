package com.tencent.polaris.api.rpc;

/**
 * @author lepdou 2022-04-21
 */
public class GetHealthyInstancesRequest extends GetAllInstancesRequest {

    /**
     * 是否返回熔断实例，默认不返回
     */
    private Boolean includeCircuitBreak;

    public Boolean getIncludeCircuitBreak() {
        return includeCircuitBreak;
    }

    public void setIncludeCircuitBreak(Boolean includeCircuitBreak) {
        this.includeCircuitBreak = includeCircuitBreak;
    }
}
