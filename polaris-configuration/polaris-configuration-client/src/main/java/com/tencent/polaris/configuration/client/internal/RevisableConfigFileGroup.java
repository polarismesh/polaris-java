package com.tencent.polaris.configuration.client.internal;

import com.tencent.polaris.configuration.api.core.ConfigFileGroup;
import com.tencent.polaris.configuration.api.core.ConfigFileMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupMetadata;
import com.tencent.polaris.configuration.api.core.ConfigFileGroupChangedEvent;

import java.util.List;

public class RevisableConfigFileGroup extends DefaultConfigFileGroup {
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
            this.revision = newRevision;
            this.configFileMetadataList = newData;
            super.trigger(new RevisableConfigFileGroupChangedEvent(this, newData, oldRevision, newRevision));
        }
    }

    public static class RevisableConfigFileGroupChangedEvent extends ConfigFileGroupChangedEvent {
        public String oldRevision;
        public String newRevision;

        public RevisableConfigFileGroupChangedEvent(ConfigFileGroupMetadata metadata, List<ConfigFileMetadata> data,
                                                    String oldRevision, String newRevision) {
            super(metadata, data);
            this.oldRevision = oldRevision;
            this.newRevision = newRevision;
        }
    }
}
