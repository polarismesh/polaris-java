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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.configuration.client.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * 配置监听画像上报插件配置。
 *
 * @author polaris
 */
public class ConfigWatchReporterConfig implements Verifier {

    @JsonProperty
    private Boolean enable;

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "clientReporter.plugin.config-watch.enable");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            ConfigWatchReporterConfig config = (ConfigWatchReporterConfig) defaultObject;
            if (null == enable) {
                setEnable(config.isEnable());
            }
        }
    }

    public boolean isEnable() {
        if (null == enable) {
            return false;
        }
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public String toString() {
        return "ConfigWatchReporterConfig{" +
                "enable=" + enable +
                '}';
    }
}
