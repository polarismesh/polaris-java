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

package com.tencent.polaris.plugins.connector.consul.service.common;

import java.util.List;

/**
 * 标签规则
 *
 * @author vanqfjiang
 */
public class TagRule {
    /**
     * 规则ID
     */
    private String id;
    /**
     * 规则名
     */
    private String name;
    /**
     * 规则引用的标签列表
     */
    private List<TagCondition> conditions;
    /**
     * 规则运算表达式 only support {0} AND {1} AND ...
     */
    private String conditionExpression;

    @Override
    public String toString() {
        return String.format("TagRule{id=%s, name=%s, condition=%s, expression=%s}", this.id, this.name,
                this.conditions, this.conditionExpression);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TagCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<TagCondition> conditions) {
        this.conditions = conditions;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public void setConditionExpression(String conditionExpression) {
        this.conditionExpression = conditionExpression;
    }
}
