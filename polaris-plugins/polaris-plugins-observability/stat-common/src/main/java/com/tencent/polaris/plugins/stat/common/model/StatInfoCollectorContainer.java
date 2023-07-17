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

import com.tencent.polaris.api.plugin.stat.CircuitBreakGauge;
import com.tencent.polaris.api.plugin.stat.RateLimitGauge;
import com.tencent.polaris.api.pojo.InstanceGauge;

import java.util.Arrays;
import java.util.List;

public class StatInfoCollectorContainer {
    private final StatInfoRevisionCollector<InstanceGauge> insCollector;
    private final StatInfoRevisionCollector<RateLimitGauge> rateLimitCollector;
    private final StatInfoStatefulCollector<CircuitBreakGauge> circuitBreakerCollector;

    public StatInfoCollectorContainer() {
        this.insCollector = new StatInfoRevisionCollector<InstanceGauge>();
        this.rateLimitCollector = new StatInfoRevisionCollector<RateLimitGauge>();
        this.circuitBreakerCollector = new StatInfoStatefulCollector<CircuitBreakGauge>();
    }

    public StatInfoRevisionCollector<InstanceGauge> getInsCollector() {
        return insCollector;
    }

    public StatInfoRevisionCollector<RateLimitGauge> getRateLimitCollector() {
        return rateLimitCollector;
    }

    public StatInfoStatefulCollector<CircuitBreakGauge> getCircuitBreakerCollector() {
        return circuitBreakerCollector;
    }

    public List<StatInfoCollector<?, ? extends StatMetric>> getCollectors() {
        return Arrays.asList(insCollector, rateLimitCollector, circuitBreakerCollector);
    }
}

