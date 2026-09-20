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

package com.tencent.polaris.ai.client.utils;

import com.tencent.polaris.api.exception.ErrorCode;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.skill.SkillDownloadRequest;
import com.tencent.polaris.api.plugin.skill.SkillGetRequest;
import com.tencent.polaris.api.plugin.skill.SkillListRequest;
import com.tencent.polaris.api.utils.StringUtils;

/**
 * Request validators for SkillAPI.
 */
public final class SkillValidator {

    private SkillValidator() {
    }

    /**
     * Validate GetSkill request.
     *
     * @param request request
     */
    public static void validateGetRequest(SkillGetRequest request) {
        if (request == null) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "SkillGetRequest can not be null");
        }
        validateNameNamespace(request.getName(), request.getNamespace());
    }

    /**
     * Validate list request.
     *
     * @param request request
     */
    public static void validateListRequest(SkillListRequest request) {
        if (request == null) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "SkillListRequest can not be null");
        }
    }

    /**
     * Validate download request.
     *
     * @param request request
     */
    public static void validateDownloadRequest(SkillDownloadRequest request) {
        if (request == null) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "SkillDownloadRequest can not be null");
        }
        validateNameNamespace(request.getName(), request.getNamespace());
    }

    private static void validateNameNamespace(String name, String namespace) {
        if (StringUtils.isBlank(name)) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "skill name can not be empty");
        }
        if (StringUtils.isBlank(namespace)) {
            throw new PolarisException(ErrorCode.API_INVALID_ARGUMENT, "skill namespace can not be empty");
        }
    }
}
