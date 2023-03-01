/*
 * Tencent is pleased to support the open source community by making Polaris available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.polaris.plugins.stat.prometheus.handler;

import com.sun.net.httpserver.HttpServer;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.HTTPServer;
import org.slf4j.Logger;

/**
 * Prometheus HTTP Server for pulling
 *
 * @author wallezhang
 */
public class PrometheusHttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusHttpServer.class);
    private static final String DEFAULT_PATH = "/metrics";
    /**
     * The minimal random port
     */
    private static final int MIN_RANDOM_PORT = 20000;
    /**
     * The maximum random port
     */
    private static final int MAX_RANDOM_PORT = 65535;
    private final HttpServer httpServer;
    private final ThreadFactory threadFactory;
    private final ExecutorService executor;
    private final String host;
    private int port;
    private String path;

    public PrometheusHttpServer(String host, int port, CollectorRegistry registry) {
        this(host, port, DEFAULT_PATH, registry);
    }

    public PrometheusHttpServer(String host, int port, String path, CollectorRegistry registry) {
        try {
            this.host = host;
            this.path = path;
            setServerPort(port);
            threadFactory = new DaemonNamedThreadFactory("prometheus-http");
            httpServer = HttpServer.create(new InetSocketAddress(this.host, this.port), 3);
            HttpMetricHandler metricHandler = new HttpMetricHandler(registry);
            httpServer.createContext("/", metricHandler);
            httpServer.createContext(path, metricHandler);
            httpServer.createContext("/-/healthy", metricHandler);
            executor = Executors.newFixedThreadPool(3, threadFactory);
            httpServer.setExecutor(executor);
            startServer();
        } catch (IOException e) {
            LOGGER.error("Create prometheus http server exception. host:{}, port:{}, path:{}", host, port, path, e);
            throw new UncheckedIOException("Create prometheus http server failed!", e);
        }
    }

    private void setServerPort(int port) {
        if (port == 0) {
            // Select a random port
            int randomPort = ThreadLocalRandom.current().nextInt(MIN_RANDOM_PORT, MAX_RANDOM_PORT);
            while (!isPortAvailable(randomPort)) {
                randomPort = ThreadLocalRandom.current().nextInt(MIN_RANDOM_PORT, MAX_RANDOM_PORT);
            }
            this.port = randomPort;
            return;
        }
        if (port == PrometheusHandlerConfig.DEFAULT_MIN_PULL_PORT) {
            for (int i = PrometheusHandlerConfig.DEFAULT_MIN_PULL_PORT; i <= PrometheusHandlerConfig.DEFAULT_MAX_PULL_PORT; i ++) {
                if (isPortAvailable(i)) {
                    this.port = i;
                    return;
                }
            }
            String errMsg = String.format("prometheus http server port from %s to %d is used, " +
                    "please modify ${global.statReporter.plugin.prometheus.port} in polaris.yml",
                    PrometheusHandlerConfig.DEFAULT_MIN_PULL_PORT, PrometheusHandlerConfig.DEFAULT_MAX_PULL_PORT);

            throw new RuntimeException(errMsg);
        }
        this.port = port;
    }

    private boolean isPortAvailable(int port) {
        try {
            bindPort(this.host, port);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    private void bindPort(String host, int port) throws IOException {
        try (Socket socket = new Socket()) {
            socket.bind(new InetSocketAddress(host, port));
        }
    }

    /**
     * Start prometheus http server in a daemon thread
     */
    private void startServer() {
        if (Thread.currentThread().isDaemon()) {
            httpServer.start();
            return;
        }

        Thread httpServerThread = threadFactory.newThread(httpServer::start);
        httpServerThread.start();
        try {
            httpServerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stop the prometheus http server and executor
     */
    public void stopServer() {
        httpServer.stop(0);
        executor.shutdownNow();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    private static final class DaemonNamedThreadFactory extends NamedThreadFactory {

        public DaemonNamedThreadFactory(String component) {
            super(component);
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = super.newThread(r);
            thread.setDaemon(true);
            return thread;
        }
    }
}
