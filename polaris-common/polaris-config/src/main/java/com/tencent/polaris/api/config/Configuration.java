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

package com.tencent.polaris.api.config;

import com.tencent.polaris.api.config.configuration.ConfigFileConfig;
import com.tencent.polaris.api.config.consumer.ConsumerConfig;
import com.tencent.polaris.api.config.global.GlobalConfig;
import com.tencent.polaris.api.config.provider.ProviderConfig;
import com.tencent.polaris.api.config.verify.Verifier;

/**
 * SDK全量配置对象
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface Configuration extends Verifier {

    /**
     * services.global前缀开头的所有配置项
     *
     * @return GlobalConfig
     */
    GlobalConfig getGlobal();

    /**
     * services.consumer前缀开头的所有配置项
     *
     * @return ConsumerConfig
     */
    ConsumerConfig getConsumer();

    /**
     * services.provider前缀开头的所有配置项
     *
     * @return ProviderConfig
     */
    ProviderConfig getProvider();

    /**
     * configFile 前缀开头的所有配置项
     * @return ConfigFileConfig
     */
    ConfigFileConfig getConfigFile();
}
