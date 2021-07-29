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

package com.tencent.polaris.plugins.stat.common.model;

public class SystemMetricModel {
    public static class SystemMetricLabelOrder {
        public static final String[] INSTANCE_GAUGE_LABEL_ORDER = new String[]{
                SystemMetricName.CALLEE_NAMESPACE,
                SystemMetricName.CALLEE_SERVICE,
                SystemMetricName.CALLEE_SUBSET,
                SystemMetricName.CALLEE_INSTANCE,
                SystemMetricName.CALLEE_RET_CODE,
                SystemMetricName.CALLER_LABELS,
                SystemMetricName.CALLER_NAMESPACE,
                SystemMetricName.CALLER_SERVICE,
                SystemMetricName.CALLER_IP,
                SystemMetricName.METRIC_NAME_LABEL,
        };
        public static final String[] RATELIMIT_GAUGE_LABEL_ORDER = new String[]{
                SystemMetricName.CALLEE_NAMESPACE,
                SystemMetricName.CALLEE_SERVICE,
                SystemMetricName.CALLEE_METHOD,
                SystemMetricName.CALLER_LABELS,
                SystemMetricName.METRIC_NAME_LABEL,
        };
        public static final String[] CIRCUIT_BREAKER_LABEL_ORDER = new String[]{
                SystemMetricName.CALLEE_NAMESPACE,
                SystemMetricName.CALLEE_SERVICE,
                SystemMetricName.CALLEE_METHOD,
                SystemMetricName.CALLEE_SUBSET,
                SystemMetricName.CALLEE_INSTANCE,
                SystemMetricName.CALLER_NAMESPACE,
                SystemMetricName.CALLER_SERVICE,
                SystemMetricName.CALLER_IP,
                SystemMetricName.METRIC_NAME_LABEL,
        };
    }

    public static class SystemMetricName {
        public static final String CALLEE_NAMESPACE = "callee_namespace";
        public static final String CALLEE_SERVICE = "callee_service";
        public static final String CALLEE_SUBSET = "callee_subset";
        public static final String CALLEE_INSTANCE = "callee_instance";
        public static final String CALLEE_RET_CODE = "callee_result_code";
        public static final String CALLEE_METHOD = "callee_method";
        public static final String CALLER_NAMESPACE = "caller_namespace";
        public static final String CALLER_SERVICE = "caller_service";
        public static final String CALLER_IP = "caller_ip";
        public static final String CALLER_LABELS = "caller_labels";
        public static final String METRIC_NAME_LABEL = "metric_name";
    }

    public static class SystemMetricValue {
        public static final String NULL_VALUE = "__NULL__";
    }
}
