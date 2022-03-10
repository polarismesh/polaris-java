package com.tencent.polaris.api.plugin.configuration;

/**
 * @author lepdou 2022-03-02
 */
public class ConfigFileResponse {

    private int        code;
    private String     message;
    private ConfigFile configFile;

    public ConfigFileResponse(int code, String message, ConfigFile configFile) {
        this.code = code;
        this.message = message;
        this.configFile = configFile;
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

    public ConfigFile getConfigFile() {
        return configFile;
    }

    public void setConfigFile(ConfigFile configFile) {
        this.configFile = configFile;
    }

    @Override
    public String toString() {
        return "ConfigFileResponse{" +
               "code=" + code +
               ", message='" + message + '\'' +
               ", configFile=" + configFile +
               '}';
    }
}
