package com.tencent.polaris.discovery.client.api;

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.plugin.server.CommonProviderRequest;
import com.tencent.polaris.api.plugin.server.CommonProviderResponse;
import com.tencent.polaris.api.plugin.server.ServerConnector;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.LocationUtils;
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.discovery.client.flow.AsyncRegisterFlow;
import com.tencent.polaris.discovery.client.flow.RegisterStateManager;
import com.tencent.polaris.discovery.client.util.Validator;
import com.tencent.polaris.logging.LoggerFactory;
import java.util.Map;
import org.slf4j.Logger;

/**
 * ProviderAPI的标准实现
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public class DefaultProviderAPI extends BaseEngine implements ProviderAPI {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultProviderAPI.class);
    private static final int DEFAULT_INSTANCE_TTL = 5;
    private final AsyncRegisterFlow asyncRegisterFlow;
    private ServerConnector serverConnector;

    public DefaultProviderAPI(SDKContext sdkContext) {
        super(sdkContext);
        asyncRegisterFlow = new AsyncRegisterFlow(sdkContext);
    }

    @Override
    protected void subInit() {
        serverConnector = sdkContext.getExtensions().getServerConnector();
    }

    private ErrorCode exceptionToErrorCode(Exception exception) {
        if (exception instanceof PolarisException) {
            return ((PolarisException) exception).getCode();
        }
        return ErrorCode.INTERNAL_ERROR;
    }

    @Override
    public InstanceRegisterResponse registerInstance(InstanceRegisterRequest req) throws PolarisException {
        if (req.getTtl() == null) {
            req.setTtl(DEFAULT_INSTANCE_TTL);
        }
        return asyncRegisterFlow.registerInstance(req, this::doRegister, this::heartbeat);
    }

    @Override
    protected void doDestroy() {
        RegisterStateManager.destroy(sdkContext);
        super.doDestroy();
    }

    @Override
    public InstanceRegisterResponse register(InstanceRegisterRequest req) throws PolarisException {
        return doRegister(req, null);
    }

    private InstanceRegisterResponse doRegister(InstanceRegisterRequest req, Map<String, String> customHeader) {
        checkAvailable("ProviderAPI");
        enrichLocationInfo(req);
        Validator.validateInstanceRegisterRequest(req);
        long retryInterval = sdkContext.getConfig().getGlobal().getAPI().getRetryInterval();
        long timeout = getTimeout(req);
        while (timeout > 0) {
            long start = System.currentTimeMillis();
            ServiceCallResult serviceCallResult = new ServiceCallResult();
            CommonProviderRequest request = req.getRequest();
            try {
                CommonProviderResponse response = serverConnector.registerInstance(request, customHeader);
                LOG.info("register {}/{} instance {} succ", req.getNamespace(), req.getService(),
                        response.getInstanceID());
                serviceCallResult.setRetStatus(RetStatus.RetSuccess);
                serviceCallResult.setRetCode(ErrorCode.Success.getCode());
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
    public void deRegister(InstanceDeregisterRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateInstanceDeregisterRequest(req);
        RegisterStateManager.removeRegisterState(sdkContext, req);
        long retryInterval = sdkContext.getConfig().getGlobal().getAPI().getRetryInterval();
        long timeout = getTimeout(req);
        while (timeout > 0) {
            long start = System.currentTimeMillis();
            ServiceCallResult serviceCallResult = new ServiceCallResult();
            CommonProviderRequest request = req.getRequest();
            try {
                serverConnector.deregisterInstance(request);
                serviceCallResult.setRetStatus(RetStatus.RetSuccess);
                serviceCallResult.setRetCode(ErrorCode.Success.getCode());
                LOG.info("deregister instance {} succ", req);
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
    public void heartbeat(InstanceHeartbeatRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateHeartbeatRequest(req);
        long timeout = getTimeout(req);
        long retryInterval = sdkContext.getConfig().getGlobal().getAPI().getRetryInterval();
        while (timeout > 0) {
            long start = System.currentTimeMillis();
            ServiceCallResult serviceCallResult = new ServiceCallResult();
            CommonProviderRequest request = req.getRequest();
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

    private void enrichLocationInfo(InstanceRegisterRequest req) {
        if (StringUtils.isBlank(req.getRequest().getRegion()) && StringUtils.isNotBlank(LocationUtils.getRegion())) {
            req.setRegion(LocationUtils.getRegion());
        }
        if (StringUtils.isBlank(req.getRequest().getZone()) && StringUtils.isNotBlank(LocationUtils.getZone())) {
            req.setRegion(LocationUtils.getZone());
        }
        if (StringUtils.isBlank(req.getRequest().getCampus()) && StringUtils.isNotBlank(LocationUtils.getCampus())) {
            req.setRegion(LocationUtils.getCampus());
        }
    }
}
