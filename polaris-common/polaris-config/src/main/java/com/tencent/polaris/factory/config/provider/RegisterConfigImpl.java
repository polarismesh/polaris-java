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

package com.tencent.polaris.factory.config.provider;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.provider.RegisterConfig;
import com.tencent.polaris.factory.util.ConfigUtils;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link RegisterConfig}.
 *
 * @author Haotian Zhang
 */
public class RegisterConfigImpl implements RegisterConfig {

    private static final AtomicLong INDEX = new AtomicLong(0);

    @JsonProperty
    private String namespace;

    @JsonProperty
    private String service;

    @JsonProperty
    private String serverConnectorId;

    @JsonProperty
    private Boolean enable;

    public static void increaseIndex() {
        INDEX.incrementAndGet();
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    public String getServerConnectorId() {
        return serverConnectorId;
    }

    public void setServerConnectorId(String serverConnectorId) {
        this.serverConnectorId = serverConnectorId;
    }

    @Override
    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public void verify() {
        ConfigUtils.validateString(serverConnectorId,
                "register.serverConnectorId or registers[?].serverConnectorId");
        ConfigUtils.validateNull(enable, "register.enable or registers[?].enable");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            RegisterConfig registerConfig = (RegisterConfig) defaultObject;
            if (null == namespace) {
                setNamespace(registerConfig.getNamespace());
            }
            if (null == service) {
                setService(registerConfig.getService());
            }
            if (null == serverConnectorId) {
                long index = INDEX.get();
                if (index == 0L) {
                    setServerConnectorId(registerConfig.getServerConnectorId());
                } else {
                    setServerConnectorId(registerConfig.getServerConnectorId() + index);
                }
            }
            if (null == enable) {
                setEnable(registerConfig.isEnable());
            }
        }
    }
}
