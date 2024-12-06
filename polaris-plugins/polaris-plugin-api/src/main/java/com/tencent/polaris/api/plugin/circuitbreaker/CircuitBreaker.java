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

package com.tencent.polaris.api.plugin.circuitbreaker;

import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.CircuitBreakerStatus;

/**
 * 【extension point】resource CircuitBreaker
 *
 * @author andrewshan
 */
public interface CircuitBreaker extends Plugin {

    /**
     * get the resource circuitbreaker status
     * @param resource resource object
     * @return status
     */
    CircuitBreakerStatus checkResource(Resource resource);

    /**
     * report resource invoke result stat
     * @param resourceStat result stat
     */
    void report(ResourceStat resourceStat);


}
