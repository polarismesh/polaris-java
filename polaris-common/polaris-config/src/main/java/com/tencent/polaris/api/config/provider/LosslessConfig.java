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

package com.tencent.polaris.api.config.provider;

import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.specification.api.v1.traffic.manage.LosslessProto;

public interface LosslessConfig extends Verifier {

    /**
     * 是否启用无损上下线
     * @return 启用无损上下线
     */
    boolean isEnable();

    /**
     * 无损下线和获取上线状态的监听IP
     * @return host
     */
    String getHost();

    /**
     * 无损下线和获取上线状态的监听端口
     * @return port
     */
    int getPort();

    /**
     * 如果没有实现健康检查，延迟注册的时间，单位毫秒
     * @return long
     */
    long getDelayRegisterInterval();

    /**
     * 获取健康探测的间隔时间
     * @return long
     */
    long getHealthCheckInterval();

    String getType();

    LosslessProto.DelayRegister.DelayStrategy getStrategy();

    String getHealthCheckProtocol();

    String getHealthCheckPath();

    String getHealthCheckMethod();

}
