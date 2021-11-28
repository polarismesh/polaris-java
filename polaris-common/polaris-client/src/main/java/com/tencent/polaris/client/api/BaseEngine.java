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

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.rpc.RequestBaseEntity;
import com.tencent.polaris.api.control.Destroyable;

/**
 * 基础构建引擎，API会基于该类进行实现
 *
 * @author andrewshan
 * @date 2019/8/21
 */
public abstract class BaseEngine extends Destroyable {

    protected final SDKContext sdkContext;

    public BaseEngine(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
    }

    public void init() throws PolarisException {
        sdkContext.init();
        subInit();
    }

    public SDKContext getSDKContext() {
        return sdkContext;
    }

    /**
     * 子类实现，用于做次级初始化
     */
    protected abstract void subInit() throws PolarisException;

    protected void checkAvailable(String apiName) throws PolarisException {
        if (isDestroyed()) {
            throw new PolarisException(ErrorCode.INVALID_STATE,
                    String.format("%s: api instance has been destroyed", apiName));
        }
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

    @Override
    protected void doDestroy() {
        sdkContext.doDestroy();
    }
}
