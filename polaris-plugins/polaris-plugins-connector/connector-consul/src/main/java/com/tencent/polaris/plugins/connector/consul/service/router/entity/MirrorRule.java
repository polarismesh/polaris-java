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

package com.tencent.polaris.plugins.connector.consul.service.router.entity;

import java.io.Serializable;

/**
 * TSF 流量镜像规则项实体
 *
 * @author Haotian Zhang
 */
public class MirrorRule implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -1886125299472426511L;

    /**
     * 是否开启流量镜像
     */
    private Boolean enabled;

    /**
     * 目标部署组id
     */
    private String destinationDeployGroup;

    /**
     * 应用 id
     */
    private String applicationId;

    /**
     * 镜像流量百分比
     */
    private Float mirrorPercentage;

    /**
     * 镜像规则id
     */
    private String mirrorId;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDestinationDeployGroup() {
        return destinationDeployGroup;
    }

    public void setDestinationDeployGroup(String destinationDeployGroup) {
        this.destinationDeployGroup = destinationDeployGroup;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public Float getMirrorPercentage() {
        return mirrorPercentage;
    }

    public void setMirrorPercentage(Float mirrorPercentage) {
        this.mirrorPercentage = mirrorPercentage;
    }

    public String getMirrorId() {
        return mirrorId;
    }

    public void setMirrorId(String mirrorId) {
        this.mirrorId = mirrorId;
    }

    @Override
    public String toString() {
        return "MirrorRule{" +
                "enabled=" + enabled +
                ", destinationDeployGroup='" + destinationDeployGroup + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", mirrorPercentage=" + mirrorPercentage +
                ", mirrorId='" + mirrorId + '\'' +
                '}';
    }
}
