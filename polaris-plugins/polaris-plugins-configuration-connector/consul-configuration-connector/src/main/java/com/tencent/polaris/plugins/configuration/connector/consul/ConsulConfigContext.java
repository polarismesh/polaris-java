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

package com.tencent.polaris.plugins.configuration.connector.consul;

import com.tencent.polaris.api.config.global.ServerConnectorConfig;

/**
 * Context of consul config server connector.
 *
 * @author Haotian Zhang
 */
public class ConsulConfigContext {

    private ServerConnectorConfig connectorConfig;

    /**
     * The number of seconds to wait (or block) for watch query, defaults to 55.
     * Needs to be less than default ConsulClient (defaults to 60). To increase
     * ConsulClient timeout create a ConsulClient bean with a custom ConsulRawClient
     * with a custom HttpClient.
     */
    private int waitTime = 55;

    /**
     * The value of the fixed delay for the watch in millis. Defaults to 1000.
     */
    private int delay = 1000;

    private String aclToken = "";

    public ServerConnectorConfig getConnectorConfig() {
        return connectorConfig;
    }

    public void setConnectorConfig(ServerConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
    }

    public int getWaitTime() {
        return this.waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getDelay() {
        return this.delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getAclToken() {
        return aclToken;
    }

    public void setAclToken(String aclToken) {
        this.aclToken = aclToken;
    }

    @Override
    public String toString() {
        return "ConsulConfigContext{" +
                "connectorConfig=" + connectorConfig +
                ", waitTime=" + waitTime +
                ", delay=" + delay +
                ", aclToken='" + aclToken + '\'' +
                '}';
    }
}
