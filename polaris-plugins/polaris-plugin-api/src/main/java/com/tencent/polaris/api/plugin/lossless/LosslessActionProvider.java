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

package com.tencent.polaris.api.plugin.lossless;

import com.tencent.polaris.api.pojo.BaseInstance;

import java.util.Collection;

public interface LosslessActionProvider {

    /**
     * the key identify in ValueContext
     */
    String CTX_KEY = "key_losslessActionProvider";

    /**
     * register name
     * @return name for register
     */
   String getName();

    /**
     * do the instance register action
     * @param instanceProperties properties, for the callback register to set into instance
     */
   void doRegister(InstanceProperties instanceProperties);

    /**
     * do the instance deregister action
     */
   void doDeregister();

    /**
     * whether enable health check policy or not
     * @return enable
     */
   boolean isEnableHealthCheck();

    /**
     * do health check action
     * @return check succeed
     */
   boolean doHealthCheck();

}
