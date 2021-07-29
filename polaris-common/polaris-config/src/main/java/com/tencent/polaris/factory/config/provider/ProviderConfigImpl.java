/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.factory.config.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.provider.ProviderConfig;
import com.tencent.polaris.api.config.provider.RegisterConfig;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * 被调端配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class ProviderConfigImpl implements ProviderConfig {

    @JsonProperty
    private RateLimitConfigImpl rateLimit;

    @JsonProperty
    private RegisterConfig register;

    @Override
    public RateLimitConfigImpl getRateLimit() {
        return rateLimit;
    }

    @Override
    public RegisterConfig getRegister() {
        return register;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(rateLimit, "rateLimitConfig");
        rateLimit.verify();
        register.verify();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null == rateLimit) {
            rateLimit = new RateLimitConfigImpl();
        }
        if (null == register) {
            register = new RegisterConfigImpl();
        }
        if (null != defaultObject) {
            ProviderConfig providerConfig = (ProviderConfig) defaultObject;
            rateLimit.setDefault(providerConfig.getRateLimit());
            register.setDefault(providerConfig.getRegister());
        }

    }
}
