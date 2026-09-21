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

package com.tencent.polaris.api.pojo;

import java.util.Collections;
import java.util.List;

/**
 * AI agent skill metadata attached to a service.
 */
public class AgentSkill {

    private String id;

    private String name;

    private String description;

    private List<String> tags = Collections.emptyList();

    private List<String> examples = Collections.emptyList();

    private List<String> inputModes = Collections.emptyList();

    private List<String> outputModes = Collections.emptyList();

    private String version;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getExamples() {
        return examples;
    }

    public List<String> getInputModes() {
        return inputModes;
    }

    public List<String> getOutputModes() {
        return outputModes;
    }

    public String getVersion() {
        return version;
    }

    public static AgentSkillBuilder builder() {
        return new AgentSkillBuilder();
    }

    /**
     * Builder for {@link AgentSkill}.
     */
    public static final class AgentSkillBuilder {

        private final AgentSkill skill = new AgentSkill();

        private AgentSkillBuilder() {
        }

        public AgentSkillBuilder id(String id) {
            skill.id = id;
            return this;
        }

        public AgentSkillBuilder name(String name) {
            skill.name = name;
            return this;
        }

        public AgentSkillBuilder description(String description) {
            skill.description = description;
            return this;
        }

        public AgentSkillBuilder tags(List<String> tags) {
            skill.tags = tags;
            return this;
        }

        public AgentSkillBuilder examples(List<String> examples) {
            skill.examples = examples;
            return this;
        }

        public AgentSkillBuilder inputModes(List<String> inputModes) {
            skill.inputModes = inputModes;
            return this;
        }

        public AgentSkillBuilder outputModes(List<String> outputModes) {
            skill.outputModes = outputModes;
            return this;
        }

        public AgentSkillBuilder version(String version) {
            skill.version = version;
            return this;
        }

        public AgentSkill build() {
            return skill;
        }
    }
}
