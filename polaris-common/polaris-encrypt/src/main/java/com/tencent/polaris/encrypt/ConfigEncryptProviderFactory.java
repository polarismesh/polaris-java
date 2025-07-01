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

package com.tencent.polaris.encrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigEncryptProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(ConfigEncryptProviderFactory.class);

    private static ConfigEncryptProvider configEncryptProvider = null;

    public static ConfigEncryptProvider getInstance() {
        if (null == configEncryptProvider) {
            try {
                Class<?> providerClass = Class.forName(EncryptConfig.getProviderClass());
                configEncryptProvider = (ConfigEncryptProvider) providerClass.newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                log.error("get config encrypt provider error", e);
            }
        }
        return configEncryptProvider;
    }
}
