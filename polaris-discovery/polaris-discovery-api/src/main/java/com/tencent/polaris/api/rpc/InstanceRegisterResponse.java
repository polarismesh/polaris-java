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

package com.tencent.polaris.api.rpc;

/**
 * 服务实例注册应答
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class InstanceRegisterResponse {

    private final String instanceId;

    private final boolean exists;

    public InstanceRegisterResponse(String instanceId, boolean exists) {
        this.instanceId = instanceId;
        this.exists = exists;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public boolean isExists() {
        return exists;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "InstanceRegisterResponse{" +
                "instanceId='" + instanceId + '\'' +
                ", exists=" + exists +
                '}';
    }
}
