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
import com.tencent.polaris.api.config.provider.ProviderConfig;
import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.exception.ServerCodes;
import com.tencent.polaris.api.rpc.InstanceHeartbeatRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.discovery.client.flow.RegisterFlow.HeartbeatFunction;
import com.tencent.polaris.discovery.client.flow.RegisterFlow.RegisterFunction;
import com.tencent.polaris.discovery.client.flow.RegisterStateManager.RegisterState;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Test for {@link RegisterFlow}.
 *
 * @author Haotian Zhang
 */
public class RegisterFlowTest {

    private RegisterFlow registerFlow;
    private RegisterState registerState;
    private RegisterFunction registerFunction;
    private HeartbeatFunction heartbeatFunction;
    private ScheduledExecutorService reRegisterExecutor;
    private ScheduledFuture reRegisterFuture;

    @Before
    public void setUp() {
        ProviderConfig providerConfig = mock(ProviderConfig.class);
        when(providerConfig.getHeartbeatWorkerSize()).thenReturn(4);
        when(providerConfig.getMinRegisterInterval()).thenReturn(30000L);
        Configuration configuration = mock(Configuration.class);
        when(configuration.getProvider()).thenReturn(providerConfig);
        SDKContext sdkContext = mock(SDKContext.class);
        when(sdkContext.getConfig()).thenReturn(configuration);
        registerFlow = new RegisterFlow(sdkContext);

        registerState = mock(RegisterState.class);
        registerFunction = mock(RegisterFunction.class);
        heartbeatFunction = mock(HeartbeatFunction.class);
        InstanceRegisterRequest registerRequest = mock(InstanceRegisterRequest.class);
        reRegisterExecutor = mock(ScheduledExecutorService.class);
        reRegisterFuture = mock(ScheduledFuture.class);

        when(registerState.getInstanceRegisterRequest()).thenReturn(registerRequest);
        when(registerState.getReRegisterExecutor()).thenReturn(reRegisterExecutor);
        when(registerState.getFirstRegisterTime()).thenReturn(System.currentTimeMillis());
        when(registerState.getHeartbeatFailCounter()).thenReturn(0);
        when(registerState.getReRegisterCounter()).thenReturn(0);

        when(registerRequest.getNamespace()).thenReturn("test-namespace");
        when(registerRequest.getService()).thenReturn("test-service");
        when(registerRequest.getHost()).thenReturn("127.0.0.1");
        when(registerRequest.getPort()).thenReturn(8080);
        when(registerRequest.getTtl()).thenReturn(5);
    }

    @Test
    public void testDoRunHeartbeat_Success() throws PolarisException {
        // 准备
        doNothing().when(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));

        // 执行
        registerFlow.doRunHeartbeat(registerState, registerFunction, heartbeatFunction);

        // 验证
        verify(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));
        verify(registerState).resetFailCount();
        verify(registerState, never()).incrementFailCount();
    }

    @Test
    public void testDoRunHeartbeat_Failure_NotFoundResource() throws PolarisException {
        // 准备
        PolarisException notFoundException = new PolarisException(ErrorCode.SERVER_USER_ERROR, "Not Found Resource");
        notFoundException.setServerErrCode(ServerCodes.NOT_FOUND_RESOURCE);
        doThrow(notFoundException).when(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));

        // 执行
        registerFlow.doRunHeartbeat(registerState, registerFunction, heartbeatFunction);

        // 验证
        verify(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));
        verify(registerState).incrementFailCount();
        verify(registerState, never()).resetFailCount();
    }

    @Test
    public void testDoRunHeartbeat_Failure_OtherError() throws PolarisException {
        // 准备
        PolarisException otherException = new PolarisException(ErrorCode.SERVER_USER_ERROR, "Internal error");
        doThrow(otherException).when(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));

        // 执行
        registerFlow.doRunHeartbeat(registerState, registerFunction, heartbeatFunction);

        // 验证
        verify(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));
        verify(registerState).resetFailCount();
        verify(registerState, never()).incrementFailCount();
    }

    @Test
    public void testDoRunHeartbeat_ReRegister_WhenFailCountExceedsThreshold() throws PolarisException {
        // 准备
        PolarisException notFoundException = new PolarisException(ErrorCode.SERVER_USER_ERROR, "Not Found Resource");
        notFoundException.setServerErrCode(ServerCodes.NOT_FOUND_RESOURCE);
        doThrow(notFoundException).when(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));
        when(registerState.getHeartbeatFailCounter()).thenReturn(2);
        when(registerState.getReRegisterFuture()).thenReturn(null);
        when(registerState.getFirstRegisterTime()).thenReturn(System.currentTimeMillis() - 30000); // 30s前注册
        when(reRegisterExecutor.schedule(any(Runnable.class), anyLong(), any(TimeUnit.class))).thenReturn(reRegisterFuture);

        // 执行
        registerFlow.doRunHeartbeat(registerState, registerFunction, heartbeatFunction);

        // 验证
        verify(registerState).setReRegisterFuture(any(ScheduledFuture.class));
        verify(registerState).incrementReRegisterCounter();
    }

    @Test
    public void testDoRunHeartbeat_NoReRegister_WhenMinIntervalNotReached() throws PolarisException {
        // 准备
        PolarisException notFoundException = new PolarisException(ErrorCode.SERVER_USER_ERROR, "Not Found Resource");
        notFoundException.setServerErrCode(ServerCodes.NOT_FOUND_RESOURCE);
        doThrow(notFoundException).when(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));
        when(registerState.getHeartbeatFailCounter()).thenReturn(2);
        when(registerState.getFirstRegisterTime()).thenReturn(System.currentTimeMillis()); // 刚刚注册

        // 执行
        registerFlow.doRunHeartbeat(registerState, registerFunction, heartbeatFunction);

        // 验证
        verify(registerState, never()).setReRegisterFuture(any(ScheduledFuture.class));
        verify(registerState, never()).incrementReRegisterCounter();
    }

    @Test
    public void testDoRunHeartbeat_NoReRegister_WhenFutureNotDone() throws PolarisException {
        // 准备
        PolarisException notFoundException = new PolarisException(ErrorCode.SERVER_USER_ERROR, "Not Found Resource");
        notFoundException.setServerErrCode(ServerCodes.NOT_FOUND_RESOURCE);
        doThrow(notFoundException).when(heartbeatFunction).doHeartbeat(any(InstanceHeartbeatRequest.class));
        when(registerState.getHeartbeatFailCounter()).thenReturn(2);
        when(registerState.getReRegisterFuture()).thenReturn(reRegisterFuture);
        when(reRegisterFuture.isDone()).thenReturn(false);
        when(reRegisterFuture.isCancelled()).thenReturn(false);

        // 执行
        registerFlow.doRunHeartbeat(registerState, registerFunction, heartbeatFunction);

        // 验证
        verify(registerState, never()).setReRegisterFuture(any(ScheduledFuture.class));
        verify(registerState, never()).incrementReRegisterCounter();
    }
}
