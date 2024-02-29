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
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.flow.DiscoveryFlow;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.GetHealthyInstancesRequest;
import com.tencent.polaris.api.rpc.GetInstancesRequest;
import com.tencent.polaris.api.rpc.GetOneInstanceRequest;
import com.tencent.polaris.api.rpc.GetServiceContractRequest;
import com.tencent.polaris.api.rpc.GetServiceRuleRequest;
import com.tencent.polaris.api.rpc.GetServicesRequest;
import com.tencent.polaris.api.rpc.InstancesFuture;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.rpc.ServiceRuleResponse;
import com.tencent.polaris.api.rpc.ServicesResponse;
import com.tencent.polaris.api.rpc.UnWatchInstancesRequest;
import com.tencent.polaris.api.rpc.UnWatchServiceRequest;
import com.tencent.polaris.api.rpc.WatchInstancesRequest;
import com.tencent.polaris.api.rpc.WatchServiceRequest;
import com.tencent.polaris.api.rpc.WatchServiceResponse;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.client.flow.AsyncFlow;
import com.tencent.polaris.discovery.client.flow.CommonInstancesRequest;
import com.tencent.polaris.discovery.client.flow.SyncFlow;
import com.tencent.polaris.discovery.client.util.Validator;

/**
 * ConsumerAPI的标准实现
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class DefaultConsumerAPI extends BaseEngine implements ConsumerAPI {

    private final Configuration config;

    private DiscoveryFlow discoveryFlow;

    private final SyncFlow syncFlow = new SyncFlow();

    private final AsyncFlow asyncFlow = new AsyncFlow();

    public DefaultConsumerAPI(SDKContext context) {
        super(context);
        config = context.getConfig();
    }

    @Override
    protected void subInit() throws PolarisException {
        discoveryFlow = sdkContext.getOrInitFlow(DiscoveryFlow.class);
        syncFlow.init(sdkContext.getExtensions());
        asyncFlow.init(syncFlow);
    }

    @Override
    public InstancesResponse getAllInstance(GetAllInstancesRequest req) throws PolarisException {
        return getAllInstances(req);
    }

    @Override
    public InstancesResponse getAllInstances(GetAllInstancesRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetAllInstancesRequest(req);
        return discoveryFlow.getAllInstances(req);
    }

    @Override
    public InstancesResponse getHealthyInstances(GetHealthyInstancesRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateGetHealthyInstancesRequest(req);
        return discoveryFlow.getHealthyInstances(req);
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
        return discoveryFlow.asyncGetAllInstances(req);
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
        return discoveryFlow.getServiceRule(request);
    }

    @Override
    public ServicesResponse getServices(GetServicesRequest request) throws PolarisException {
        checkAvailable("ConsumerAPI");
        return discoveryFlow.getServices(request);
    }

    @Override
    public WatchServiceResponse watchService(WatchServiceRequest request) throws PolarisException {
        checkAvailable("ConsumerAPI");
        Validator.validateWatchServiceRequest(request);
        InstancesResponse response = discoveryFlow.watchInstances(WatchInstancesRequest.builder()
                .service(request.getService())
                .namespace(request.getNamespace())
                .listeners(request.getListeners())
                .build());
        return new WatchServiceResponse(response, true);
    }

    @Override
    public boolean unWatchService(UnWatchServiceRequest request) {
        checkAvailable("ConsumerAPI");
        Validator.validateUnWatchServiceRequest(request);
        discoveryFlow.unWatchInstances(UnWatchInstancesRequest.builder()
                .service(request.getService())
                .namespace(request.getNamespace())
                .listeners(request.getListeners())
                .build());
        return true;
    }

    @Override
    public ServiceRuleResponse getServiceContract(GetServiceContractRequest req) throws PolarisException {
        checkAvailable("ConsumerAPI");
        if (StringUtils.isBlank(req.getService())) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "service_contract namespace can not be blank");
        }
        if (StringUtils.isBlank(req.getName())) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "service_contract name can not be blank");
        }
        if (StringUtils.isBlank(req.getProtocol())) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "service_contract protocol can not be blank");
        }
        return discoveryFlow.getServiceContract(req);
    }
}
