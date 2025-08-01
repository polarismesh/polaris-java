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

package com.tencent.polaris.api.plugin.stat;

import com.tencent.polaris.api.pojo.Service;

public interface RateLimitGauge extends Service {

    enum Result {
        PASSED, LIMITED
    }

    /**
     * 获取方法名
     *
     * @return method
     */
    String getMethod();

    /**
     * 获取请求标签
     *
     * @return labels
     */
    String getLabels();

    /**
     * 获取限流结果
     *
     * @return 是否通过
     */
    Result getResult();

    /**
     * 获取生效的限流规则名称
     *
     * @return 限流规则名称
     */
    String getRuleName();
}
