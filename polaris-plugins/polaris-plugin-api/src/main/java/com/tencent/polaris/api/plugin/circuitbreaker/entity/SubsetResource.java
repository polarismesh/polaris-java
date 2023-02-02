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

package com.tencent.polaris.api.plugin.circuitbreaker.entity;

import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.client.util.CommonValidator;
import com.tencent.polaris.specification.api.v1.fault.tolerance.CircuitBreakerProto.Level;
import com.tencent.polaris.specification.api.v1.model.ModelProto.MatchString;
import java.util.Map;
import java.util.Objects;

public class SubsetResource extends AbstractResource {

    private final String subset;

    private final Map<String, MatchString> metadata;

    public SubsetResource(ServiceKey service, String subset, Map<String, MatchString> metadata) {
        this(service, subset, metadata, null);
    }

    public SubsetResource(ServiceKey service, String subset, Map<String, MatchString> metadata,
            ServiceKey callerService) {
        super(service, callerService);
        CommonValidator.validateService(service);
        CommonValidator.validateNamespaceService(service.getNamespace(), service.getService());
        CommonValidator.validateText(subset, "subset");
        this.subset = subset;
        this.metadata = metadata;
    }

    @Override
    public Level getLevel() {
        return Level.GROUP;
    }

    public ServiceKey getService() {
        return service;
    }

    public String getSubset() {
        return subset;
    }

    public Map<String, MatchString> getMetadata() {
        return metadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SubsetResource)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SubsetResource that = (SubsetResource) o;
        return Objects.equals(subset, that.subset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), subset);
    }

    @Override
    public String toString() {
        return "SubsetResource{" +
                "subset='" + subset + '\'' +
                ", metadata=" + metadata +
                "} " + super.toString();
    }
}
