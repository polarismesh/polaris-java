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

package com.tencent.polaris.fault.api.core;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.fault.api.rpc.FaultRequest;
import com.tencent.polaris.fault.api.rpc.FaultResponse;

import java.io.Closeable;

public interface FaultAPI extends AutoCloseable, Closeable {

    /**
     * 故障注入
     *
     * @param faultRequest 故障注入请求（服务及标签信息）
     * @return 故障注入结果
     * @throws PolarisException 异常信息
     */
    FaultResponse fault(FaultRequest faultRequest) throws PolarisException;

    /**
     * 清理并释放资源
     */
    void destroy();

    @Override
    default void close() {
        destroy();
    }
}
