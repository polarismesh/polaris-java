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
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author wallezhang
 */
public class PrometheusHandlerConfig implements Verifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusHandlerConfig.class);
    @JsonProperty
    private String host;

    @JsonProperty
    private Integer port = 28080;

    @JsonProperty
    private String path = "/metrics";

    @JsonProperty
    private String pushgatewayAddress;

    public PrometheusHandlerConfig() {
    }

    /**
     * 执行校验操作，参数校验失败会抛出IllegalArgumentException
     */
    @Override
    public void verify() {
        if (StringUtils.isNotBlank(pushgatewayAddress)) {
            LOGGER.warn("Prometheus pushgateway stat reporter plugin name has been changed to prometheus-pushgateway.");
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
            PrometheusHandlerConfig config = (PrometheusHandlerConfig) defaultObject;
            if (StringUtils.isBlank(host)) {
                setHost(config.getHost());
            }
            if (port == null) {
                setPort(config.getPort());
            }
            if (StringUtils.isBlank(path)) {
                setPath(config.getPath());
            }
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPushgatewayAddress() {
        return pushgatewayAddress;
    }

    public void setPushgatewayAddress(String pushgatewayAddress) {
        this.pushgatewayAddress = pushgatewayAddress;
    }
}
