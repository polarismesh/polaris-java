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

package com.tencent.polaris.api.plugin.server;


import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.ServiceEventKey;

/**
 * 服务变更事件
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class ServerEvent {

    /**
     * 获取服务标识
     */
    private final ServiceEventKey serviceEventKey;

    /**
     * 获取泛型的值
     */
    private final Object value;

    /**
     * 获取错误信息，只有当出错的时候才返回
     */
    private final PolarisException error;

    public ServerEvent(ServiceEventKey serviceEventKey, Object value, PolarisException error) {
        this.serviceEventKey = serviceEventKey;
        this.value = value;
        this.error = error;
    }

    public ServiceEventKey getServiceEventKey() {
        return serviceEventKey;
    }

    public Object getValue() {
        return value;
    }

    public PolarisException getError() {
        return error;
    }
}
