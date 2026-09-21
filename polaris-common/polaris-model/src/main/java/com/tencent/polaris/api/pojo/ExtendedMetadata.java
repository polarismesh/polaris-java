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

/**
 * Extended metadata attached to a service.
 */
public class ExtendedMetadata {

    /**
     * Supported extended metadata types.
     */
    public enum ExtendedMetadataType {
        UNKNOWN,
        SKILL
    }

    private ExtendedMetadataType type = ExtendedMetadataType.UNKNOWN;

    private AgentSkill agentSkill;

    public ExtendedMetadataType getType() {
        return type;
    }

    public AgentSkill getAgentSkill() {
        return agentSkill;
    }

    public static ExtendedMetadataBuilder builder() {
        return new ExtendedMetadataBuilder();
    }

    /**
     * Builder for {@link ExtendedMetadata}.
     */
    public static final class ExtendedMetadataBuilder {

        private final ExtendedMetadata metadata = new ExtendedMetadata();

        private ExtendedMetadataBuilder() {
        }

        public ExtendedMetadataBuilder type(ExtendedMetadataType type) {
            metadata.type = type;
            return this;
        }

        public ExtendedMetadataBuilder agentSkill(AgentSkill agentSkill) {
            metadata.agentSkill = agentSkill;
            return this;
        }

        public ExtendedMetadata build() {
            return metadata;
        }
    }
}
