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

package com.tencent.polaris.plugins.connector.consul.service.lane.entity;

import java.sql.Timestamp;

/**
 * 泳道规则标签
 * <p>
 * User: MackZhang
 * Date: 2020/1/15
 */
public class LaneRuleTag {

    private String tagId;

    private String tagName;

    private String tagOperator;

    private String tagValue;

    private String laneRuleId;

    /**
     * 规则创建时间
     */
    private Timestamp createTime;

    /**
     * 规则更新时间
     */
    private Timestamp updateTime;

    public String getTagId() {
        return tagId;
    }

    public void setTagId(final String tagId) {
        this.tagId = tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(final String tagName) {
        this.tagName = tagName;
    }

    public String getTagOperator() {
        return tagOperator;
    }

    public void setTagOperator(final String tagOperator) {
        this.tagOperator = tagOperator;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(final String tagValue) {
        this.tagValue = tagValue;
    }

    public String getLaneRuleId() {
        return laneRuleId;
    }

    public void setLaneRuleId(final String laneRuleId) {
        this.laneRuleId = laneRuleId;
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
