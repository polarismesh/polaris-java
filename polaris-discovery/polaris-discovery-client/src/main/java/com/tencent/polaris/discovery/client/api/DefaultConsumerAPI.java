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

package com.tencent.polaris.discovery.client.api;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.listener.ServiceListener;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.GetServiceRuleRequest;
import com.tencent.polaris.api.rpc.InstancesFuture;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.api.ServiceCallResultListener;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.discovery.client.flow.AsyncFlow;
import com.tencent.polaris.discovery.client.flow.CommonInstancesRequest;
import com.tencent.polaris.discovery.client.flow.CommonRuleRequest;
import com.tencent.polaris.discovery.client.flow.SyncFlow;
import com.tencent.polaris.discovery.client.util.Validator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConsumerAPI的标准实现
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class DefaultConsumerAPI extends BaseEngine implements ConsumerAPI {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultConsumerAPI.class);

    private final Configuration config;

    private final SyncFlow syncFlow = new SyncFlow();

    private final AsyncFlow asyncFlow = new AsyncFlow();

    private List<ServiceCallResultListener> serviceCallResultListeners;

    public DefaultConsumerAPI(SDKContext context) {
        super(context);
        config = context.getConfig();
        syncFlow.init(context.getExtensions());
        asyncFlow.init(syncFlow);
    }

    @Override
    protected void subInit() {
        serviceCallResultListeners = ServiceCallResultListener.getServiceCallResultListeners(sdkContext);
        sdkContext.registerDestroyHook(new Destroyable() {
            @Override
            protected void doDestroy() {
                if (null != serviceCallResultListeners) {
                    for (ServiceCallResultListener listener : serviceCallResultListeners) {
                        listener.destroy();
                    }
                }
            }
        });
    }

    @Override
    public InstancesResponse getAllInstance(GetAllInstancesRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetAllInstancesRequest(req);
        CommonInstancesRequest allRequest = new CommonInstancesRequest(req, config);
        return syncFlow.commonSyncGetAllInstances(allRequest);
    }

    @Override
    public InstancesResponse getOneInstance(GetOneInstanceRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetOneInstanceRequest(req);
        CommonInstancesRequest allRequest = new CommonInstancesRequest(req, config);
        return syncFlow.commonSyncGetOneInstance(allRequest);
    }

    @Override
    public InstancesResponse getInstances(GetInstancesRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetInstancesRequest(req);
        CommonInstancesRequest allRequest = new CommonInstancesRequest(req, config);
        return syncFlow.commonSyncGetInstances(allRequest);
    }

    @Override
    public InstancesFuture asyncGetOneInstance(GetOneInstanceRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetOneInstanceRequest(req);
        CommonInstancesRequest allRequest = new CommonInstancesRequest(req, config);
        return asyncFlow.commonAsyncGetOneInstance(allRequest);
    }

    @Override
    public InstancesFuture asyncGetInstances(GetInstancesRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetInstancesRequest(req);
        CommonInstancesRequest allRequest = new CommonInstancesRequest(req, config);
        return asyncFlow.commonAsyncGetInstances(allRequest);
    }

    @Override
    public InstancesFuture asyncGetAllInstances(GetAllInstancesRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetAllInstancesRequest(req);
        CommonInstancesRequest allRequest = new CommonInstancesRequest(req, config);
        return asyncFlow.commonAsyncGetAllInstances(allRequest);
    }

    @Override
    public void updateServiceCallResult(ServiceCallResult req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateServiceCallResult(req);
        for (ServiceCallResultListener listener : serviceCallResultListeners) {
            listener.onServiceCallResult(req);
        }
    }

    @Override
    public ServiceRuleResponse getServiceRule(GetServiceRuleRequest request) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetServiceRuleRequest(request);
        CommonRuleRequest commonRuleRequest = new CommonRuleRequest(request, config);
        return syncFlow.commonSyncGetServiceRule(commonRuleRequest);
    }

    @Override
    public boolean addListener(ServiceListener listener) {
        return false;
    }

    @Override
    public boolean removeListener(ServiceListener listener) {
        return false;
    }
}
