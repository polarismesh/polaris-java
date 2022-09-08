/*
 * Tencent is pleased to support the open source community by making CL5 available.
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

package com.tencent.polaris.ratelimit.api.rpc;

public interface RateLimitConsts {

    String LABEL_KEY_METHOD = "$method";

    String LABEL_KEY_HEADER = "$header.";

    String LABEL_KEY_QUERY = "$query.";

    String LABEL_KEY_CALLER_SERVICE = "$caller_service.";

    String LABEL_KEY_CALLER_IP = "$caller_ip";
}
