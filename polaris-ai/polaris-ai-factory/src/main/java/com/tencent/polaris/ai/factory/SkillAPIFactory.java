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

package com.tencent.polaris.ai.factory;

import com.tencent.polaris.ai.api.core.SkillAPI;
import com.tencent.polaris.ai.client.api.DefaultSkillAPI;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;

/**
 * Factory to create SkillAPI.
 */
public final class SkillAPIFactory {

    private SkillAPIFactory() {
    }

    /**
     * Create SkillAPI by default config.
     *
     * @return SkillAPI
     * @throws PolarisException init error
     */
    public static SkillAPI createSkillAPI() throws PolarisException {
        Configuration configuration = ConfigAPIFactory.defaultConfig();
        return createSkillAPIByConfig(configuration);
    }

    /**
     * Create SkillAPI by SDKContext.
     *
     * @param sdkContext context
     * @return SkillAPI
     * @throws PolarisException init error
     */
    public static SkillAPI createSkillAPIByContext(SDKContext sdkContext) throws PolarisException {
        DefaultSkillAPI defaultSkillAPI = new DefaultSkillAPI(sdkContext);
        defaultSkillAPI.init();
        return defaultSkillAPI;
    }

    /**
     * Create SkillAPI by configuration.
     *
     * @param config configuration
     * @return SkillAPI
     * @throws PolarisException init error
     */
    public static SkillAPI createSkillAPIByConfig(Configuration config) throws PolarisException {
        SDKContext context = SDKContext.initContextByConfig(config);
        return createSkillAPIByContext(context);
    }
}
