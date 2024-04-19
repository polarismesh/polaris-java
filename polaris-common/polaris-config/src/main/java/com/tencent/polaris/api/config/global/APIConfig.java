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

package com.tencent.polaris.api.config.global;

import com.tencent.polaris.api.config.verify.Verifier;

/**
 * api相关的配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface APIConfig extends Verifier {

    /**
     * services.global.api.timeout
     * 默认调用超时时间
     *
     * @return long, 毫秒
     */
    long getTimeout();

    /**
     * 最大重试次数，设置为0则不重试，默认10次
     *
     * @return 最大重试次数
     */
    int getMaxRetryTimes();

    /**
     * 调用失败后，自动重试的时间间隔，默认5ms
     *
     * @return long
     */
    long getRetryInterval();

    /**
     * 获取监听的网卡名
     *
     * @return 网卡名
     */
    String getBindIf();

    /**
     * 获取监听的IP地址
     *
     * @return IP地址
     */
    String getBindIP();

    /**
     * 客户端上报开关
     */
    boolean isReportEnable();

    /**
     * 客户端上报周期，默认30s
     *
     * @return long
     */
    long getReportInterval();

}
