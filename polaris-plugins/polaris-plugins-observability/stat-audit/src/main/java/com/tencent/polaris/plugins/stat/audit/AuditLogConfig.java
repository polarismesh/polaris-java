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

package com.tencent.polaris.plugins.stat.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * Audit log plugin configuration, mapped under
 * {@code global.statReporter.plugin.auditLog}.
 *
 * @author Yuwei Fu
 */
public class AuditLogConfig implements Verifier {

    public static final String DEFAULT_FORMAT = "json";

    @JsonProperty
    private Boolean enable;

    @JsonProperty
    private String format;

    public boolean isEnable() {
        if (null == enable) {
            return false;
        }
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    @Override
    public void verify() {
        ConfigUtils.validateNull(enable, "auditLog.enable");
        if (isEnable() && StringUtils.isBlank(format)) {
            format = DEFAULT_FORMAT;
        }
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof AuditLogConfig) {
            AuditLogConfig config = (AuditLogConfig) defaultObject;
            if (null == enable) {
                setEnable(config.isEnable());
            }
            if (StringUtils.isBlank(format)) {
                setFormat(config.getFormat());
            }
        }
        if (null == enable) {
            setEnable(false);
        }
        if (StringUtils.isBlank(format)) {
            setFormat(DEFAULT_FORMAT);
        }
    }

    @Override
    public String toString() {
        return "AuditLogConfig{" +
                "enable=" + enable +
                ", format='" + format + '\'' +
                '}';
    }
}
