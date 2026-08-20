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

package com.tencent.polaris.ai.api.flow;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillDownloadResponse;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetResponse;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.plugin.skill.SkillListResponse;
import com.tencent.polaris.client.flow.AbstractFlow;

/**
 * Skill flow SPI.
 */
public interface SkillFlow extends AbstractFlow {

    /**
     * Get a published skill.
     *
     * @param request get request
     * @return get response
     * @throws PolarisException polaris exception
     */
    default SkillGetResponse getSkill(SkillGetRequest request) throws PolarisException {
        return null;
    }

    /**
     * List published skills.
     *
     * @param request list request
     * @return list response
     * @throws PolarisException polaris exception
     */
    default SkillListResponse listSkills(SkillListRequest request) throws PolarisException {
        return null;
    }

    /**
     * Download a skill package.
     *
     * @param request download request
     * @return download response
     * @throws PolarisException polaris exception
     */
    default SkillDownloadResponse downloadSkill(SkillDownloadRequest request) throws PolarisException {
        return null;
    }
}
