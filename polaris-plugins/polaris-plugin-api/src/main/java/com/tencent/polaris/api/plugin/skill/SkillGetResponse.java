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

/**
 * GetSkill response.
 */
public class SkillGetResponse {

    private int code;

    private String info;

    private SkillResource resource;

    private SkillResourceVersion resourceVersion;

    private String content;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public SkillResource getResource() {
        return resource;
    }

    public void setResource(SkillResource resource) {
        this.resource = resource;
    }

    public SkillResourceVersion getResourceVersion() {
        return resourceVersion;
    }

    public void setResourceVersion(SkillResourceVersion resourceVersion) {
        this.resourceVersion = resourceVersion;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
