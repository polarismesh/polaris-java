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

package com.tencent.polaris.api.plugin.configuration;

public class ConfigFileGroupResponse {
    private int code;
    private String message;
    private String revision;
    private ConfigFileGroup configFileGroup;

    public ConfigFileGroupResponse(int code, String message, String revision, ConfigFileGroup configFileGroup) {
        this.code = code;
        this.message = message;
        this.revision = revision;
        this.configFileGroup = configFileGroup;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public ConfigFileGroup getConfigFileGroup() {
        return configFileGroup;
    }

    public void setConfigFileGroup(ConfigFileGroup configFileGroup) {
        this.configFileGroup = configFileGroup;
    }
}
