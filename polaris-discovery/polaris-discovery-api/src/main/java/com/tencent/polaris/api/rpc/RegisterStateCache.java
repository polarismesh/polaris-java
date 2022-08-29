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

package com.tencent.polaris.api.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 服务注册状态缓存
 *
 * @author wallezhang
 */
public class RegisterStateCache {

    private final static Map<String, RegisterState> REGISTER_STATES = new ConcurrentHashMap<>();

    /**
     * Put instance register state to cache
     *
     * @param instanceRegisterRequest instance register request
     * @return Return new instance register state object if it is not cached, otherwise null
     */
    public static RegisterState putRegisterState(InstanceRegisterRequest instanceRegisterRequest) {
        String registerStateKey = buildRegisterStateKey(instanceRegisterRequest);
        RegisterState registerState = REGISTER_STATES.get(registerStateKey);
        if (registerState == null) {
            registerState = new RegisterState();
            registerState.setInstanceRegisterRequest(instanceRegisterRequest);
            registerState.setFirstRegisterTime(System.currentTimeMillis());
            REGISTER_STATES.put(registerStateKey, registerState);
            return registerState;
        }
        return null;
    }

    /**
     * Remove the instance heartbeat task and cancel the task
     *
     * @param instanceDeregisterRequest instance deregister request
     */
    public static void removeRegisterState(InstanceDeregisterRequest instanceDeregisterRequest) {
        String registerStateKey = buildRegisterStateKey(instanceDeregisterRequest);
        RegisterState registerState = REGISTER_STATES.remove(registerStateKey);
        if (registerState != null) {
            registerState.getTaskFuture().cancel(false);
        }
    }

    public static void destroy() {
        for (RegisterState registerState : REGISTER_STATES.values()) {
            registerState.getTaskFuture().cancel(false);
        }
    }

    private static String buildRegisterStateKey(CommonProviderBaseEntity baseEntity) {
        return String.format("%s##%s##%s##%s", baseEntity.getNamespace(), baseEntity.getService(), baseEntity.getHost(),
                baseEntity.getPort());
    }

    public static final class RegisterState {

        private InstanceRegisterRequest instanceRegisterRequest;
        private long firstRegisterTime;
        private ScheduledFuture<?> taskFuture;
        private int heartbeatFailCounter = 0;

        /**
         * Increment fail count by one
         */
        public void incrementFailCount() {
            heartbeatFailCounter += 1;
        }

        public int getHeartbeatFailCounter() {
            return heartbeatFailCounter;
        }

        public void resetFailCount() {
            heartbeatFailCounter = 0;
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
    }
}
