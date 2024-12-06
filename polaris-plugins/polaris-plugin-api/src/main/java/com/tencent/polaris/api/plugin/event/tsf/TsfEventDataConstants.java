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

package com.tencent.polaris.api.plugin.event.tsf;

/**
 * @author Haotian Zhang
 */
public interface TsfEventDataConstants {
    Integer QUEUE_THRESHOLD = 1000;
    Integer MAX_BATCH_SIZE = 50;

    String CIRCUIT_BREAKER_EVENT_NAME = "circuit_break";
    String RATE_LIMIT_EVENT_NAME = "tsf_rate_limit";

    // dimensions keys
    String APP_ID_KEY = "app_id";
    String NAMESPACE_ID_KEY = "namespace_id";
    String SERVICE_NAME = "service_name";
    String APPLICATION_ID = "application_id";

    // additionalMsg keys
    String UPSTREAM_SERVICE_KEY = "upstream_service";
    String UPSTREAM_NAMESPACE_ID_KEY = "upstream_namespace";
    String DOWNSTREAM_SERVICE_KEY = "downstream_service";
    String DOWNSTREAM_NAMESPACE_ID_KEY = "downstream_namespace";
    String ISOLATION_OBJECT_KEY = "isolation_object";
    String FAILURE_RATE_KEY = "failure_rate";
    String SLOW_CALL_DURATION_KEY = "slow_call_rate";
    String RULE_ID_KEY = "rule_id";
    String RULE_DETAIL_KEY = "rule_detail";

    /**
     * 熔断恢复
     */
    Byte STATUS_RECOVER = 0;

    /**
     * 熔断打开
     */
    Byte STATUS_TRIGGER = 1;
}
