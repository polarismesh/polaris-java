/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.polaris.plugins.event.pushgateway;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tencent.polaris.api.config.global.EventReporterConfig;
import com.tencent.polaris.api.config.plugin.DefaultPlugins;
import com.tencent.polaris.api.config.plugin.PluginConfigProvider;
import com.tencent.polaris.api.config.verify.Verifier;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.PluginType;
import com.tencent.polaris.api.plugin.common.InitContext;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.event.BaseEvent;
import com.tencent.polaris.api.plugin.event.EventReporter;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.pojo.Node;
import com.tencent.polaris.client.remote.ServiceAddressRepository;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Polaris push gateway event reporter.
 *
 * @author Haotian Zhang
 */
public class PushGatewayEventReporter implements EventReporter, PluginConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PushGatewayEventReporter.class);

    private final ObjectMapper mapper = new ObjectMapper();

    private BlockingQueue<BaseEvent> eventQueue;

    private volatile boolean init = true;

    private PushGatewayEventReporterConfig config;

    private ServiceAddressRepository serviceAddressRepository;

    private final Map<Node, URI> eventUriMap = new ConcurrentHashMap<>();

    private final ScheduledExecutorService eventExecutors = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("event-pushgateway"));

    @Override
    public boolean isEnabled() {
        return config.isEnable();
    }

    @Override
    public boolean reportEvent(BaseEvent flowEvent) {
        if (serviceAddressRepository == null) {
            LOG.warn("build event request url fail, can not sent event.");
            return false;
        }
        // 如果满了就抛出异常
        try {
            eventQueue.add(flowEvent);
            if (LOG.isDebugEnabled()) {
                LOG.debug("add push gateway event to event queue: {}", flowEvent);
            }
        } catch (Throwable throwable) {
            LOG.warn("Event queue is full. Log this event and drop it. event={}, error={}.",
                    flowEvent, throwable.getMessage(), throwable);
        }
        return true;
    }

    @Override
    public String getName() {
        return DefaultPlugins.PUSH_GATEWAY_EVENT_REPORTER_TYPE;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return PushGatewayEventReporterConfig.class;
    }

    @Override
    public PluginType getType() {
        return PluginTypes.EVENT_REPORTER.getBaseType();
    }

    @Override
    public void init(InitContext ctx) throws PolarisException {
        EventReporterConfig eventReporterConfig = ctx.getConfig().getGlobal().getEventReporter();
        if (eventReporterConfig != null && CollectionUtils.isNotEmpty(eventReporterConfig.getReporters())) {
            for (String reporter : eventReporterConfig.getReporters()) {
                if (StringUtils.equals(getName(), reporter)) {
                    this.config = ctx.getConfig().getGlobal().getEventReporter()
                            .getPluginConfig(getName(), PushGatewayEventReporterConfig.class);
                    if (config.isEnable()) {
                        init = false;
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void postContextInit(Extensions ctx) throws PolarisException {
        if (!init) {
            synchronized (this) {
                if (!init) {
                    init = true;
                    try {
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        mapper.registerModule(new JavaTimeModule());
                        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

                        eventQueue = new LinkedBlockingQueue<>(config.getEventQueueSize());

                        serviceAddressRepository = new ServiceAddressRepository(Collections.singletonList(this.config.getAddress()),
                                ctx.getValueContext().getClientId(), ctx, new ServiceKey(config.getNamespace(), config.getService()));

                        eventExecutors.scheduleWithFixedDelay(new PushGatewayEventTask(), 1000, 1000, TimeUnit.MILLISECONDS);
                        LOG.info("PushGateway event reporter starts reporting task.");
                    } catch (Throwable e) {
                        LOG.error("Init PushGateway event reporter fail.", e);
                    }
                }
            }
        }
    }

    private URI getEventUri() {
        return getEventUriByNode(serviceAddressRepository.getServiceAddressNode());
    }

    private URI getEventUriByNode(Node node) {
        if (node != null) {
            // First try to get URI from cache.
            URI cachedUri = eventUriMap.get(node);
            if (cachedUri != null) {
                return cachedUri;
            }

            // If not in cache, build new URI.
            try {
                URI uri = new URIBuilder()
                        .setHost(node.getHost())
                        .setPort(node.getPort())
                        .setScheme("http")
                        .setPath("/polaris/client/events")
                        .build();
                URI existingUri = eventUriMap.putIfAbsent(node, uri);
                return existingUri != null ? existingUri : uri;
            } catch (URISyntaxException e) {
                LOG.error("Build event request url with node {} fail.", node, e);
            }
        }
        return null;
    }

    @Override
    public void destroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{eventExecutors});
    }

    class PushGatewayEventTask implements Runnable {

        @Override
        public void run() {
            try {
                // 每次把eventQueue发空结束
                while (CollectionUtils.isNotEmpty(eventQueue)) {
                    List<BaseEvent> eventDataList = new ArrayList<>();
                    PushGatewayEventRequest request = new PushGatewayEventRequest();

                    eventQueue.drainTo(eventDataList, config.getMaxBatchSize());
                    request.setBatch(eventDataList);

                    postPushGatewayEvent(request);
                }
            } catch (Throwable e) {
                LOG.warn("Push gateway event reporter task fail.", e);
            }
        }

        private void postPushGatewayEvent(PushGatewayEventRequest request) {
            StringEntity postBody = null;
            RequestConfig config = RequestConfig.custom().setConnectTimeout(2000).setConnectionRequestTimeout(10000).setSocketTimeout(10000).build();
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
                HttpPost httpPost = new HttpPost(getEventUri());
                postBody = new StringEntity(mapper.writeValueAsString(request));
                if (LOG.isDebugEnabled()) {
                    LOG.debug("postPushGatewayEvent body:{}", postBody);
                }
                httpPost.setEntity(postBody);
                httpPost.setHeader("Content-Type", "application/json");
                HttpResponse httpResponse;

                httpResponse = httpClient.execute(httpPost);

                if (200 != httpResponse.getStatusLine().getStatusCode()) {
                    String resultString = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
                    throw new RuntimeException("Report push gateway event failed. Response = [" + resultString + "].");
                } else {
                    if (LOG.isDebugEnabled()) {
                        String resultString = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
                        LOG.info("Report push gateway event success. Response is : {}", resultString);
                    } else {
                        LOG.info("Report push gateway event success.");
                    }
                }
            } catch (Exception e) {
                LOG.warn("Report push gateway event failed, postBody:{}.", postBody, e);
            }
        }
    }
}
