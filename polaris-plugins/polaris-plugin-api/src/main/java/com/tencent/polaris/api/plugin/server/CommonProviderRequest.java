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

package com.tencent.polaris.api.plugin.server;

import java.util.HashMap;
import java.util.Map;

public class CommonProviderRequest {

    private String instanceID;

    private String namespace;

    private String service;

    private String token;

    private String host;

    private int port;

    private String version;

    private String protocol;

    private Integer weight;

    private Integer priority;

    private Map<String, String> metadata;

    private Map<String, String> extendedMetadata = new HashMap<>();

    private Integer ttl;

    private TargetServer targetServer;

    private String zone;

    private String region;

    private String campus;

    private long timeoutMs;

    public String getInstanceID() {
        return instanceID;
    }

    public void setInstanceID(String instanceID) {
        this.instanceID = instanceID;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Map<String, String> getExtendedMetadata() {
        return extendedMetadata;
    }

    public void setExtendedMetadata(Map<String, String> extendedMetadata) {
        this.extendedMetadata = extendedMetadata;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(TargetServer targetServer) {
        this.targetServer = targetServer;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String toString() {
        return "CommonProviderRequest{" +
                "instanceID='" + instanceID + '\'' +
                ", namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", token='" + token + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", version='" + version + '\'' +
                ", protocol='" + protocol + '\'' +
                ", weight=" + weight +
                ", priority=" + priority +
                ", metadata=" + metadata +
                ", extendedMetadata=" + extendedMetadata +
                ", ttl=" + ttl +
                ", targetServer=" + targetServer +
                ", zone='" + zone + '\'' +
                ", region='" + region + '\'' +
                ", campus='" + campus + '\'' +
                ", timeoutMs=" + timeoutMs +
                '}';
    }
}
