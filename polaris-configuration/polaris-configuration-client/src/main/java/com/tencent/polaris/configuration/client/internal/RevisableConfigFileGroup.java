/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupChangedEvent;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.logging.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class RevisableConfigFileGroup extends DefaultConfigFileGroup {
    private static final Logger LOGGER = LoggerFactory.getLogger(RevisableConfigFileGroup.class);

    private String revision;

    public RevisableConfigFileGroup(ConfigFileGroup configFileGroup) {
        this(configFileGroup, "");
    }

    public RevisableConfigFileGroup(ConfigFileGroup cfg, String revision) {
        this(cfg.getNamespace(), cfg.getFileGroupName(), cfg.getConfigFileMetadataList(), revision);
    }

    public RevisableConfigFileGroup(String namespace, String fileGroupName, List<ConfigFileMetadata> configFileMetadataList, String revision) {
        super(namespace, fileGroupName, configFileMetadataList);
        this.revision = revision;
    }

    public String getRevision() {
        return revision;
    }

    public void updateConfigFileList(List<ConfigFileMetadata> newData, String newRevision) {
        String oldRevision = this.revision;
        if (!oldRevision.equals(newRevision)) {
            LOGGER.info("[Config] trigger update event, oldRevision = {}, newRevision = {}", oldRevision, newRevision);

            this.revision = newRevision;
            List<ConfigFileMetadata> oldData = new ArrayList<>(this.configFileMetadataList);
            this.configFileMetadataList = newData;
            super.trigger(new RevisableConfigFileGroupChangedEvent(this, oldData, newData, oldRevision, newRevision));
        }
    }

    public static class RevisableConfigFileGroupChangedEvent extends ConfigFileGroupChangedEvent {
        public String oldRevision;
        public String newRevision;

        public RevisableConfigFileGroupChangedEvent(ConfigFileGroupMetadata metadata, List<ConfigFileMetadata> oldData,
                                                    List<ConfigFileMetadata> newData,
                                                    String oldRevision, String newRevision) {
            super(metadata, oldData, newData);
            this.oldRevision = oldRevision;
            this.newRevision = newRevision;
        }
    }
}
