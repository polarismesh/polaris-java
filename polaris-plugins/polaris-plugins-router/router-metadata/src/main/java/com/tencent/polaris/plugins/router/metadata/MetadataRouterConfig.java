package com.tencent.polaris.plugins.router.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * 元数据路由的配置
 *
 * @author starkwen
 * @date 2021/2/24 下午3:26
 */
public class MetadataRouterConfig implements Verifier {

    /**
     * 可选, metadata失败降级策略
     */
    @JsonProperty
    private FailOverType metadataFailOverType;

    @Override
    public void verify() {
        ConfigUtils.validateNull(metadataFailOverType, "metadataFailOverType");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            MetadataRouterConfig metadataRouterConfig = (MetadataRouterConfig) defaultObject;
            setMetadataFailOverType(metadataRouterConfig.getMetadataFailOverType());
        }
    }

    public FailOverType getMetadataFailOverType() {
        return metadataFailOverType;
    }

    public void setMetadataFailOverType(FailOverType metadataFailoverType) {
        this.metadataFailOverType = metadataFailoverType;
    }
}
