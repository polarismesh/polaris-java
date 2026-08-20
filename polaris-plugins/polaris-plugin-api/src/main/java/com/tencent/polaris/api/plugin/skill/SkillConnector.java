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

package com.tencent.polaris.api.plugin.skill;

import com.tencent.polaris.api.plugin.Plugin;

/**
 * Connector SPI for Polaris Skill RPCs.
 */
public interface SkillConnector extends Plugin {

    /**
     * Get a published skill by name and version.
     *
     * @param request get request
     * @return get response
     */
    SkillGetResponse getSkill(SkillGetRequest request);

    /**
     * List published skills.
     *
     * @param request list request
     * @return list response
     */
    SkillListResponse listSkills(SkillListRequest request);

    /**
     * Download a skill package as markdown or zip.
     *
     * @param request download request
     * @return assembled download response
     */
    SkillDownloadResponse downloadSkill(SkillDownloadRequest request);
}
