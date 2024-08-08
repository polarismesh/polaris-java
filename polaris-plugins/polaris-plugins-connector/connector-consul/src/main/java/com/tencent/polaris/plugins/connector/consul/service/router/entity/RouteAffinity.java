package com.tencent.polaris.plugins.connector.consul.service.router.entity;

import java.io.Serializable;

/**
 * 路由就近访问策略
 */
public class RouteAffinity implements Serializable {

    private static final long serialVersionUID = -1879939014196760924L;

    private String namespaceId;

    private Boolean affinity;

    public String getNamespaceId() {
        return namespaceId;
    }

    public void setNamespaceId(String namespaceId) {
        this.namespaceId = namespaceId;
    }

    public Boolean getAffinity() {
        return affinity;
    }

    public void setAffinity(Boolean affinity) {
        this.affinity = affinity;
    }

    @Override
    public String toString() {
        return "RouteAffinity{" +
                "namespaceId='" + namespaceId + '\'' +
                ", affinity=" + affinity +
                '}';
    }
}
