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

package com.tencent.polaris.api.plugin.stat;

/**
 * Stat reporter metadata info
 *
 * @author wallezhang
 */
public class ReporterMetaInfo {

    private String host;
    private Integer port;
    private String path;
    private String protocol;
    private String target;

    public String getTarget() {
        return target;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getProtocol() {
        return protocol;
    }

    public static ReporterMetaInfoBuilder builder() {
        return new ReporterMetaInfoBuilder();
    }

    public static final class ReporterMetaInfoBuilder {
        private String host;
        private Integer port;
        private String path;
        private String protocol;
        private String target;

        public ReporterMetaInfoBuilder host(String host) {
            this.host = host;
            return this;
        }

        public ReporterMetaInfoBuilder port(Integer port) {
            this.port = port;
            return this;
        }

        public ReporterMetaInfoBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ReporterMetaInfoBuilder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public ReporterMetaInfoBuilder target(String target) {
            this.target = target;
            return this;
        }

        public ReporterMetaInfo build() {
            ReporterMetaInfo reporterMetaInfo = new ReporterMetaInfo();
            reporterMetaInfo.target = this.target;
            reporterMetaInfo.host = this.host;
            reporterMetaInfo.port = this.port;
            reporterMetaInfo.path = this.path;
            reporterMetaInfo.protocol = this.protocol;
            return reporterMetaInfo;
        }
    }
}
