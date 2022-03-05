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
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.client.flow.*;
import com.tencent.polaris.discovery.client.util.Validator;
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

    private final WatchFlow watchFlow = new WatchFlow();

    public DefaultConsumerAPI(SDKContext context) {
        super(context);
        config = context.getConfig();
        syncFlow.init(context.getExtensions());
        asyncFlow.init(syncFlow);
        watchFlow.init(context.getExtensions(), syncFlow);
    }

    @Override
    protected void subInit() throws PolarisException {

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
        reportInvokeStat(req);
    }

    @Override
    public ServiceRuleResponse getServiceRule(GetServiceRuleRequest request) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetServiceRuleRequest(request);
        CommonRuleRequest commonRuleRequest = new CommonRuleRequest(request, config);
        return syncFlow.commonSyncGetServiceRule(commonRuleRequest);
    }

    @Override
    public ServicesResponse getServices(GetServicesRequest request) throws PolarisException {
        checkAvailable("ConsumerAPI");
        CommonServicesRequest commonServicesRequest = new CommonServicesRequest(request, config);
        return syncFlow.commonSyncGetServices(commonServicesRequest);
    }

    @Override
    public WatchServiceResponse watchService(WatchServiceRequest request) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateWatchServiceRequest(request);
        CommonWatchServiceRequest watchServiceRequest = new CommonWatchServiceRequest(request,
                new CommonInstancesRequest(GetAllInstancesRequest.builder()
                        .service(request.getService())
                        .namespace(request.getNamespace())
                        .build(), config), true);
        return watchFlow.commonWatchService(watchServiceRequest);
    }

    @Override
    public boolean unWatchService(WatchServiceRequest request) {
        checkAvailable("ConsumerAPI");
        Validator.validateWatchServiceRequest(request);
        CommonWatchServiceRequest watchServiceRequest = new CommonWatchServiceRequest(request, false);
        return watchFlow.commonWatchService(watchServiceRequest).isResult();
    }
}
