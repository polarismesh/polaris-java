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

package com.tencent.polaris.api.config.consumer;

import com.tencent.polaris.api.config.plugin.PluginConfig;
import com.tencent.polaris.api.config.verify.Verifier;
import java.util.List;

/**
 * 熔断相关的配置项
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface CircuitBreakerConfig extends PluginConfig, Verifier {

    /**
     * 是否启用熔断
     *
     * @return boolean
     */
    boolean isEnable();

    /**
     * 熔断器插件链
     *
     * @return 插件链名字
     */
    List<String> getChain();

    /**
     * 熔断器定时检测时间
     *
     * @return 检测时间间隔
     */
    long getCheckPeriod();

    /**
     * 熔断周期，被熔断后多久可以变为半开
     *
     * @return 熔断周期
     */
    long getSleepWindow();

    /**
     * 半开状态后最多分配多少个探测请求
     *
     * @return 探测请求数
     */
    int getRequestCountAfterHalfOpen();

    /**
     * 半开状态后多少个成功请求则恢复
     *
     * @return 半开成功数
     */
    int getSuccessCountAfterHalfOpen();

    /**
     * 熔断规则远程拉取开关
     *
     * @return true if 启用远程拉取
     */
    boolean isEnableRemotePull();

    /**
     * 熔断计数器淘汰时长
     * @return 0 if 用不淘汰
     */
    long getCountersExpireInterval();


}
