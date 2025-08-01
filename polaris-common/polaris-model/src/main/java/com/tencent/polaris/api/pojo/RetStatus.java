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

package com.tencent.polaris.api.pojo;

/**
 * 服务调用状态
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public enum RetStatus {

    /**
     * unknown status
     */
    RetUnknown("unknown"),

    /**
     * invoke success
     */
    RetSuccess("success"),

    /**
     * invoke fail
     */
    RetFail("fail"),

    /**
     * invoke timeout
     */
    RetTimeout("timeout"),

    /**
     * reject request by circuitbreaking
     */
    RetReject("reject"),

    /**
     * request cancel by flow control
     */
    RetFlowControl("flow_control"),

    ;
    private final String desc;

    RetStatus(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
