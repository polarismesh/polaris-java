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

package com.tencent.polaris.plugins.circuitbreaker.composite.trigger;

import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.TrieNode;
import com.tencent.polaris.plugins.circuitbreaker.composite.StatusChangeHandler;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.TriggerCondition;
import com.tencent.polaris.specification.api.v1.model.ModelProto;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;
import java.util.regex.Pattern;

public class CounterOptions {

    private Resource resource;

    private ModelProto.API api;

    private List<CircuitBreakerProto.ErrorCondition> errorConditionList;

    private TriggerCondition triggerCondition;

    private ScheduledExecutorService executorService;

    private StatusChangeHandler statusChangeHandler;

    private Function<String, Pattern> regexFunction;

    private Function<String, TrieNode<String>> trieNodeFunction;

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public ModelProto.API getApi() {
        return api;
    }

    public void setApi(ModelProto.API api) {
        this.api = api;
    }

    public List<CircuitBreakerProto.ErrorCondition> getErrorConditionList() {
        return errorConditionList;
    }

    public void setErrorConditionList(List<CircuitBreakerProto.ErrorCondition> errorConditionList) {
        this.errorConditionList = errorConditionList;
    }

    public TriggerCondition getTriggerCondition() {
        return triggerCondition;
    }

    public void setTriggerCondition(
            TriggerCondition triggerCondition) {
        this.triggerCondition = triggerCondition;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public StatusChangeHandler getStatusChangeHandler() {
        return statusChangeHandler;
    }

    public void setStatusChangeHandler(
            StatusChangeHandler statusChangeHandler) {
        this.statusChangeHandler = statusChangeHandler;
    }

    public Function<String, Pattern> getRegexFunction() {
        return regexFunction;
    }

    public void setRegexFunction(Function<String, Pattern> regexFunction) {
        this.regexFunction = regexFunction;
    }

    public Function<String, TrieNode<String>> getTrieNodeFunction() {
        return trieNodeFunction;
    }

    public void setTrieNodeFunction(Function<String, TrieNode<String>> trieNodeFunction) {
        this.trieNodeFunction = trieNodeFunction;
    }
}
