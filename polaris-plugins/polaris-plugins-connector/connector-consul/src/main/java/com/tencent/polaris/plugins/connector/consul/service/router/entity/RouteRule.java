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
import java.util.List;

/**
 * TSF 路由规则项实体
 *
 * @author jingerzhang
 */
public class RouteRule implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -1886125299472426511L;

    /**
     * 空构造函数
     */
    public RouteRule() {
    }

    /**
     * 路由规则项ID
     */
    private String routeRuleId;

    /**
     * 路由规则项所属路由规则ID
     */
    private String routeId;

    /**
     * 路由规则项包含的匹配条件列表
     */
    private List<RouteTag> tagList;

    /**
     * 路由规则项包含的目的列表
     */
    private List<RouteDest> destList;

    public String getRouteRuleId() {
        return routeRuleId;
    }

    public void setRouteRuleId(String routeRuleId) {
        this.routeRuleId = routeRuleId;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public List<RouteTag> getTagList() {
        return tagList;
    }

    public void setTagList(List<RouteTag> tagList) {
        this.tagList = tagList;
    }

    public List<RouteDest> getDestList() {
        return destList;
    }

    public void setDestList(List<RouteDest> destList) {
        this.destList = destList;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("RouteRule{");
        sb.append("routeRuleId='").append(routeRuleId).append('\'');
        sb.append(", routeId='").append(routeId).append('\'');
        sb.append(", tagList=").append(tagList);
        sb.append(", destList=").append(destList);
        sb.append('}');
        return sb.toString();
    }
}
