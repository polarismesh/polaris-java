package com.tencent.polaris.api.plugin.server;

/**
 * 客户端上报应答
 *
 * @author vickliu
 * @date 2019/9/22
 */
public class ReportClientResponse {

    enum RunMode {
        ModeNoAgent,        //ModeNoAgent 以no agent模式运行————SDK
        ModeWithAgent        //ModeWithAgent 带agent模式运行
    }

    private RunMode mode;
    private String version;
    private String region;
    private String zone;
    private String campus;

    public RunMode getMode() {
        return mode;
    }

    public void setMode(RunMode mode) {
        this.mode = mode;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getCampus() {
        return campus;
    }

    public void setCampus(String campus) {
        this.campus = campus;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ReportClientResponse{" +
                "mode=" + mode +
                ", version='" + version + '\'' +
                ", region='" + region + '\'' +
                ", zone='" + zone + '\'' +
                ", campus='" + campus + '\'' +
                "}" + super.toString();
    }
}
