package com.tencent.polaris.api.plugin.configuration;

public class ConfigFileGroupResponse {
    private int code;
    private String message;
    private String revision;
    private ConfigFileGroup configFileGroup;

    public ConfigFileGroupResponse(int code, String message, String revision, ConfigFileGroup configFileGroup) {
        this.code = code;
        this.message = message;
        this.revision = revision;
        this.configFileGroup = configFileGroup;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public ConfigFileGroup getConfigFileGroup() {
        return configFileGroup;
    }

    public void setConfigFileGroup(ConfigFileGroup configFileGroup) {
        this.configFileGroup = configFileGroup;
    }
}
