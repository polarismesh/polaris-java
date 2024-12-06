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

package com.tencent.polaris.plugins.configuration.connector.polaris.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author fabian4 2023-03-02
 */
public class ConfigClientResponse {

    @SerializedName("code")
    private String code;
    @SerializedName("info")
    private String info;
    @SerializedName("configFileGroup")
    private String configFileGroup;
    @SerializedName("configFile")
    private ConfigClientFile configFile;
    @SerializedName("configFileRelease")
    private ConfigClientFileRelease configFileRelease;
    @SerializedName("configFileReleaseHistory")
    private String configFileReleaseHistory;
    @SerializedName("configFileTemplate")
    private String configFileTemplate;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getConfigFileGroup() {
        return configFileGroup;
    }

    public void setConfigFileGroup(String configFileGroup) {
        this.configFileGroup = configFileGroup;
    }

    public ConfigClientFile getConfigFile() {
        return configFile;
    }

    public void setConfigFile(ConfigClientFile configFile) {
        this.configFile = configFile;
    }

    public ConfigClientFileRelease getConfigFileRelease() {
        return configFileRelease;
    }

    public void setConfigFileRelease(ConfigClientFileRelease configFileRelease) {
        this.configFileRelease = configFileRelease;
    }

    public String getConfigFileReleaseHistory() {
        return configFileReleaseHistory;
    }

    public void setConfigFileReleaseHistory(String configFileReleaseHistory) {
        this.configFileReleaseHistory = configFileReleaseHistory;
    }

    public String getConfigFileTemplate() {
        return configFileTemplate;
    }

    public void setConfigFileTemplate(String configFileTemplate) {
        this.configFileTemplate = configFileTemplate;
    }

    @Override
    public String toString() {
        return "ConfigClientResponse{" +
                "code=" + code +
                ", info='" + info + '\'' +
                ", configFileGroup='" + configFileGroup + '\'' +
                ", configFile=" + configFile +
                ", configFileRelease='" + configFileRelease + '\'' +
                ", configFileReleaseHistory='" + configFileReleaseHistory + '\'' +
                ", configFileTemplate='" + configFileTemplate + '\'' +
                '}';
    }
}

