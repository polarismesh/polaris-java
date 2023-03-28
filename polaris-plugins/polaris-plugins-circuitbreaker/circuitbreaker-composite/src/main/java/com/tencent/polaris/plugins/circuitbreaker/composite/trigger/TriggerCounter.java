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

package com.tencent.polaris.plugins.circuitbreaker.composite.trigger;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.composite.StatusChangeHandler;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.TriggerCondition;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

public abstract class TriggerCounter {

    private static final Logger LOG = LoggerFactory.getLogger(TriggerCounter.class);

    protected final String ruleName;

    protected final TriggerCondition triggerCondition;

    protected final Resource resource;

    protected final StatusChangeHandler statusChangeHandler;

    protected final AtomicBoolean suspended = new AtomicBoolean(false);

    public TriggerCounter(String ruleName, CounterOptions counterOptions) {
        this.triggerCondition = counterOptions.getTriggerCondition();
        this.ruleName = ruleName;
        this.resource = counterOptions.getResource();
        this.statusChangeHandler = counterOptions.getStatusChangeHandler();
        init();
    }

    public void suspend() {
        suspended.set(true);
        LOG.info("[CircuitBreaker][Counter] counter {} suspend", ruleName);
    }

    public void resume() {
        suspended.set(false);
        LOG.info("[CircuitBreaker][Counter] counter {} resume", ruleName);
    }

    protected abstract void init();

    public abstract void report(boolean success);
}
