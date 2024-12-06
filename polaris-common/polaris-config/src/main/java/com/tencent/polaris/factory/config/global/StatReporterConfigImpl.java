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

package com.tencent.polaris.factory.config.global;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.global.StatReporterConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import java.util.List;

public class StatReporterConfigImpl extends PluginConfigImpl implements StatReporterConfig {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private List<String> chain;

    @Override
    public boolean isEnable() {
        if (null == enable) {
            return false;
        }
        return enable;
    }

    @Override
    public List<String> getChain() {
        return chain;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public void setChain(List<String> chain) {
        this.chain = chain;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "statReporter.enable");
        verifyPluginConfig();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            StatReporterConfig statReporterConfig = (StatReporterConfig) defaultObject;
            if (null == enable) {
                setEnable(statReporterConfig.isEnable());
            }
            if (CollectionUtils.isEmpty(chain)) {
                setChain(statReporterConfig.getChain());
            }
            if (enable) {
                setDefaultPluginConfig(statReporterConfig);
            }
        }
    }

    @Override
    public String toString() {
        return "StatReporterConfigImpl{" +
                "enable=" + enable +
                ", chain=" + chain +
                '}';
    }
}
