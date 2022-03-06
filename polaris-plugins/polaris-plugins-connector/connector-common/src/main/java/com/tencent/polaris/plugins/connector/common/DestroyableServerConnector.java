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

package com.tencent.polaris.plugins.connector.common;

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.plugin.server.ServerConnector;

/**
 * Destroyable server connector.
 *
 * @author Haotian Zhang
 */
public abstract class DestroyableServerConnector extends Destroyable implements ServerConnector {

    /**
     * Check if initialized.
     *
     * @return true if initialized.
     */
    public abstract boolean isInitialized();

    /**
     * Retry service update task.
     *
     * @param updateTask
     */
    public abstract void retryServiceUpdateTask(ServiceUpdateTask updateTask);

    /**
     * Add long-running task to destroyable server connector.
     *
     * @param serviceUpdateTask
     */
    public abstract void addLongRunningTask(ServiceUpdateTask serviceUpdateTask);
}
