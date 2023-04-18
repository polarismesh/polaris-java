package com.tencent.polaris.discovery.client.api;

import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.RetriableException;
import com.tencent.polaris.api.flow.DiscoveryFlow;
import com.tencent.polaris.api.plugin.route.LocationLevel;
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
import com.tencent.polaris.client.util.Utils;
import com.tencent.polaris.discovery.client.flow.RegisterFlow;
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

    private final DiscoveryFlow discoveryFlow;

    public DefaultProviderAPI(SDKContext sdkContext) {
        super(sdkContext);
        discoveryFlow = DiscoveryFlow.loadDiscoveryFlow(
                sdkContext.getConfig().getGlobal().getSystem().getFlowConfig().getName());
    }

    @Override
    protected void subInit() {
        discoveryFlow.setSDKContext(sdkContext);
    }

    @Override
    public InstanceRegisterResponse registerInstance(InstanceRegisterRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateInstanceRegisterRequest(req);
        req.setAutoHeartbeat(true);
        return discoveryFlow.register(req);
    }

    @Override
    protected void doDestroy() {
        RegisterStateManager.destroy(sdkContext);
        super.doDestroy();
    }

    @Override
    public InstanceRegisterResponse register(InstanceRegisterRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateInstanceRegisterRequest(req);
        return discoveryFlow.register(req);
    }

    @Override
    public void deRegister(InstanceDeregisterRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateInstanceDeregisterRequest(req);
        discoveryFlow.deRegister(req);
    }

    @Override
    public void heartbeat(InstanceHeartbeatRequest req) throws PolarisException {
        checkAvailable("ProviderAPI");
        Validator.validateHeartbeatRequest(req);
        discoveryFlow.heartbeat(req);
    }

}
