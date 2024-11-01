package com.tencent.polaris.plugins.event.tsf;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.factory.util.ConfigUtils;

/**
 * @author Haotian Zhang
 */
public class TsfEventReporterConfig implements Verifier {

    @JsonProperty
    private String eventMasterIp;

    @JsonProperty
    private Integer eventMasterPort;

    @JsonProperty
    private String appId;

    @JsonProperty
    private String region;

    @JsonProperty
    private String instanceId;

    @JsonProperty
    private String tsfNamespaceId;

    @JsonProperty
    private String serviceName;

    @JsonProperty
    private String token;

    @JsonProperty
    private String applicationId;

    @Override
    public void verify() {
        ConfigUtils.validateString(eventMasterIp, "global.eventReporter.plugins.tsf.eventMasterIp");
        ConfigUtils.validatePositiveInteger(eventMasterPort, "global.eventReporter.plugins.tsf.eventMasterPort");
        ConfigUtils.validateString(appId, "global.eventReporter.plugins.tsf.appId");
        ConfigUtils.validateString(region, "global.eventReporter.plugins.tsf.region");
        ConfigUtils.validateString(instanceId, "global.eventReporter.plugins.tsf.instanceId");
        ConfigUtils.validateString(tsfNamespaceId, "global.eventReporter.plugins.tsf.tsfNamespaceId");
        ConfigUtils.validateString(serviceName, "global.eventReporter.plugins.tsf.serviceName");
        ConfigUtils.validateString(token, "global.eventReporter.plugins.tsf.token");
        ConfigUtils.validateString(applicationId, "global.eventReporter.plugins.tsf.applicationId");
    }

    @Override
    public void setDefault(Object defaultObject) {
        if (defaultObject instanceof TsfEventReporterConfig) {
            TsfEventReporterConfig tsfEventReporterConfig = (TsfEventReporterConfig) defaultObject;
            if (StringUtils.isBlank(eventMasterIp)) {
                setEventMasterIp(tsfEventReporterConfig.getEventMasterIp());
            }
            if (eventMasterPort == null || 0 == eventMasterPort) {
                setEventMasterPort(tsfEventReporterConfig.getEventMasterPort());
            }
            if (StringUtils.isBlank(appId)) {
                setAppId(tsfEventReporterConfig.getAppId());
            }
            if (StringUtils.isBlank(region)) {
                setRegion(tsfEventReporterConfig.getRegion());
            }
            if (StringUtils.isBlank(instanceId)) {
                setInstanceId(tsfEventReporterConfig.getInstanceId());
            }
            if (StringUtils.isBlank(tsfNamespaceId)) {
                setTsfNamespaceId(tsfEventReporterConfig.getTsfNamespaceId());
            }
            if (StringUtils.isBlank(serviceName)) {
                setServiceName(tsfEventReporterConfig.getServiceName());
            }
            if (StringUtils.isBlank(token)) {
                setToken(tsfEventReporterConfig.getToken());
            }
            if (StringUtils.isBlank(applicationId)) {
                setApplicationId(tsfEventReporterConfig.getApplicationId());
            }
        }
    }

    public String getEventMasterIp() {
        return eventMasterIp;
    }

    public void setEventMasterIp(String eventMasterIp) {
        this.eventMasterIp = eventMasterIp;
    }

    public Integer getEventMasterPort() {
        return eventMasterPort;
    }

    public void setEventMasterPort(Integer eventMasterPort) {
        this.eventMasterPort = eventMasterPort;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getTsfNamespaceId() {
        return tsfNamespaceId;
    }

    public void setTsfNamespaceId(String tsfNamespaceId) {
        this.tsfNamespaceId = tsfNamespaceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
}
