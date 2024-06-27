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

package com.tencent.polaris.plugins.connector.consul;

import com.ecwid.consul.v1.agent.model.NewService;
import com.tencent.polaris.api.config.global.ServerConnectorConfig;

import java.util.LinkedList;
import java.util.List;

/**
 * Context of consul server connector.
 *
 * @author Haotian Zhang
 */
public class ConsulContext {

    private ServerConnectorConfig connectorConfig;

    private String serviceName;

    private String instanceId;

    private String ipAddress;

    private boolean preferIpAddress;

    private String aclToken;

    private List<String> tags;

    private NewService.Check check;

    private String checkId;

    private String queryTag;

    private Boolean queryPassing;

    private long consulErrorSleep;

    /**
     * The number of seconds to wait (or block) for watch query, defaults to 55.
     * Needs to be less than default ConsulClient (defaults to 60). To increase
     * ConsulClient timeout create a ConsulClient bean with a custom ConsulRawClient
     * with a custom HttpClient.
     */
    private int waitTime;

    public ConsulContext() {
        serviceName = "";
        instanceId = "";
        ipAddress = "";
        aclToken = "";
        preferIpAddress = false;
        tags = new LinkedList<>();
        check = new NewService.Check();
        checkId = "";
        queryTag = "";
        queryPassing = true;
        consulErrorSleep = 60000L;
        waitTime = 55;
    }

    public ServerConnectorConfig getConnectorConfig() {
        return connectorConfig;
    }

    public void setConnectorConfig(ServerConnectorConfig connectorConfig) {
        this.connectorConfig = connectorConfig;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public boolean isPreferIpAddress() {
        return preferIpAddress;
    }

    public void setPreferIpAddress(boolean preferIpAddress) {
        this.preferIpAddress = preferIpAddress;
    }

    public String getAclToken() {
        return aclToken;
    }

    public void setAclToken(String aclToken) {
        this.aclToken = aclToken;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public NewService.Check getCheck() {
        return check;
    }

    public void setCheck(NewService.Check check) {
        this.check = check;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getQueryTag() {
        return queryTag;
    }

    public void setQueryTag(String queryTag) {
        this.queryTag = queryTag;
    }

    public Boolean getQueryPassing() {
        return queryPassing;
    }

    public void setQueryPassing(Boolean queryPassing) {
        this.queryPassing = queryPassing;
    }

    public long getConsulErrorSleep() {
        return consulErrorSleep;
    }

    public void setConsulErrorSleep(long consulErrorSleep) {
        this.consulErrorSleep = consulErrorSleep;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }
}
