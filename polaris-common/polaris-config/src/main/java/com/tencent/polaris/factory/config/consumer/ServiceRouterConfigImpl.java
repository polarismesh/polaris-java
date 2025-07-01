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

package com.tencent.polaris.factory.config.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.consumer.ServiceRouterConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import java.util.List;

/**
 * 服务路由相关配置项
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public class ServiceRouterConfigImpl extends PluginConfigImpl implements ServiceRouterConfig {

    @JsonProperty
    private List<String> afterChain;

    @JsonProperty
    private List<String> beforeChain;

    @JsonProperty
    private List<String> chain;

    @Override
    public List<String> getChain() {
        return chain;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }

    @Override
    public List<String> getAfterChain() {
        return afterChain;
    }

    public void setAfterChain(List<String> afterChain) {
        this.afterChain = afterChain;
    }

    @Override
    public List<String> getBeforeChain() {
        return beforeChain;
    }

    public void setBeforeChain(List<String> beforeChain) {
        this.beforeChain = beforeChain;
    }

    @Override
    public void verify() {
        if (CollectionUtils.isEmpty(beforeChain)) {
            throw new IllegalArgumentException("beforeChain cannot be empty");
        }
        if (CollectionUtils.isEmpty(afterChain)) {
            throw new IllegalArgumentException("afterChain cannot be empty");
        }
        verifyPluginConfig();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            ServiceRouterConfig serviceRouterConfig = (ServiceRouterConfig) defaultObject;
            if (CollectionUtils.isEmpty(beforeChain)) {
                setBeforeChain(serviceRouterConfig.getBeforeChain());
            }
            if (CollectionUtils.isEmpty(chain)) {
                setChain(serviceRouterConfig.getChain());
            }
            if (CollectionUtils.isEmpty(afterChain)) {
                setAfterChain(serviceRouterConfig.getAfterChain());
            }
            setDefaultPluginConfig(serviceRouterConfig);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ServiceRouterConfigImpl{" +
                "afterChain=" + afterChain +
                ", beforeChain=" + beforeChain +
                ", chain=" + chain +
                "} " + super.toString();
    }
}
