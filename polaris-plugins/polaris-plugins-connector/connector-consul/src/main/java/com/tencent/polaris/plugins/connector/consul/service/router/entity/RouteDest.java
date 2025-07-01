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

package com.tencent.polaris.plugins.connector.consul.service.router.entity;

import java.io.Serializable;
import java.util.List;

/**
 * TSF 路由规则目标实体
 *
 * @author jingerzhang
 */

public class RouteDest implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 984582541720418394L;

    /**
     * 空构造函数
     */
    public RouteDest() {
    }

    /**
     * 路由规则项路由目标Id
     */

    private String destId;

    /**
     * 路由目标权重
     */
    private Integer destWeight;

    /**
     * 路由目标匹配条件列表
     * <p>
     * 忽略到数据库表字段的映射
     */

    private List<RouteDestItem> destItemList;

    /**
     * 路由目标所属路由规则项Id
     */
    private String routeRuleId;

    public String getDestId() {
        return destId;
    }

    public void setDestId(String destId) {
        this.destId = destId;
    }

    public Integer getDestWeight() {
        return destWeight;
    }

    public void setDestWeight(Integer destWeight) {
        this.destWeight = destWeight;
    }

    public List<RouteDestItem> getDestItemList() {
        return destItemList;
    }

    public void setDestItemList(List<RouteDestItem> destItemList) {
        this.destItemList = destItemList;
    }

    public String getRouteRuleId() {
        return routeRuleId;
    }

    public void setRouteRuleId(String routeRuleId) {
        this.routeRuleId = routeRuleId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("RouteDest{");
        sb.append("destId='").append(destId).append('\'');
        sb.append(", destWeight=").append(destWeight);
        sb.append(", destItemList=").append(destItemList);
        sb.append(", routeRuleId='").append(routeRuleId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
