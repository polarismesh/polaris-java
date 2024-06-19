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
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author wallezhang
 */
public class PrometheusHandlerConfig implements Verifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusHandlerConfig.class);

    public static final Integer DEFAULT_MIN_PULL_PORT = 28080;

    public static final Integer DEFAULT_MAX_PULL_PORT = DEFAULT_MIN_PULL_PORT + 10;

    @JsonProperty
    private String host = "0.0.0.0";

    @JsonProperty
    private Integer port = DEFAULT_MIN_PULL_PORT;

    @JsonProperty
    private String path = "/metrics";

    @JsonProperty
    private String type;

    @JsonProperty
    private String address;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long pushInterval = 10000L;

    @JsonProperty
    private Boolean openGzip = false;

    public PrometheusHandlerConfig() {
    }

    /**
     * 执行校验操作，参数校验失败会抛出IllegalArgumentException
     */
    @Override
    public void verify() {

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
            if (StringUtils.isBlank(type)) {
                setType(config.getType());
            }
            if (StringUtils.isBlank(host)) {
                setHost(config.getHost());
            }
            if (port == null) {
                setPort(config.getPort());
            }
            if (StringUtils.isBlank(path)) {
                setPath(config.getPath());
            }
            if (StringUtils.isBlank(address)) {
                setAddress(config.getAddress());
            }
            if (null == pushInterval) {
                if (config.getPushInterval() != null) {
                    setPushInterval(config.getPushInterval());
                } else {
                    setPushInterval(10000L);
                }
            }
            if (null == openGzip) {
                if (config.isOpenGzip() != null) {
                    setOpenGzip(config.isOpenGzip());
                } else {
                    setOpenGzip(false);
                }
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getPushInterval() {
        return pushInterval;
    }

    public void setPushInterval(Long pushInterval) {
        this.pushInterval = pushInterval;
    }

    public Boolean isOpenGzip() {
        return openGzip;
    }

    public void setOpenGzip(Boolean openGzip) {
        this.openGzip = openGzip;
    }

    @Override
    public String toString() {
        return "PrometheusHandlerConfig{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", path='" + path + '\'' +
                ", type='" + type + '\'' +
                ", address='" + address + '\'' +
                ", pushInterval=" + pushInterval +
                ", openGzip=" + openGzip +
                '}';
    }
}
