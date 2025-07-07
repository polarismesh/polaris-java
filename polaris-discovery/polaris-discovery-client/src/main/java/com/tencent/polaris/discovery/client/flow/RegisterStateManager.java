/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

import com.tencent.polaris.api.rpc.CommonProviderBaseEntity;
import com.tencent.polaris.api.rpc.InstanceDeregisterRequest;
import com.tencent.polaris.api.rpc.InstanceRegisterRequest;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 注册状态管理器
 *
 * @author wallezhang
 */
public class RegisterStateManager {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterStateManager.class);

    private final static Map<String, Map<String, RegisterState>> REGISTER_STATES = new ConcurrentHashMap<>();

    /**
     * Put instance register state to cache
     *
     * @param sdkContext sdk context
     * @param instanceRegisterRequest instance register request
     * @return Return new instance register state object if it is not cached, otherwise null
     */
    public static RegisterState putRegisterState(SDKContext sdkContext,
            InstanceRegisterRequest instanceRegisterRequest) {
        String registerStateKey = buildRegisterStateKey(instanceRegisterRequest);
        Map<String, RegisterState> sdkRegisterStates = REGISTER_STATES.computeIfAbsent(
                sdkContext.getValueContext().getClientId(), clientId -> new ConcurrentHashMap<>());
        RegisterState existsRegisterState = sdkRegisterStates.get(registerStateKey);
        if (existsRegisterState != null) {
             existsRegisterState.setInstanceRegisterRequest(instanceRegisterRequest);
            return null;
        }
        return sdkRegisterStates.computeIfAbsent(registerStateKey, unused -> {
            RegisterState registerState = new RegisterState();
            registerState.setInstanceRegisterRequest(instanceRegisterRequest);
            registerState.setFirstRegisterTime(System.currentTimeMillis());
            return registerState;
        });
    }

    /**
     * Get instance register state from cache
     *
     * @param sdkContext              sdk context
     * @param instanceRegisterRequest instance register request
     * @return Return instance register state object if it is cached, otherwise null
     */
    public static RegisterState getRegisterState(SDKContext sdkContext, InstanceRegisterRequest instanceRegisterRequest) {
        String registerStateKey = buildRegisterStateKey(instanceRegisterRequest);
        Map<String, RegisterState> sdkRegisterStates = REGISTER_STATES.computeIfAbsent(
                sdkContext.getValueContext().getClientId(), clientId -> new ConcurrentHashMap<>());
        return sdkRegisterStates.get(registerStateKey);
    }

    /**
     * Remove the instance heartbeat task and cancel the task
     *
     * @param sdkContext sdk context
     * @param instanceDeregisterRequest instance deregister request
     */
    public static void removeRegisterState(SDKContext sdkContext, InstanceDeregisterRequest instanceDeregisterRequest) {
        Optional.ofNullable(REGISTER_STATES.get(sdkContext.getValueContext().getClientId()))
                .ifPresent(sdkRegisterStates -> {
                    String registerStateKey = buildRegisterStateKey(instanceDeregisterRequest);
                    Optional.ofNullable(sdkRegisterStates.remove(registerStateKey))
                            .ifPresent(RegisterState::destroy);
                });
    }

    public static void destroy(SDKContext sdkContext) {
        Optional.ofNullable(REGISTER_STATES.remove(sdkContext.getValueContext().getClientId()))
                .ifPresent(sdkRegisterStates -> {
                    for (RegisterState registerState : sdkRegisterStates.values()) {
                        registerState.destroy();
                    }
                    sdkRegisterStates.clear();
                });
    }

    private static String buildRegisterStateKey(CommonProviderBaseEntity baseEntity) {
        return String.format("%s##%s##%s##%s", baseEntity.getNamespace(), baseEntity.getService(), baseEntity.getHost(),
                baseEntity.getPort());
    }

    public static class RegisterState {

        private volatile InstanceRegisterRequest instanceRegisterRequest;
        private long firstRegisterTime;
        private ScheduledFuture<?> taskFuture;
        private final AtomicInteger heartbeatFailCounter = new AtomicInteger(0);
        private ScheduledFuture<?> reRegisterFuture;
        private final ScheduledExecutorService reRegisterExecutor;
        private final AtomicInteger reRegisterCounter = new AtomicInteger(0);

        public RegisterState() {
            this.reRegisterExecutor = Executors.newSingleThreadScheduledExecutor();
        }

        /**
         * Increment fail count by one
         */
        public void incrementFailCount() {
            heartbeatFailCounter.incrementAndGet();
        }

        public int getHeartbeatFailCounter() {
            return heartbeatFailCounter.get();
        }

        public void resetFailCount() {
            heartbeatFailCounter.set(0);
        }

        public InstanceRegisterRequest getInstanceRegisterRequest() {
            return instanceRegisterRequest;
        }

        public void setInstanceRegisterRequest(InstanceRegisterRequest instanceRegisterRequest) {
            this.instanceRegisterRequest = instanceRegisterRequest;
        }

        public long getFirstRegisterTime() {
            return firstRegisterTime;
        }

        public void setFirstRegisterTime(long firstRegisterTime) {
            this.firstRegisterTime = firstRegisterTime;
        }

        public ScheduledFuture<?> getTaskFuture() {
            return taskFuture;
        }

        public void setTaskFuture(ScheduledFuture<?> taskFuture) {
            this.taskFuture = taskFuture;
        }

        public ScheduledFuture<?> getReRegisterFuture() {
            return reRegisterFuture;
        }

        public void setReRegisterFuture(ScheduledFuture<?> reRegisterFuture) {
            this.reRegisterFuture = reRegisterFuture;
        }

        public ScheduledExecutorService getReRegisterExecutor() {
            return reRegisterExecutor;
        }

        public int getReRegisterCounter() {
            return reRegisterCounter.get();
        }

        public void incrementReRegisterCounter() {
            reRegisterCounter.incrementAndGet();
        }

        public void resetReRegisterCounter() {
            reRegisterCounter.set(0);
        }

        public void destroy() {
            try {
                if (getTaskFuture() != null) {
                    getTaskFuture().cancel(false);
                }
                if (getReRegisterFuture() != null) {
                    getReRegisterFuture().cancel(false);
                }
                if (getReRegisterExecutor() != null) {
                    getReRegisterExecutor().shutdownNow();
                }
            } catch (Throwable throwable) {
                LOG.warn("[RegisterState] destroy error. namespace:{}, service:{}, host:{}, port:{}.",
                        getInstanceRegisterRequest().getNamespace(), getInstanceRegisterRequest().getService(),
                        getInstanceRegisterRequest().getHost(), getInstanceRegisterRequest().getPort(), throwable);
            }
        }
    }
}
