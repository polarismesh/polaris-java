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

package com.tencent.polaris.discovery.client.flow;

import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.flow.DiscoveryFlow;
import com.tencent.polaris.api.plugin.lossless.InstanceProperties;
import com.tencent.polaris.api.plugin.lossless.LosslessPolicy;
import com.tencent.polaris.api.plugin.server.*;
import com.tencent.polaris.api.pojo.BaseInstance;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.api.ServiceCallResultListener;
import com.tencent.polaris.client.pojo.ServiceRuleByProto;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.specification.api.v1.traffic.manage.RoutingProto;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class DefaultDiscoveryFlow implements DiscoveryFlow {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultDiscoveryFlow.class);

    private static final int DEFAULT_INSTANCE_TTL = 5;

    private SDKContext sdkContext;

    private Configuration config;

    private final SyncFlow syncFlow = new SyncFlow();

    private final AsyncFlow asyncFlow = new AsyncFlow();

    private final WatchFlow watchFlow = new WatchFlow();

    private RegisterFlow registerFlow;

    private List<ServiceCallResultListener> serviceCallResultListeners;

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
        this.config = sdkContext.getConfig();
        syncFlow.init(sdkContext.getExtensions());
        asyncFlow.init(syncFlow);
        watchFlow.init(sdkContext.getExtensions(), syncFlow);
        serviceCallResultListeners = ServiceCallResultListener.getServiceCallResultListeners(sdkContext);
        registerFlow = new RegisterFlow(sdkContext);
    }

    @Override
    public InstancesResponse getAllInstances(GetAllInstancesRequest req) {
        CommonInstancesRequest allRequest = new CommonInstancesRequest(req, config);
        return syncFlow.commonSyncGetAllInstances(allRequest);
    }

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public InstancesFuture asyncGetAllInstances(GetAllInstancesRequest req) {
        CommonInstancesRequest allRequest = new CommonInstancesRequest(req, config);
        return asyncFlow.commonAsyncGetAllInstances(allRequest);
    }

    @Override
    public InstancesResponse getHealthyInstances(GetHealthyInstancesRequest req) {
        CommonInstancesRequest healthyRequest = new CommonInstancesRequest(req, config);
        return syncFlow.commonSyncGetInstances(healthyRequest);
    }

    @Override
    public InstancesResponse watchInstances(WatchInstancesRequest request) {
        GetAllInstancesRequest getAllInstancesRequest = GetAllInstancesRequest.builder()
                .namespace(request.getNamespace())
                .service(request.getService())
                .build();
        CommonInstancesRequest allRequest = new CommonInstancesRequest(getAllInstancesRequest, this.config);
        CommonWatchServiceRequest watchServiceRequest = new CommonWatchServiceRequest(request, allRequest);
        WatchServiceResponse response = watchFlow.commonWatchService(watchServiceRequest);
        return response.getResponse();
    }

    @Override
    public InstancesResponse unWatchInstances(UnWatchInstancesRequest request) {
        CommonUnWatchServiceRequest watchServiceRequest = new CommonUnWatchServiceRequest(request);
        WatchServiceResponse response = watchFlow.commonUnWatchService(watchServiceRequest);
        return response.getResponse();
    }

    @Override
    public ServiceRuleResponse getServiceRule(GetServiceRuleRequest req) {
        CommonRuleRequest commonRuleRequest = new CommonRuleRequest(req, config);
        return syncFlow.commonSyncGetServiceRule(commonRuleRequest);
    }

    @Override
    public ServicesResponse getServices(GetServicesRequest req) {
        CommonServicesRequest commonServicesRequest = new CommonServicesRequest(req, config);
        return syncFlow.commonSyncGetServices(commonServicesRequest);
    }

    @Override
    public InstanceRegisterResponse register(InstanceRegisterRequest req) {
        if (!req.isAutoHeartbeat()) {
            return doRegister(req, null);
        }
        if (req.getTtl() == null) {
            req.setTtl(DEFAULT_INSTANCE_TTL);
        }
        return registerFlow.registerInstance(req, this::doRegister, this::heartbeat);
    }

    private InstanceRegisterResponse doRegister(InstanceRegisterRequest req, Map<String, String> customHeader) {
        enrichInstanceLocation(req);
        ServerConnector serverConnector = sdkContext.getExtensions().getServerConnector();
        long retryInterval = sdkContext.getConfig().getGlobal().getAPI().getRetryInterval();
        long timeout = getTimeout(req);
        while (timeout > 0) {
            long start = System.currentTimeMillis();
            ServiceCallResult serviceCallResult = new ServiceCallResult();
            CommonProviderRequest request = req.getRequest();
            try {
                CommonProviderResponse response = serverConnector.registerInstance(request, customHeader);
                LOG.info("register {}/{} instance {} successfully", req.getNamespace(), req.getService(),
                        response.getInstanceID());
                serviceCallResult.setRetStatus(RetStatus.RetSuccess);
                serviceCallResult.setRetCode(ErrorCode.Success.getCode());
                sdkContext.getValueContext().setPort(req.getPort());
                sdkContext.getValueContext().setInstanceId(response.getInstanceID());
                return new InstanceRegisterResponse(response.getInstanceID(), response.isExists());
            } catch (PolarisException e) {
                serviceCallResult.setRetStatus(RetStatus.RetFail);
                serviceCallResult.setRetCode(exceptionToErrorCode(e).getCode());
                if (e instanceof RetriableException) {
                    LOG.warn("instance register request error, retrying.", e);
                    Utils.sleepUninterrupted(retryInterval);
                    continue;
                }
                throw e;
            } finally {
                long delay = System.currentTimeMillis() - start;
                serviceCallResult.setDelay(delay);
                reportServerCall(serviceCallResult, request.getTargetServer(), "register");
                timeout -= delay;
            }
        }
        throw new PolarisException(ErrorCode.API_TIMEOUT, "instance register request timeout.");
    }

    @Override
    public void deRegister(InstanceDeregisterRequest req) {
        RegisterStateManager.removeRegisterState(sdkContext, req);
        long retryInterval = sdkContext.getConfig().getGlobal().getAPI().getRetryInterval();
        long timeout = getTimeout(req);
        ServerConnector serverConnector = sdkContext.getExtensions().getServerConnector();
        while (timeout > 0) {
            long start = System.currentTimeMillis();
            ServiceCallResult serviceCallResult = new ServiceCallResult();
            CommonProviderRequest request = req.getRequest();
            try {
                serverConnector.deregisterInstance(request);
                serviceCallResult.setRetStatus(RetStatus.RetSuccess);
                serviceCallResult.setRetCode(ErrorCode.Success.getCode());
                LOG.info("deregister instance {} successfully", req);
                return;
            } catch (PolarisException e) {
                serviceCallResult.setRetStatus(RetStatus.RetFail);
                serviceCallResult.setRetCode(exceptionToErrorCode(e).getCode());
                if (e instanceof RetriableException) {
                    LOG.warn("instance deregister request error, retrying.", e);
                    Utils.sleepUninterrupted(retryInterval);
                    continue;
                }
                throw e;
            } finally {
                long delay = System.currentTimeMillis() - start;
                serviceCallResult.setDelay(delay);
                reportServerCall(serviceCallResult, request.getTargetServer(), "deRegister");
                timeout -= delay;
            }
        }
        throw new PolarisException(ErrorCode.API_TIMEOUT, "instance deregister request timeout.");
    }

    @Override
    public void heartbeat(InstanceHeartbeatRequest req) {
        long timeout = getTimeout(req);
        long retryInterval = sdkContext.getConfig().getGlobal().getAPI().getRetryInterval();
        ServerConnector serverConnector = sdkContext.getExtensions().getServerConnector();
        while (timeout > 0) {
            long start = System.currentTimeMillis();
            ServiceCallResult serviceCallResult = new ServiceCallResult();
            CommonProviderRequest request = req.getRequest();
            request.setTimeoutMs(timeout);
            try {
                serverConnector.heartbeat(request);
                serviceCallResult.setRetStatus(RetStatus.RetSuccess);
                serviceCallResult.setRetCode(ErrorCode.Success.getCode());
                return;
            } catch (PolarisException e) {
                serviceCallResult.setRetStatus(RetStatus.RetFail);
                serviceCallResult.setRetCode(exceptionToErrorCode(e).getCode());
                if (e instanceof RetriableException) {
                    LOG.warn("heartbeat request error, retrying.", e);
                    Utils.sleepUninterrupted(retryInterval);
                    continue;
                }
                throw e;
            } finally {
                long delay = System.currentTimeMillis() - start;
                serviceCallResult.setDelay(delay);
                reportServerCall(serviceCallResult, request.getTargetServer(), "heartbeat");
                timeout -= delay;
            }
        }
        throw new PolarisException(ErrorCode.API_TIMEOUT, "heartbeat request timeout.");
    }

    @Override
    public ReportServiceContractResponse reportServiceContract(ReportServiceContractRequest req) {
        long timeout = getTimeout(req);
        long retryInterval = sdkContext.getConfig().getGlobal().getAPI().getRetryInterval();
        ServerConnector serverConnector = sdkContext.getExtensions().getServerConnector();
        while (timeout > 0) {
            long start = System.currentTimeMillis();
            ServiceCallResult serviceCallResult = new ServiceCallResult();
            try {
                ReportServiceContractResponse response = serverConnector.reportServiceContract(req);
                serviceCallResult.setRetStatus(RetStatus.RetSuccess);
                serviceCallResult.setRetCode(ErrorCode.Success.getCode());
                return response;
            } catch (PolarisException e) {
                serviceCallResult.setRetStatus(RetStatus.RetFail);
                serviceCallResult.setRetCode(exceptionToErrorCode(e).getCode());
                if (e instanceof RetriableException) {
                    LOG.warn("report service contract error, retrying.", e);
                    Utils.sleepUninterrupted(retryInterval);
                    continue;
                }
                throw e;
            } finally {
                long delay = System.currentTimeMillis() - start;
                serviceCallResult.setDelay(delay);
                reportServerCall(serviceCallResult, req.getTargetServer(), "reportServiceContract");
                timeout -= delay;
            }
        }
        throw new PolarisException(ErrorCode.API_TIMEOUT, "report service contract timeout.");
    }

    @Override
    public ServiceRuleResponse getServiceContract(GetServiceContractRequest req) {
        long timeout = getTimeout(req);
        long retryInterval = sdkContext.getConfig().getGlobal().getAPI().getRetryInterval();
        ServerConnector serverConnector = sdkContext.getExtensions().getServerConnector();

        while (timeout > 0) {
            long start = System.currentTimeMillis();
            ServiceCallResult serviceCallResult = new ServiceCallResult();
            CommonServiceContractRequest request = req.getRequest();
            request.setNamespace(req.getNamespace());
            request.setService(req.getService());
            try {
                ServiceRuleByProto response = serverConnector.getServiceContract(request);
                serviceCallResult.setRetStatus(RetStatus.RetSuccess);
                serviceCallResult.setRetCode(ErrorCode.Success.getCode());
                return new ServiceRuleResponse(response);
            } catch (PolarisException e) {
                serviceCallResult.setRetStatus(RetStatus.RetFail);
                serviceCallResult.setRetCode(exceptionToErrorCode(e).getCode());
                if (e instanceof RetriableException) {
                    LOG.warn("get service contract error, retrying.", e);
                    Utils.sleepUninterrupted(retryInterval);
                    continue;
                }
                throw e;
            } finally {
                long delay = System.currentTimeMillis() - start;
                serviceCallResult.setDelay(delay);
                reportServerCall(serviceCallResult, request.getTargetServer(), "getServiceContract");
                timeout -= delay;
            }
        }
        throw new PolarisException(ErrorCode.API_TIMEOUT, "get service contract timeout.");
    }

    private void enrichInstanceLocation(InstanceRegisterRequest request) {
        if (!StringUtils.isAllEmpty(request.getRegion(), request.getZone(), request.getCampus())) {
            return;
        }

        request.setRegion(sdkContext.getValueContext().getValue(RoutingProto.NearbyRoutingConfig.LocationLevel.REGION.name()));
        request.setZone(sdkContext.getValueContext().getValue(RoutingProto.NearbyRoutingConfig.LocationLevel.ZONE.name()));
        request.setCampus(sdkContext.getValueContext().getValue(RoutingProto.NearbyRoutingConfig.LocationLevel.CAMPUS.name()));
    }

    /**
     * 获取API超时时间
     *
     * @param entity entity
     * @return 超时时间，单位毫秒
     */
    private long getTimeout(RequestBaseEntity entity) {
        return entity.getTimeoutMs() == 0 ? sdkContext.getConfig().getGlobal().getAPI().getTimeout()
                : entity.getTimeoutMs();
    }

    private ErrorCode exceptionToErrorCode(Exception exception) {
        if (exception instanceof PolarisException) {
            return ((PolarisException) exception).getCode();
        }
        return ErrorCode.INTERNAL_ERROR;
    }

    /**
     * 上报调用结果数据
     *
     * @param req 调用结果数据
     * @throws PolarisException
     */
    private void reportInvokeStat(ServiceCallResult req) throws PolarisException {
        for (ServiceCallResultListener listener : serviceCallResultListeners) {
            listener.onServiceCallResult(req);
        }
    }

    /**
     * 上报内部服务调用结果
     *
     * @param serviceCallResult 服务调用结果
     * @param targetServer      目标服务端
     * @param method            方法
     */
    public void reportServerCall(ServiceCallResult serviceCallResult, TargetServer targetServer, String method) {
        if (null != targetServer) {
            serviceCallResult.setNamespace(targetServer.getServiceKey().getNamespace());
            serviceCallResult.setService(targetServer.getServiceKey().getService());
            serviceCallResult.setHost(targetServer.getHost());
            serviceCallResult.setPort(targetServer.getPort());
            serviceCallResult.setLabels(targetServer.getLabels());
        }
        serviceCallResult.setMethod(method);
        reportInvokeStat(serviceCallResult);
    }

    @Override
    public void losslessRegister(BaseInstance instance) {
        List<LosslessPolicy> losslessPolicies = sdkContext.getExtensions().getLosslessPolicies();
        if (CollectionUtils.isEmpty(losslessPolicies)) {
            LOG.info("lossless is disabled, no losslessRegister will do");
            return;
        }
        InstanceProperties instanceProperties = new InstanceProperties();
        for (LosslessPolicy losslessPolicy : losslessPolicies) {
            losslessPolicy.buildInstanceProperties(instanceProperties);
        }
        for (LosslessPolicy losslessPolicy : losslessPolicies) {
            losslessPolicy.losslessRegister(instance, instanceProperties);
        }
    }

    @Override
    public void losslessDeregister(BaseInstance instance) {
        List<LosslessPolicy> losslessPolicies = sdkContext.getExtensions().getLosslessPolicies();
        if (CollectionUtils.isEmpty(losslessPolicies)) {
            LOG.info("lossless is disabled, no losslessDeregister will do");
            return;
        }
        for (LosslessPolicy losslessPolicy : losslessPolicies) {
            losslessPolicy.losslessDeregister(instance);
        }
    }

    @Override
    public void destroy() {
        if (registerFlow != null) {
            registerFlow.destroy();
        }
    }
}
