/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.polaris.api.config.provider.AuthConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

import java.util.List;

public class AuthConfigImpl implements AuthConfig {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private List<String> chain;

    @Override
    public boolean isEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    @Override
    public List<String> getChain() {
        return chain;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "provider.auth.enable");
        if (CollectionUtils.isEmpty(chain)) {
            throw new IllegalArgumentException("provider.auth.chain must not be empty");
        }
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            AuthConfig authConfig = (AuthConfig) defaultObject;
            if (null == enable) {
                setEnable(authConfig.isEnable());
            }
            if (CollectionUtils.isEmpty(chain)) {
                setChain(authConfig.getChain());
            }
        }
    }

    @Override
    public String toString() {
        return "AuthConfigImpl{" +
                "enable=" + enable +
                ", chain=" + chain +
                '}';
    }
}
