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

package com.tencent.polaris.assembly.flow;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.assembly.api.pojo.AfterRequest;
import com.tencent.polaris.assembly.api.pojo.BeforeRequest;
import com.tencent.polaris.assembly.api.pojo.BeforeResponse;
import com.tencent.polaris.assembly.api.pojo.GetOneInstanceRequest;
import com.tencent.polaris.assembly.api.pojo.ServiceCallResult;
import com.tencent.polaris.client.flow.AbstractFlow;
import java.util.ServiceLoader;

public interface AssemblyFlow extends AbstractFlow {

    default BeforeResponse beforeCallService(BeforeRequest beforeRequest) {
        return null;
    }

    default void afterCallService(AfterRequest afterRequest) {

    }

    default BeforeResponse beforeProcess(BeforeRequest beforeRequest) {
        return null;
    }

    default void afterProcess(AfterRequest afterRequest) {

    }

    default void initService(ServiceKey serviceKey) {

    }

    default Instance getOneInstance(GetOneInstanceRequest request) {
        return null;
    }

    default void updateServiceCallResult(ServiceCallResult result) {

    }

    static AssemblyFlow loadAssemblyFlow(String name) {
        ServiceLoader<AssemblyFlow> discoveryFlows = ServiceLoader.load(AssemblyFlow.class);
        for (AssemblyFlow discoveryFlow : discoveryFlows) {
            if (StringUtils.equals(discoveryFlow.getName(), name)) {
                return discoveryFlow;
            }
        }
        throw new PolarisException(ErrorCode.INVALID_CONFIG,
                String.format("unknown flow name %s, type is %s", name, AssemblyFlow.class.getCanonicalName()));
    }

}
