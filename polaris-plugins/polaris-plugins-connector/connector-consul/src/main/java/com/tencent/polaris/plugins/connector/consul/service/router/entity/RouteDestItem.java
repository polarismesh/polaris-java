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

/**
 * TSF 路由规则路由目标匹配项实体
 *
 * @author jingerzhang
 */
public class RouteDestItem implements Serializable {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = -2276599402100262473L;

    /**
     * 空构造函数
     */
    public RouteDestItem() {
    }

    /**
     * 路由规则路由目标匹配项ID
     */
    private String routeDestItemId;

    /**
     * 所属路由规则路由目标ID
     */
    private String routeDestId;

    /**
     * 路由规则目标字段名称
     */
    private String destItemField;

    /**
     * 路由规则目标字段取值
     */
    private String destItemValue;

    public String getDestItemField() {
        return destItemField;
    }

    public void setDestItemField(String destItemField) {
        this.destItemField = destItemField;
    }

    public String getDestItemValue() {
        return destItemValue;
    }

    public void setDestItemValue(String destItemValue) {
        this.destItemValue = destItemValue;
    }

    public String getRouteDestItemId() {
        return routeDestItemId;
    }

    public void setRouteDestItemId(String routeDestItemId) {
        this.routeDestItemId = routeDestItemId;
    }

    public String getRouteDestId() {
        return routeDestId;
    }

    public void setRouteDestId(String routeDestId) {
        this.routeDestId = routeDestId;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("RouteDestItem{");
        sb.append("routeDestItemId='").append(routeDestItemId).append('\'');
        sb.append(", routeDestId='").append(routeDestId).append('\'');
        sb.append(", destItemField='").append(destItemField).append('\'');
        sb.append(", destItemValue='").append(destItemValue).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
