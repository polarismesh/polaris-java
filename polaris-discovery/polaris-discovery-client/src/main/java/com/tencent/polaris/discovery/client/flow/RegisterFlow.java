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

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterResponse;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.discovery.client.flow.RegisterStateManager.RegisterState;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.logging.LoggingConsts;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 异步注册流
 *
 * @author wallezhang
 */
public class RegisterFlow {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingConsts.LOGGING_HEARTBEAT_RECORD);
    /**
     * 异步注册header key
     */
    private static final String HEADER_KEY_ASYNC_REGIS = "async-regis";
    private static final int HEARTBEAT_FAIL_COUNT_THRESHOLD = 2;
    private final SDKContext sdkContext;
    private final ScheduledThreadPoolExecutor asyncRegisterExecutor;

    public RegisterFlow(SDKContext sdkContext) {
        int threadSize = sdkContext.getConfig().getProvider().getHeartbeatWorkerSize();
        this.sdkContext = sdkContext;
        this.asyncRegisterExecutor = new ScheduledThreadPoolExecutor(threadSize, new NamedThreadFactory("async-register"));
    }

    public InstanceRegisterResponse registerInstance(InstanceRegisterRequest request, RegisterFunction registerFunction,
            HeartbeatFunction heartbeatFunction) {
        InstanceRegisterResponse instanceRegisterResponse = registerFunction.doRegister(request,
                createRegisterV2Header());
        RegisterState registerState = RegisterStateManager.putRegisterState(sdkContext, request);
        if (registerState != null) {
            registerState.setTaskFuture(asyncRegisterExecutor.scheduleAtFixedRate(
                    () -> {
                        try {
                            doRunHeartbeat(registerState, registerFunction, heartbeatFunction);
                        } catch (Throwable e) {
                            LOG.error("[AsyncHeartbeat]Re-register instance failed, namespace:{}, service:{}, host:{}, port:{}",
                                    request.getNamespace(), request.getService(), request.getHost(), request.getPort(), e);
                        }
                    }, request.getTtl(),
                    request.getTtl(), TimeUnit.SECONDS));
        }
        return instanceRegisterResponse;
    }

    void doRunHeartbeat(RegisterState registerState, RegisterFunction registerFunction,
            HeartbeatFunction heartbeatFunction) {
        InstanceRegisterRequest registerRequest = registerState.getInstanceRegisterRequest();
        LOG.info("[AsyncHeartbeat]Instance heartbeat task started, namespace:{}, service:{}, host:{}, port:{}",
                registerRequest.getNamespace(), registerRequest.getService(), registerRequest.getHost(),
                registerRequest.getPort());
        try {
            heartbeatFunction.doHeartbeat(buildHeartbeatRequest(registerRequest));
            registerState.resetFailCount();
            LOG.info("[AsyncHeartbeat]Instance heartbeat success. Reset fail count. namespace:{}, service:{}, host:{}, port:{}",
                    registerRequest.getNamespace(), registerRequest.getService(), registerRequest.getHost(),
                    registerRequest.getPort());
        } catch (PolarisException e) {
            if (e.getServerErrCode() == ServerCodes.NOT_FOUND_RESOURCE) {
                registerState.incrementFailCount();
                LOG.error("[AsyncHeartbeat]Instance heartbeat failed because of NOT_FOUND_RESOURCE. Increase fail count. " +
                                "namespace:{}, service:{}, host:{}, port:{}, heartbeat fail count:{}",
                        registerRequest.getNamespace(), registerRequest.getService(), registerRequest.getHost(),
                        registerRequest.getPort(), registerState.getHeartbeatFailCounter(), e);
            } else {
                registerState.resetFailCount();
                LOG.error("[AsyncHeartbeat]Instance heartbeat failed not because of NOT_FOUND_RESOURCE. Reset fail count. " +
                                "namespace:{}, service:{}, host:{}, port:{}, heartbeat fail count:{}",
                        registerRequest.getNamespace(), registerRequest.getService(), registerRequest.getHost(),
                        registerRequest.getPort(), registerState.getHeartbeatFailCounter(), e);
            }
        }

        long minRegisterInterval = sdkContext.getConfig().getProvider().getMinRegisterInterval();
        long sinceFirstRegister = System.currentTimeMillis() - registerState.getFirstRegisterTime();
        if (sinceFirstRegister < minRegisterInterval
                || registerState.getHeartbeatFailCounter() < HEARTBEAT_FAIL_COUNT_THRESHOLD) {
            return;
        }

        synchronized (registerState) {
            if (registerState.getReRegisterFuture() == null
                    || registerState.getReRegisterFuture().isDone()
                    || registerState.getReRegisterFuture().isCancelled()) {
                int reRegisterCounter = registerState.getReRegisterCounter();
                double base = reRegisterCounter == 0 ? 0 : registerRequest.getTtl() * Math.pow(2, reRegisterCounter - 1);
                int offset = reRegisterCounter == 0 ? 0 : new Random().nextInt(registerRequest.getTtl());
                long delay = (long) Math.min(base + offset, 60);
                LOG.info("[AsyncHeartbeat]Re-register instance, namespace:{}, service:{}, host:{}, port:{}, count:{}, delay:{}s",
                        registerRequest.getNamespace(), registerRequest.getService(), registerRequest.getHost(),
                        registerRequest.getPort(), reRegisterCounter, delay);
                registerState.setReRegisterFuture(registerState.getReRegisterExecutor().schedule(() -> {
                    try {
                        registerFunction.doRegister(registerRequest, createRegisterV2Header());
                        LOG.info("[AsyncHeartbeat]Re-register instance success, namespace:{}, service:{}, host:{}, port:{}",
                                registerRequest.getNamespace(), registerRequest.getService(), registerRequest.getHost(),
                                registerRequest.getPort());
                        registerState.resetFailCount();
                        registerState.resetReRegisterCounter();
                    } catch (PolarisException e) {
                        LOG.error(
                                "[AsyncHeartbeat]Re-register instance failed, namespace:{}, service:{}, host:{}, port:{}, re-register count:{}",
                                registerRequest.getNamespace(), registerRequest.getService(), registerRequest.getHost(),
                                registerRequest.getPort(), reRegisterCounter, e);
                    }
                }, delay, TimeUnit.SECONDS));
                registerState.incrementReRegisterCounter();
            }
        }
    }

    private InstanceHeartbeatRequest buildHeartbeatRequest(InstanceRegisterRequest registerRequest) {
        InstanceHeartbeatRequest instanceHeartbeatRequest = new InstanceHeartbeatRequest();
        instanceHeartbeatRequest.setService(registerRequest.getService());
        instanceHeartbeatRequest.setNamespace(registerRequest.getNamespace());
        instanceHeartbeatRequest.setToken(registerRequest.getToken());
        instanceHeartbeatRequest.setHost(registerRequest.getHost());
        instanceHeartbeatRequest.setPort(registerRequest.getPort());
        instanceHeartbeatRequest.setInstanceID(registerRequest.getInstanceId());
        return instanceHeartbeatRequest;
    }

    private Map<String, String> createRegisterV2Header() {
        Map<String, String> header = new HashMap<>(1);
        header.put(HEADER_KEY_ASYNC_REGIS, "true");
        return header;
    }

    @FunctionalInterface
    public interface RegisterFunction {

        /**
         * 发送实例注册请求
         *
         * @param request 实例注册请求体
         * @param customHeader 自定义头部
         * @return 注册响应
         */
        InstanceRegisterResponse doRegister(InstanceRegisterRequest request, Map<String, String> customHeader);
    }

    @FunctionalInterface
    public interface HeartbeatFunction {

        /**
         * 发送心跳请求
         *
         * @param request 心跳请求体
         */
        void doHeartbeat(InstanceHeartbeatRequest request);
    }

	public void destroy() {
		asyncRegisterExecutor.shutdownNow();
	}
}
