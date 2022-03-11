package com.tencent.polaris.configuration.api.core;

import java.util.Map;
import java.util.Set;

/**
 * @author lepdou 2022-03-01
 */
public class ConfigKVFileChangeEvent {

    private final Map<String, ConfigPropertyChangeInfo> propertyChangeInfos;

    public ConfigKVFileChangeEvent(Map<String, ConfigPropertyChangeInfo> changeInfos) {
        this.propertyChangeInfos = changeInfos;
    }

    public Set<String> changedKeys() {
        return propertyChangeInfos.keySet();
    }

    public ConfigPropertyChangeInfo getChangeInfo(String propertyKey) {
        return propertyChangeInfos.get(propertyKey);
    }

    public boolean isChanged(String propertyKey) {
        return propertyChangeInfos.containsKey(propertyKey);
    }

    public String getPropertyOldValue(String propertyKey) {
        ConfigPropertyChangeInfo changeInfo = propertyChangeInfos.get(propertyKey);
        if (changeInfo != null) {
            return changeInfo.getOldValue();
        }
        return null;
    }

    public String getPropertyNewValue(String propertyKey) {
        ConfigPropertyChangeInfo changeInfo = propertyChangeInfos.get(propertyKey);
        if (changeInfo != null) {
            return changeInfo.getNewValue();
        }
        return null;
    }

    public ChangeType getPropertiesChangeType(String propertyKey) {
        ConfigPropertyChangeInfo changeInfo = propertyChangeInfos.get(propertyKey);
        if (changeInfo != null) {
            return changeInfo.getChangeType();
        }
        return ChangeType.NOT_CHANGED;
    }

}
