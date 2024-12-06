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

/**
 * 客户端上报应答
 *
 * @author vickliu
 * @date 2019/9/22
 */
public class ReportClientResponse {

    enum RunMode {
        ModeNoAgent,        //ModeNoAgent 以no agent模式运行————SDK
        ModeWithAgent        //ModeWithAgent 带agent模式运行
    }

    private RunMode mode;
    private String version;
    private String region;
    private String zone;
    private String campus;

    public RunMode getMode() {
        return mode;
    }

    public void setMode(RunMode mode) {
        this.mode = mode;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ReportClientResponse{" +
                "mode=" + mode +
                ", version='" + version + '\'' +
                ", region='" + region + '\'' +
                ", zone='" + zone + '\'' +
                ", campus='" + campus + '\'' +
                "}" + super.toString();
    }
}
