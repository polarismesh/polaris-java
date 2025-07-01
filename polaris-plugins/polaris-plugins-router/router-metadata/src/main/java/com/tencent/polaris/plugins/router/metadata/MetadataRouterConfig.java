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
            if (metadataFailOverType == null) {
                setMetadataFailOverType(metadataRouterConfig.getMetadataFailOverType());
            }
        }
    }

    public FailOverType getMetadataFailOverType() {
        return metadataFailOverType;
    }

    public void setMetadataFailOverType(FailOverType metadataFailoverType) {
        this.metadataFailOverType = metadataFailoverType;
    }

    @Override
    public String toString() {
        return "MetadataRouterConfig{" +
                "metadataFailOverType=" + metadataFailOverType +
                '}';
    }
}
