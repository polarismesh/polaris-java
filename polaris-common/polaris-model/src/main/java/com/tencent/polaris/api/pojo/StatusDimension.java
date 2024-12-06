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

package com.tencent.polaris.api.pojo;

import com.tencent.polaris.api.utils.StringUtils;
import java.util.Objects;

/**
 * 状态查询维度
 */
public class StatusDimension {

    public enum Level {
        /**
         * 服务级，不指定主调方和接口
         */
        SERVICE,
        /**
         * 对所有主调方维度生效
         */
        ALL_CALLER,
        /**
         * 对所有接口生效
         */
        ALL_METHOD,
        /**
         * 指定主调方和接口生效
         */
        CALLER_METHOD,
    }

    public static final StatusDimension EMPTY_DIMENSION = new StatusDimension("", null);

    private final String method;

    private final ServiceKey callerService;

    public StatusDimension(String method, Service callerService) {
        if (StringUtils.isBlank(method)) {
            this.method = "";
        } else {
            this.method = method;
        }
        if (null != callerService) {
            if (StringUtils.isBlank(callerService.getNamespace()) && StringUtils.isBlank(callerService.getService())) {
                this.callerService = null;
            } else {
                this.callerService = new ServiceKey(callerService.getNamespace(), callerService.getService());
            }
        } else {
            this.callerService = null;
        }
    }

    public String getMethod() {
        return method;
    }

    public Service getCallerService() {
        return callerService;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatusDimension that = (StatusDimension) o;
        return Objects.equals(method, that.method) &&
                Objects.equals(callerService, that.callerService);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, callerService);
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "StatusDimension{" +
                "method='" + method + '\'' +
                ", callerService=" + callerService +
                '}';
    }
}
