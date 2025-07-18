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

package com.tencent.polaris.plugins.outlier.detector.udp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

/**
 * TCP网络探测配置
 *
 * @author lambdaliu
 * @date 2022/7/26
 */
public class Config implements Verifier {

    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long timeout;

    @JsonProperty
    private String send;

    @JsonProperty
    private String receive;

    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public String getSend() {
        return send;
    }

    public void setSend(String send) {
        this.send = send;
    }

    public String getReceive() {
        return receive;
    }

    public void setReceive(String receive) {
        this.receive = receive;
    }

    @Override
    public void verify() {
        ConfigUtils.validateInterval(timeout, "udp.timeout");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            Config config = (Config) defaultObject;
            if (null == timeout) {
                setTimeout(config.getTimeout());
            }
            if (null == send) {
                setSend(config.getSend());
            }
            if (null == receive) {
                setReceive(config.getReceive());
            }
        }
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "Config{" +
                "timeout=" + timeout +
                "send=" + send +
                "receive=" + receive +
                '}';
    }
}
