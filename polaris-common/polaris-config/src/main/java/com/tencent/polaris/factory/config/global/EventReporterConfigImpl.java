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
import com.tencent.polaris.api.config.global.EventReporterConfig;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;

import java.util.ArrayList;
import java.util.List;

public class EventReporterConfigImpl extends PluginConfigImpl implements EventReporterConfig {

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private List<String> reporters;

    @Override
    public boolean isEnable() {
        if (null == enable) {
            return true;
        }
        return enable;
    }

    @Override
    public List<String> getReporters() {
        return reporters;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public void setReporters(List<String> reporters) {
        this.reporters = reporters;
    }

    public void verify() {
        ConfigUtils.validateNull(enable, "eventReporter.enable");
        if (isEnable()) {
            ConfigUtils.validateNull(reporters, "eventReporter.reporters");
        }
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            EventReporterConfig eventReporterConfig = (EventReporterConfig) defaultObject;
            if (null == enable) {
                setEnable(eventReporterConfig.isEnable());
            }
            if (CollectionUtils.isEmpty(reporters)) {
                reporters = new ArrayList<>(eventReporterConfig.getReporters());
            }
        }
    }

    @Override
    public String toString() {
        return "EventReporterConfigImpl{" +
                "enable=" + enable +
                ", reporters=" + reporters +
                '}';
    }
}
