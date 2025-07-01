/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugins.connector.consul.service.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * TSF标签规则实体
 *
 * @author vanqfjiang
 */
public class TagCondition implements Serializable {

    private static final long serialVersionUID = -4620595359940020010L;

    /**
     * 标签ID
     */
    private String tagId;
    /**
     * 标签类型
     * 定义在TagConstant.TYPE
     */
    private String tagType;
    /**
     * 标签名
     * 系统类型的标签名定义在TagConstant.SYSTEM_FIELD
     */
    private String tagField;
    /**
     * 标签运算符
     * 定义在TagConstant.OPERATOR
     */
    private String tagOperator;
    /**
     * 标签的被运算对象值
     */
    private String tagValue;


    @Override
    public String toString() {
        return "TagCondition{" +
                "tagId=" + tagId +
                ", tagType='" + tagType + '\'' +
                ", tagField='" + tagField + '\'' +
                ", tagOperator='" + tagOperator + '\'' +
                ", tagValue='" + tagValue + '\'' +
                '}';
    }

    /**
     * name by equals2 is avoid override hashCode
     *
     * @param other
     * @return true if equals other
     */
    public boolean equals2(TagCondition other) {
        return Objects.equals(this.tagType, other.tagType)
                && Objects.equals(this.tagField, other.tagField)
                && Objects.equals(this.tagOperator, other.tagOperator)
                && Objects.equals(this.tagValue, other.tagValue);
    }

    public String getTagId() {
        return tagId;
    }

    public void setTagId(String tagId) {
        this.tagId = tagId;
    }

    public String getTagType() {
        return tagType;
    }

    public void setTagType(String tagType) {
        this.tagType = tagType;
    }

    public String getTagField() {
        return tagField;
    }

    public void setTagField(String tagField) {
        this.tagField = tagField;
    }

    public String getTagOperator() {
        return tagOperator;
    }

    public void setTagOperator(String tagOperator) {
        this.tagOperator = tagOperator;
    }

    public String getTagValue() {
        return tagValue;
    }

    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }
}
