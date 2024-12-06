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

import com.tencent.polaris.api.plugin.stat.ReporterMetaInfo;

import java.util.List;

/**
 * 客户端上报请求
 *
 * @author vickliu
 * @date 2019/9/22
 */
public class ReportClientRequest {

    private String namespace;

    private String service;

    private String clientHost;

    private String version;

    private TargetServer targetServer;

    private List<ReporterMetaInfo> reporterMetaInfos;

    public List<ReporterMetaInfo> getReporterMetaInfos() {
        return reporterMetaInfos;
    }

    public void setReporterMetaInfos(List<ReporterMetaInfo> reporterMetaInfos) {
        this.reporterMetaInfos = reporterMetaInfos;
    }

    public String getClientHost() {
        return clientHost;
    }

    public void setClientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public TargetServer getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(TargetServer targetServer) {
        this.targetServer = targetServer;
    }

    @Override
    public String toString() {
        return "ReportClientRequest{" +
                "namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", clientHost='" + clientHost + '\'' +
                ", version='" + version + '\'' +
                ", targetServer=" + targetServer +
                '}';
    }
}
