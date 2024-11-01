/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 *  Licensed under the BSD 3-Clause License (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.api.plugin.event;

/**
 * @author Haotian Zhang
 */
public class FlowEventConstants {

    public enum Status {
        // circuit breaker status
        OPEN,
        CLOSE,
        HALF_OPEN,
        DESTROY,

        // rate limiter status
        UNLIMITED,
        LIMITED,

        UNKNOWN,
    }

    public enum ResourceType {
        SERVICE,
        METHOD,
        INSTANCE,

        // rate limiter resource
        QPS,
        CONCURRENCY,

        UNKNOWN,
    }

}
