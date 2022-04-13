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

package com.tencent.polaris.api.config.provider;

import com.tencent.polaris.api.config.verify.Verifier;

/**
 * Configuration of register.
 *
 * @author Haotian Zhang
 */
public interface RegisterConfig extends Verifier {

    /**
     * 获取命名空间
     *
     * @return namespace
     */
    String getNamespace();

    /**
     * 获取服务名
     *
     * @return service
     */
    String getService();

    /**
     * Get id of discovery server connector.
     *
     * @return id
     */
    String getServerConnectorId();

    /**
     * If registration is enabled.
     *
     * @return boolean
     */
    boolean isEnable();
}
