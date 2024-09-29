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

package com.tencent.polaris.api.plugin.lossless;

import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.SortableAware;
import com.tencent.polaris.api.pojo.BaseInstance;

public interface LosslessPolicy extends Plugin, SortableAware {

    /**
     * 上线状态的KEY
     */
    String CTX_KEY_REGISTER_STATUS = "key-register-status";

    String CTX_KEY_REGISTER_TIMESTAMP = "key-register-timestamp";

    String EVENT_DIRECT_REGISTER = "DirectRegister";

    String EVENT_LOSSLESS_REGISTER = "LosslessRegister";

    String EVENT_LOSSLESS_DEREGISTER = "LosslessDeregister";

    String EVENT_LOSSLESS_DELAY_REGISTER_START = "LosslessDelayRegisterStart";

    String EVENT_LOSSLESS_WARMUP_START = "LosslessWarmupStart";

    String EVENT_LOSSLESS_WARMUP_END = "LosslessWarmupEnd";

    String READINESS_PATH = "/readiness";

    @Deprecated
    String DEPRECATED_READINESS_PATH = "/online";

    String OFFLINE_PATH = "/offline";

    String REPS_TEXT_ONLY_LOCALHOST = "only localhost can call this path";

    String REPS_TEXT_NO_ACTION = "no action";

    String REPS_TEXT_NO_POLICY = "no policy";

    String REPS_TEXT_NO_INSTANCE_NEED_OFFLINE = "no instance need offline";

    String REPS_TEXT_NO_INSTANCE_NEED_READINESS_CHECK = "no instance need readiness check";

    String REPS_TEXT_OK = "ok";

    String REPS_TEXT_FAILED = "failed";

    /**
     * build or modify the instance properties
     *
     * @param instanceProperties properties, for the callback register to set into instance
     */
    void buildInstanceProperties(InstanceProperties instanceProperties);

    /**
     * do lossless register
     *
     * @param instance           instance to lossless register
     * @param instanceProperties properties, for the callback register to set into instance
     */
    void losslessRegister(BaseInstance instance, InstanceProperties instanceProperties);

    /**
     * do lossless deregister
     *
     * @param instance instance to lossless deregister
     */
    void losslessDeregister(BaseInstance instance);
}
