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
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tencent.polaris.api.config.consumer.LoadBalanceConfig;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.config.plugin.PluginConfigImpl;
import com.tencent.polaris.factory.util.ConfigUtils;
import com.tencent.polaris.factory.util.TimeStrJsonDeserializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 与名字服务服务端的连接配置
 *
 * @author andrewshan, Haotian Zhang
 */
public class ServerConnectorConfigImpl extends PluginConfigImpl implements ServerConnectorConfig {

    private static final Pattern addressPattern = Pattern.compile("([a-zA-Z\\d]+(:)[a-zA-Z\\d~!@&%#_]+@)?(.*)(:)\\d+$");
    private final Map<String, String> metadata = new ConcurrentHashMap<>();
    @JsonProperty
    private List<String> addresses;
    @JsonProperty
    private String lbPolicy = LoadBalanceConfig.LOAD_BALANCE_ROUND_ROBIN;
    @JsonProperty
    private String protocol;
    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long connectTimeout;
    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long messageTimeout;
    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long serverSwitchInterval;
    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long connectionIdleTimeout;
    @JsonProperty
    @JsonDeserialize(using = TimeStrJsonDeserializer.class)
    private Long reconnectInterval;
    @JsonProperty
    private String trustedCAFile;
    @JsonProperty
    private String certFile;
    @JsonProperty
    private String keyFile;
    @JsonProperty
    private String id = "polaris";
    @JsonProperty
    private String token;

    @Override
    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    @Override
    public String getLbPolicy() {
        return lbPolicy;
    }

    public void setLbPolicy(String lbPolicy) {
        this.lbPolicy = lbPolicy;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    @Override
    public long getConnectTimeout() {
        if (null == connectTimeout) {
            return 0;
        }
        return connectTimeout;
    }

    public void setConnectTimeout(Long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Override
    public long getMessageTimeout() {
        if (null == messageTimeout) {
            return 0;
        }
        return messageTimeout;
    }

    public void setMessageTimeout(Long messageTimeout) {
        this.messageTimeout = messageTimeout;
    }

    @Override
    public long getServerSwitchInterval() {
        if (null == serverSwitchInterval) {
            return 0;
        }
        return serverSwitchInterval;
    }

    public void setServerSwitchInterval(Long serverSwitchInterval) {
        this.serverSwitchInterval = serverSwitchInterval;
    }

    @Override
    public long getConnectionIdleTimeout() {
        if (null == connectionIdleTimeout) {
            return 0;
        }
        return connectionIdleTimeout;
    }

    public void setConnectionIdleTimeout(Long connectionIdleTimeout) {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }

    @Override
    public long getReconnectInterval() {
        if (null == reconnectInterval) {
            return 0;
        }
        return reconnectInterval;
    }

    public void setReconnectInterval(Long reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }

    @Override
    public Map<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTrustedCAFile() {
        return trustedCAFile;
    }

    @Override
    public String getCertFile() {
        return certFile;
    }

    @Override
    public String getKeyFile() {
        return keyFile;
    }

    public void setTrustedCAFile(String trustedCAFile) {
        this.trustedCAFile = trustedCAFile;
    }

    public void setCertFile(String certFile) {
        this.certFile = certFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void verify() {
        if (CollectionUtils.isEmpty(addresses)) {
            throw new IllegalArgumentException(String.format("addresses of [%s] must not be empty", protocol));
        }
        for (String address : addresses) {
            boolean matched = addressPattern.matcher(address).find();
            if (!matched) {
                throw new IllegalArgumentException(String.format("address [%s] of [%s] is invalid", address, protocol));
            }
        }
        List<String> targetLbPolicyList = new ArrayList<>();
        targetLbPolicyList.add(LoadBalanceConfig.LOAD_BALANCE_ROUND_ROBIN);
        targetLbPolicyList.add(LoadBalanceConfig.LOAD_BALANCE_NEARBY_BACKUP);
        ConfigUtils.validateIn(lbPolicy, targetLbPolicyList, "serverConnector.lbPolicy");
        ConfigUtils.validateString(id, "serverConnector.id");
        if (DefaultPlugins.SERVER_CONNECTOR_GRPC.equals(protocol)) {
            ConfigUtils.validateString(protocol, "serverConnector.protocol");
            ConfigUtils.validateInterval(connectTimeout, "serverConnector.connectTimeout");
            ConfigUtils.validateInterval(messageTimeout, "serverConnector.messageTimeout");
            ConfigUtils.validateInterval(serverSwitchInterval, "serverConnector.serverSwitchInterval");
            ConfigUtils.validateInterval(connectionIdleTimeout, "serverConnector.connectionIdleTimeout");
            ConfigUtils.validateInterval(reconnectInterval, "serverConnector.reconnectInterval");
        }
        verifyPluginConfig();
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (null != defaultObject) {
            ServerConnectorConfig serverConnectorConfig = (ServerConnectorConfig) defaultObject;
            if (null == addresses) {
                setAddresses(serverConnectorConfig.getAddresses());
            }
            if (StringUtils.isBlank(lbPolicy)) {
                setLbPolicy(LoadBalanceConfig.LOAD_BALANCE_ROUND_ROBIN);
            }
            if (null == protocol) {
                setProtocol(serverConnectorConfig.getProtocol());
            }
            if (null == connectTimeout) {
                setConnectTimeout(serverConnectorConfig.getConnectTimeout());
            }
            if (null == messageTimeout) {
                setMessageTimeout(serverConnectorConfig.getMessageTimeout());
            }
            if (null == serverSwitchInterval) {
                setServerSwitchInterval(serverConnectorConfig.getServerSwitchInterval());
            }
            if (null == connectionIdleTimeout) {
                setConnectionIdleTimeout(serverConnectorConfig.getConnectionIdleTimeout());
            }
            if (null == reconnectInterval) {
                setReconnectInterval(serverConnectorConfig.getReconnectInterval());
            }
            setDefaultPluginConfig(serverConnectorConfig);
        }
    }

    @Override
    public String toString() {
        return "ServerConnectorConfigImpl{" +
                "metadata=" + metadata +
                ", addresses=" + addresses +
                ", protocol='" + protocol + '\'' +
                ", connectTimeout=" + connectTimeout +
                ", messageTimeout=" + messageTimeout +
                ", serverSwitchInterval=" + serverSwitchInterval +
                ", connectionIdleTimeout=" + connectionIdleTimeout +
                ", reconnectInterval=" + reconnectInterval +
                ", trustedCAFile='" + trustedCAFile + '\'' +
                ", certFile='" + certFile + '\'' +
                ", keyFile='" + keyFile + '\'' +
                ", id='" + id + '\'' +
                ", token='" + token + '\'' +
                '}';
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

}
