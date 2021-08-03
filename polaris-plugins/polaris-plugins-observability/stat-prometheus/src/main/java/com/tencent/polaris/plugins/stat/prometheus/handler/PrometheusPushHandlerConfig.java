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

package com.tencent.polaris.plugins.stat.prometheus.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;

public class PrometheusPushHandlerConfig implements Verifier {
    public static final String PROMETHEUS_PUSH_CONFIG_NAME = "pushgatewayConfig";

    @JsonProperty
    private String jobName;

    @JsonProperty
    private String instanceName;

    @JsonProperty
    private Long pushInterval;

    public PrometheusPushHandlerConfig() {
    }

    /**
     * 执行校验操作，参数校验失败会抛出IllegalArgumentException
     */
    @Override
    public void verify() {
        ConfigUtils.validateString(jobName, "prometheus push-gateway job name");
    }

    /**
     * 设置默认值信息
     *
     * @param defaultObject 默认值对象
     */
    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            PrometheusPushHandlerConfig config = (PrometheusPushHandlerConfig) defaultObject;
            if (null == jobName) {
                setJobName(config.getJobName());
            }
            if (null == instanceName) {
                setPushInterval(config.getPushInterval());
            }
            if (null == pushInterval) {
                setPushInterval(config.getPushInterval());
            }
        }
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public Long getPushInterval() {
        return pushInterval;
    }

    public void setPushInterval(Long pushInterval) {
        this.pushInterval = pushInterval;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }
}
