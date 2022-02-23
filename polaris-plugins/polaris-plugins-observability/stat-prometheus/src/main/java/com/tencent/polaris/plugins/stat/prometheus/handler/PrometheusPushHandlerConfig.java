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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

public class PrometheusPushHandlerConfig implements Verifier {

    @JsonProperty
    private String pushgatewayAddress;

    @JsonProperty
    private String pushgatewayService;

    @JsonProperty
    private String pushgatewayNamespace;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long pushInterval;

    public PrometheusPushHandlerConfig() {
    }

    /**
     * 执行校验操作，参数校验失败会抛出IllegalArgumentException
     */
    @Override
    public void verify() {
        if (StringUtils.isBlank(pushgatewayAddress) && (StringUtils.isBlank(pushgatewayNamespace) || StringUtils
                .isBlank(pushgatewayService))) {
            throw new IllegalArgumentException("both pushgatewayAddress and pushgatewayService are empty");
        }
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
            if (StringUtils.isBlank(pushgatewayAddress)) {
                setPushgatewayAddress(config.getPushgatewayAddress());
            }
            if (null == pushInterval) {
                setPushInterval(config.getPushInterval());
            }
        }
    }

    public Long getPushInterval() {
        return pushInterval;
    }

    public void setPushInterval(Long pushInterval) {
        this.pushInterval = pushInterval;
    }

    public String getPushgatewayAddress() {
        return pushgatewayAddress;
    }

    public void setPushgatewayAddress(String pushgatewayAddress) {
        this.pushgatewayAddress = pushgatewayAddress;
    }

    public String getPushgatewayService() {
        return pushgatewayService;
    }

    public void setPushgatewayService(String pushgatewayService) {
        this.pushgatewayService = pushgatewayService;
    }

    public String getPushgatewayNamespace() {
        return pushgatewayNamespace;
    }

    public void setPushgatewayNamespace(String pushgatewayNamespace) {
        this.pushgatewayNamespace = pushgatewayNamespace;
    }
}
