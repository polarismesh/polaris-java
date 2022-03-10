package com.tencent.polaris.configuration.api.core;


/**
 * @author lepdou 2022-03-01
 */
public class ConfigFileChangeEvent {

    private final ConfigFileMetadata configFileMetadata;
    private final String             oldValue;
    private final String             newValue;
    private final ChangeType         changeType;


    public ConfigFileChangeEvent(ConfigFileMetadata configFileMetadata,
                                 String oldValue, String newValue, ChangeType changeType) {
        this.configFileMetadata = configFileMetadata;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    @Override
    public String toString() {
        return "ConfigFileChangeEvent{" +
               "configFileMetadata=" + configFileMetadata +
               ", oldValue='" + oldValue + '\'' +
               ", newValue='" + newValue + '\'' +
               ", changeType=" + changeType +
               '}';
    }
}
