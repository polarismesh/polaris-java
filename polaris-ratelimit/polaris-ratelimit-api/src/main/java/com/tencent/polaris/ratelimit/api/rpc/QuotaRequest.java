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

import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.utils.CollectionUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//配额查询请求
public class QuotaRequest extends RequestBaseEntity {

    private String namespace;

    private String service;

    private String method;

    private Set<MatchArgument> arguments = new HashSet<>();

    private int count = 1;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    /**
     * Deprecated: use getArguments instead
     *
     * @return labels
     */
    @Deprecated
    public Map<String, String> getLabels() {
        Map<String, String> values = new HashMap<>();
        for (MatchArgument matchArgument : arguments) {
            matchArgument.toLabel(values);
        }
        return values;
    }

    /**
     * Deprecated: use setArguments instead
     */
    @Deprecated
    public void setLabels(Map<String, String> labels) {
        if (CollectionUtils.isEmpty(labels)) {
            return;
        }
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            arguments.add(MatchArgument.fromLabel(entry.getKey(), entry.getValue()));
        }
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Set<MatchArgument> getArguments() {
        return arguments;
    }

    public void setArguments(Set<MatchArgument> arguments) {
        if (CollectionUtils.isEmpty(arguments)) {
            this.arguments = Collections.emptySet();
        } else {
            this.arguments = arguments;
        }
    }

    @Override
    public String toString() {
        return "QuotaRequest{" +
                "namespace='" + namespace + '\'' +
                ", service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", arguments=" + arguments +
                ", count=" + count +
                "} " + super.toString();
    }
}
