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

package com.tencent.polaris.plugins.connector.consul.service.circuitbreaker.entity;


import com.tencent.polaris.api.utils.StringUtils;

import java.io.Serializable;

/**
 * @author zhixinzxliu
 */
public class CircuitBreakerApi implements Serializable {

    private String apiId;

    private String path;

    private String method;

    private String strategyId;

    public String getApiId() {
        return apiId;
    }

    public void setApiId(final String apiId) {
        this.apiId = apiId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(final String strategyId) {
        this.strategyId = strategyId;
    }

    public boolean validate() {
        if (StringUtils.isBlank(path) || StringUtils.isBlank(method)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "CircuitBreakerApi{" +
                "apiId='" + apiId + '\'' +
                ", path='" + path + '\'' +
                ", method='" + method + '\'' +
                ", strategyId='" + strategyId + '\'' +
                '}';
    }
}
