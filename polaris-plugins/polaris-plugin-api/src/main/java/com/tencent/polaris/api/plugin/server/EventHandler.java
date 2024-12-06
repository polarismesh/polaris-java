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

package com.tencent.polaris.api.plugin.server;

import com.tencent.polaris.api.pojo.RegistryCacheValue;

/**
 * 事件回调函数
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public interface EventHandler {

    /**
     * 服务时间变更后的回调逻辑
     *
     * @param event 服务变更事件
     * @return 是否产生服务删除事件
     */
    boolean onEventUpdate(ServerEvent event);

    /**
     * 获取当前资源的版本号
     * @return 版本号
     */
    String getRevision();

    /**
     * 获取当前资源的本地缓存
     * @return
     */
    RegistryCacheValue getValue();

}
