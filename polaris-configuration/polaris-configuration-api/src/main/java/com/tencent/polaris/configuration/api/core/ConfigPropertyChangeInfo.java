package com.tencent.polaris.configuration.api.core;

/**
 * @author lepdou 2022-03-01
 */
public class ConfigPropertyChangeInfo {

    private final String     propertyName;
    private final String     oldValue;
    private final String     newValue;
    private final ChangeType changeType;

    public ConfigPropertyChangeInfo(String propertyName,
                                    String oldValue, String newValue,
                                    ChangeType changeType) {
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changeType = changeType;
    }

    public String getPropertyName() {
        return propertyName;
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
        return "ConfigPropertyChangeInfo{" +
               ", propertyName='" + propertyName + '\'' +
               ", oldValue='" + oldValue + '\'' +
               ", newValue='" + newValue + '\'' +
               ", changeType=" + changeType +
               '}';
    }
}
