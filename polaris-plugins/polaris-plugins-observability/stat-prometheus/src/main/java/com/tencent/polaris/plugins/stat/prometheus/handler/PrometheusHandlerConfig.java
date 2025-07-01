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

package com.tencent.polaris.plugins.stat.prometheus.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wallezhang
 */
public class PrometheusHandlerConfig implements Verifier {

    @JsonProperty
    private String path = "/metrics";

    @JsonProperty
    private String type;

    @JsonProperty
    private String address;

    @JsonProperty
    private String namespace;

    @JsonProperty
    private String service;

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long pushInterval = 10000L;

    @JsonProperty
    private Boolean openGzip = false;

    @JsonProperty
    private List<String> pathRegexList = new ArrayList<>();

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
            if (StringUtils.isBlank(namespace)) {
                setNamespace(config.getNamespace());
            }
            if (StringUtils.isBlank(service)) {
                setService(config.getService());
            }
        }
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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public List<String> getPathRegexList() {
        return pathRegexList;
    }

    public void setPathRegexList(List<String> pathRegexList) {
        this.pathRegexList = pathRegexList;
    }

    @Override
    public String toString() {
        return "PrometheusHandlerConfig{" +
                "path='" + path + '\'' +
                ", type='" + type + '\'' +
                ", address='" + address + '\'' +
                ", namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", pushInterval=" + pushInterval +
                ", openGzip=" + openGzip +
                ", pathRegexList=" + pathRegexList +
                '}';
    }
}
