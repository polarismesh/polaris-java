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

package com.tencent.polaris.client.pojo;

import com.tencent.polaris.api.pojo.AgentSkill;
import com.tencent.polaris.api.pojo.ExtendedMetadata;
import com.tencent.polaris.api.pojo.ExtendedMetadata.ExtendedMetadataType;
import com.tencent.polaris.specification.api.v1.service.manage.ServiceProto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ServiceMetadataConverter {

    private ServiceMetadataConverter() {
    }

    static List<ExtendedMetadata> toExtendedMetadata(List<ServiceProto.ExtendedMetadata> metadataList) {
        List<ExtendedMetadata> result = new ArrayList<>();
        for (ServiceProto.ExtendedMetadata metadata : metadataList) {
            result.add(toExtendedMetadata(metadata));
        }
        return Collections.unmodifiableList(result);
    }

    private static ExtendedMetadata toExtendedMetadata(ServiceProto.ExtendedMetadata metadata) {
        ExtendedMetadata.ExtendedMetadataBuilder builder = ExtendedMetadata.builder()
                .type(toExtendedMetadataType(metadata.getType()));
        if (metadata.hasAgentSkill()) {
            builder.agentSkill(toAgentSkill(metadata.getAgentSkill()));
        }
        return builder.build();
    }

    private static ExtendedMetadataType toExtendedMetadataType(
            ServiceProto.ExtendedMetadata.ExtendedMetadataType type) {
        ExtendedMetadataType result = ExtendedMetadataType.UNKNOWN;
        if (type == ServiceProto.ExtendedMetadata.ExtendedMetadataType.EXTENDED_METADATA_SKILL) {
            result = ExtendedMetadataType.SKILL;
        }
        return result;
    }

    private static AgentSkill toAgentSkill(ServiceProto.AgentSkill skill) {
        return AgentSkill.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .tags(Collections.unmodifiableList(new ArrayList<>(skill.getTagsList())))
                .examples(Collections.unmodifiableList(new ArrayList<>(skill.getExamplesList())))
                .inputModes(Collections.unmodifiableList(new ArrayList<>(skill.getInputModesList())))
                .outputModes(Collections.unmodifiableList(new ArrayList<>(skill.getOutputModesList())))
                .version(skill.getVersion())
                .build();
    }
}
