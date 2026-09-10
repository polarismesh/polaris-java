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

package com.tencent.polaris.ai.client.api;

import com.tencent.polaris.ai.api.core.SkillAPI;
import com.tencent.polaris.ai.api.flow.SkillFlow;
import com.tencent.polaris.ai.client.utils.SkillValidator;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;

/**
 * Default SkillAPI implementation.
 */
public class DefaultSkillAPI extends BaseEngine implements SkillAPI {

    private SkillFlow skillFlow;

    public DefaultSkillAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() {
        skillFlow = sdkContext.getOrInitFlow(SkillFlow.class);
    }

    @Override
    public SkillGetResponse getSkill(SkillGetRequest request) throws PolarisException {
        checkAvailable("SkillFlow");
        SkillValidator.validateGetRequest(request);
        return skillFlow.getSkill(request);
    }

    @Override
    public SkillListResponse listSkills(SkillListRequest request) throws PolarisException {
        checkAvailable("SkillFlow");
        SkillValidator.validateListRequest(request);
        return skillFlow.listSkills(request);
    }

    @Override
    public SkillDownloadResponse downloadSkill(SkillDownloadRequest request) throws PolarisException {
        checkAvailable("SkillFlow");
        SkillValidator.validateDownloadRequest(request);
        return skillFlow.downloadSkill(request);
    }
}
