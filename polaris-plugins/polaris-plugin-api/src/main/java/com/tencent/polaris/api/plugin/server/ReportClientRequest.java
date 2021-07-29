package com.tencent.polaris.api.plugin.server;

/**
 * 客户端上报请求
 *
 * @author vickliu
 * @date 2019/9/22
 */
public class ReportClientRequest {

    private String namespace;

    private String service;

    private String clientHost;

    private String version;

    public String getClientHost() {
        return clientHost;
    }

    public void setClientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "ReportClientRequest{" +
                "clientHost='" + clientHost + '\'' +
                ", version='" + version + '\'' +
                "}" + super.toString();
    }
}
