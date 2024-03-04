/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
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

import com.tencent.polaris.api.rpc.RequestBaseEntity;

import java.util.List;

/**
 * Request of reporting service contract.
 *
 * @author Haotian Zhang
 */
public class ReportServiceContractRequest extends RequestBaseEntity {

    private String name;

    private String namespace;

    private String service;

    private String protocol;

    private String version;

    private String content;

    private List<InterfaceDescriptor> interfaceDescriptors;

    private String revision;

    private TargetServer targetServer;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<InterfaceDescriptor> getInterfaceDescriptors() {
        return interfaceDescriptors;
    }

    public void setInterfaceDescriptors(List<InterfaceDescriptor> interfaceDescriptors) {
        this.interfaceDescriptors = interfaceDescriptors;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(TargetServer targetServer) {
        this.targetServer = targetServer;
    }
}
