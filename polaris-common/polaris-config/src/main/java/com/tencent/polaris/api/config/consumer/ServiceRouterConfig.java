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
 * 服务路由相关配置项
 *
 * @author andrewshan
 * @date 2019/8/20
 */
public interface ServiceRouterConfig extends PluginConfig, Verifier {

    String DEFAULT_ROUTER_ISOLATED = "isolatedRouter";

    String DEFAULT_ROUTER_RECOVER = "recoverRouter";

    String DEFAULT_ROUTER_METADATA = "metadataRouter";

    String DEFAULT_ROUTER_RULE = "ruleBasedRouter";

    String DEFAULT_ROUTER_NEARBY = "nearbyBasedRouter";

    String DEFAULT_ROUTER_SET = "setRouter";

    String DEFAULT_ROUTER_CANARY = "canaryRouter";

    String DEFAULT_ROUTER_LANE = "laneRouter";


    /**
     * services.consumer.serviceRouter.beforeChain
     * 前置路由链配置
     *
     * @return 前置路由链列表
     */
    List<String> getBeforeChain();

    /**
     * services.consumer.serviceRouter.chain
     * 路由核心链配置
     *
     * @return 核心路由链列表
     */
    List<String> getChain();

    /**
     * services.consumer.serviceRouter.afterChain
     * 后置路由链配置
     *
     * @return 后置路由链列表
     */
    List<String> getAfterChain();

}
