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

package com.tencent.polaris.plugins.connector.consul.service.lane.entity;

import java.sql.Timestamp;

/**
 * 泳道
 * User: MackZhang
 * Date: 2020/1/15
 */
public class LaneGroup {

    private String laneGroupId;

    /**
     * 泳道ID
     */
    private String laneId;

    /**
     * 部署组ID
     */
    private String groupId;

    private String groupName;

    /**
     * 应用ID
     */
    private String applicationId;

    private String applicationName;

    private String namespaceId;

    private String namespaceName;

    /**
     * 是否入口应用
     */
    private boolean entrance;

    /**
     * 1 已删除/ 2 运行中 / 3 停止
     */
    private Integer status;

    /**
     * 规则创建时间
     */
    private Timestamp createTime;

    /**
     * 规则更新时间
     */
    private Timestamp updateTime;

    public String getLaneGroupId() {
        return laneGroupId;
    }

    public void setLaneGroupId(final String laneGroupId) {
        this.laneGroupId = laneGroupId;
    }

    public String getLaneId() {
        return laneId;
    }

    public void setLaneId(final String laneId) {
        this.laneId = laneId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(final String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(final String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setNamespaceName(final String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public boolean isEntrance() {
        return entrance;
    }

    public void setEntrance(final boolean entrance) {
        this.entrance = entrance;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public Timestamp getCreateTime() {
        return createTime;
    }

    public void setCreateTime(final Timestamp createTime) {
        this.createTime = createTime;
    }

    public Timestamp getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(final Timestamp updateTime) {
        this.updateTime = updateTime;
    }
}
