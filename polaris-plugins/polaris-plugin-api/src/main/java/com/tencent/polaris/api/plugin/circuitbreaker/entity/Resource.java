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

package com.tencent.polaris.api.plugin.circuitbreaker.entity;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;

public interface Resource {

    /**
     * get the resource level
     *
     * @return level
     */
    Level getLevel();

    /**
     * get the service related to resource
     *
     * @return serviceKey
     */
    ServiceKey getService();

    /**
     * get the caller service name, for rule filtering
     *
     * @return serviceKey
     */
    ServiceKey getCallerService();
}
