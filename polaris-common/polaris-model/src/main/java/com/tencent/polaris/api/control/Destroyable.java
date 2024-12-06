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

package com.tencent.polaris.api.control;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 所有可销毁的对象的基类型
 *
 * @author andrewshan
 * @date 2019/8/22
 */
public abstract class Destroyable {

    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    /**
     * 子类去实现销毁逻辑
     */
    protected void doDestroy() {
        
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            doDestroy();
        }
    }

    /**
     * 获取插件名字
     *
     * @return String
     */
    public String getName() {
        return "Destroyable";
    }

    /**
     * 是否已经销毁
     *
     * @return boolean
     */
    public boolean isDestroyed() {
        return destroyed.get();
    }

    /**
     * 检查是否已经销毁，已销毁则抛出异常
     *
     * @throws PolarisException 已销毁返回异常
     */
    protected void checkDestroyed() throws PolarisException {
        if (isDestroyed()) {
            throw new PolarisException(ErrorCode.INVALID_STATE,
                    String.format("Plugin %s has been destroyed", getName()));
        }
    }
}
