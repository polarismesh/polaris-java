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

package com.tencent.polaris.client.api;

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.common.ValueContext;
import com.tencent.polaris.api.plugin.server.TargetServer;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;

/**
 * 基础构建引擎，API会基于该类进行实现
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public abstract class BaseEngine extends Destroyable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseEngine.class);

    private static final String CTX_KEY_ENGINE = "key_engine";

    protected final SDKContext sdkContext;

    private List<ServiceCallResultListener> serviceCallResultListeners;

    public BaseEngine(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
    }

    public void init() throws PolarisException {
        sdkContext.init();
        subInit();
        serviceCallResultListeners = ServiceCallResultListener.getServiceCallResultListeners(sdkContext);
        sdkContext.getValueContext().setValue(CTX_KEY_ENGINE, this);
    }

    public SDKContext getSDKContext() {
        return sdkContext;
    }

    /**
     * 子类实现，用于做次级初始化
     */
    protected abstract void subInit() throws PolarisException;

    protected void checkAvailable(String apiName) throws PolarisException {
        checkAvailable(apiName, true);
    }

    protected boolean checkAvailable(String apiName, boolean withException) {
        if (isDestroyed()) {
            String errMsg = String.format("%s: api instance has been destroyed", apiName);
            if (withException) {
                throw new PolarisException(ErrorCode.INVALID_STATE, errMsg);
            } else {
                LOGGER.error(errMsg);
                return false;
            }
        }
        return true;
    }

    /**
     * 获取API超时时间
     *
     * @param entity entity
     * @return 超时时间，单位毫秒
     */
    protected long getTimeout(RequestBaseEntity entity) {
        return entity.getTimeoutMs() == 0 ? sdkContext.getConfig().getGlobal().getAPI().getTimeout()
                : entity.getTimeoutMs();
    }

    /**
     * 上报调用结果数据
     *
     * @param req 调用结果数据
     * @throws PolarisException
     */
    protected void reportInvokeStat(ServiceCallResult req) throws PolarisException {
        for (ServiceCallResultListener listener : serviceCallResultListeners) {
            listener.onServiceCallResult(req);
        }
    }

    @Override
    protected void doDestroy() {
        sdkContext.doDestroy();
    }

    public static BaseEngine getEngine(ValueContext valueContext) {
        return valueContext.getValue(CTX_KEY_ENGINE);
    }

    /**
     * 上报内部服务调用结果
     *
     * @param serviceCallResult 服务调用结果
     * @param targetServer 目标服务端
     * @param method 方法
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
}
