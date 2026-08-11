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

package com.tencent.polaris.api.plugin.client;

import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.server.ReportClientRequest;

/**
 * 【扩展点接口】在 ReportClient 请求发送前定制客户端画像字段。
 *
 *
 * @author fishtailfu
 */
public interface ReportClientRequestCustomizer extends Plugin {

    /**
     * 定制当前 ReportClientRequest。
     *
     * @param request 客户端上报请求
     */
    void customize(ReportClientRequest request);
}
