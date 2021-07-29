package com.tencent.polaris.ratelimit.client.flow;

import java.util.Objects;

/**
 * 节点的唯一标识
 */
public class HostIdentifier {

    /**
     * host
     */
    private final String host;

    /**
     * port
     */
    private final int port;

    public HostIdentifier(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HostIdentifier that = (HostIdentifier) o;
        return port == that.port &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    @SuppressWarnings("checkstyle:all")
    public String toString() {
        return "HostIdentifier{" +
                "host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
