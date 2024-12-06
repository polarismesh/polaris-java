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
import java.util.List;

/**
 * 泳道
 * User: MackZhang
 * Date: 2020/1/14
 */
public class LaneInfo {

    /**
     * 泳道ID
     */
    private String laneId;

    /**
     * 泳道名称
     */
    private String laneName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 规则创建时间
     */
    private Timestamp createTime;

    /**
     * 规则更新时间
     */
    private Timestamp updateTime;

    /**
     * 泳道部署组信息
     */
    private List<LaneGroup> laneGroupList;

    public String getLaneId() {
        return laneId;
    }

    public void setLaneId(final String laneId) {
        this.laneId = laneId;
    }

    public String getLaneName() {
        return laneName;
    }

    public void setLaneName(final String laneName) {
        this.laneName = laneName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public List<LaneGroup> getLaneGroupList() {
        return laneGroupList;
    }

    public void setLaneGroupList(final List<LaneGroup> laneGroupList) {
        this.laneGroupList = laneGroupList;
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
