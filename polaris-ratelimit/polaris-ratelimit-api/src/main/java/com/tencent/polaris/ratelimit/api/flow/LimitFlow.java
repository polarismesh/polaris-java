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

package com.tencent.polaris.ratelimit.api.flow;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.flow.AbstractFlow;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;

public interface LimitFlow extends AbstractFlow {

    /**
     * 获取检查限流配额通过情况
     *
     * @param request 限流请求（服务及标签信息）
     * @return 配额通过情况
     * @throws PolarisException 异常信息
     */
    default QuotaResponse getQuota(QuotaRequest request) {
        return null;
    }

}
