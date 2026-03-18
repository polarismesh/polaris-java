/*
 * Tencent is pleased to support the open source community by making polaris-java available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.polaris.plugins.event.tsf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import com.tencent.polaris.api.plugin.event.EventConstants;
import com.tencent.polaris.api.plugin.event.EventReporter;
import com.tencent.polaris.api.plugin.event.FlowEvent;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.IPAddressUtils;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.metadata.core.constant.MetadataConstants;
import com.tencent.polaris.metadata.core.manager.CalleeMetadataContainerGroup;
import com.tencent.polaris.plugins.event.tsf.report.CloudEvent;
import com.tencent.polaris.plugins.event.tsf.report.Event;
import com.tencent.polaris.plugins.event.tsf.report.EventResponse;
import com.tencent.polaris.plugins.event.tsf.v1.TsfEventData;
import com.tencent.polaris.plugins.event.tsf.v1.TsfEventDataPair;
import com.tencent.polaris.plugins.event.tsf.v1.TsfEventResponse;
import com.tencent.polaris.plugins.event.tsf.v1.TsfGenericEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tencent.polaris.api.plugin.event.tsf.TsfEventDataConstants.*;

/**
 * @author Haotian Zhang
 */
public class TsfEventReporter implements EventReporter, PluginConfigProvider {

    private static final Logger LOG = LoggerFactory.getLogger(TsfEventReporter.class);

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    /** V1 事件上报业务失败最大重试次数 */
    private static final int V1_MAX_RETRY = 3;

    /** 通用异常最大重试次数 */
    private static final int COMMON_MAX_RETRY = 120;

    /** 通用异常重试间隔（秒） */
    private static final long RETRY_INTERVAL_SECONDS = 60;

    private final LinkedBlockingDeque<TsfEventData> v1EventQueue = new LinkedBlockingDeque<>(QUEUE_THRESHOLD);

    private final Map<String, LinkedBlockingDeque<Event>> reportEventQueueMap = new ConcurrentHashMap<>();

    private volatile boolean init = true;

    private volatile boolean eventUriInit = false;

    private TsfEventReporterConfig tsfEventReporterConfig;

    private URI v1EventUri;

    private URI reportEventUri;

    /** 重试等待时间（毫秒），默认 60s，测试时可通过构造函数注入较小值 */
    private final long retryIntervalMs;

    private final ScheduledExecutorService v1EventExecutors = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("event-tsf-v1"));

    protected ScheduledExecutorService reportEventExecutors = Executors.newScheduledThreadPool(1,
            new NamedThreadFactory("event-tsf-report"));

    /**
     * 独立重试调度器，用于网络异常后延迟恢复消费。
     * 网络异常时设置 paused 标志，schedule 60s 后恢复，避免为每个事件单独重试导致任务堆积。
     */
    private final ScheduledExecutorService retryExecutors = new ScheduledThreadPoolExecutor(1,
            new NamedThreadFactory("event-tsf-retry", true));

    /** 通用异常重试计数（v1 和 report 共用） */
    private final AtomicInteger commonRetryCount = new AtomicInteger(0);

    /** 是否因网络异常暂停消费队列 */
    private volatile boolean paused = false;

    public TsfEventReporter() {
        this.retryIntervalMs = RETRY_INTERVAL_SECONDS * 1000;
    }

    /** 测试专用构造函数，允许注入较小的重试等待时间 */
    TsfEventReporter(long retryIntervalMs) {
        this.retryIntervalMs = retryIntervalMs;
    }

    @Override
    public boolean isEnabled() {
        return tsfEventReporterConfig.isEnable();
    }

    @Override
    public boolean reportEvent(BaseEvent baseEvent) {
        if (baseEvent instanceof FlowEvent) {
            if (baseEvent.getEventType().equals(EventConstants.EventType.CIRCUIT_BREAKING)) {
                return reportV1Event((FlowEvent) baseEvent);
            } else if (baseEvent.getEventType().equals(EventConstants.EventType.RATE_LIMITING)) {
                return reportReportEvent((FlowEvent) baseEvent);
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("event {} is not supported for reporting, return true.", baseEvent);
        }
        return true;
    }

    private boolean reportV1Event(FlowEvent flowEvent) {
        try {
            initEventUri();
            if (v1EventUri == null) {
                LOG.warn("build v1 event request url fail, can not sent event.");
                return false;
            }

            TsfEventData eventData = new TsfEventData();
            eventData.setOccurTime(flowEvent.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond());
            eventData.setEventName(TsfEventDataUtils.convertEventName(flowEvent));
            Byte status = TsfEventDataUtils.convertStatus(flowEvent);
            if (status == null || status == -1) {
                return true;
            }
            eventData.setStatus(status);

            List<TsfEventDataPair> dimensions = new ArrayList<>();
            dimensions.add(new TsfEventDataPair(APP_ID_KEY, tsfEventReporterConfig.getAppId()));
            dimensions.add(new TsfEventDataPair(NAMESPACE_ID_KEY, tsfEventReporterConfig.getTsfNamespaceId()));
            dimensions.add(new TsfEventDataPair(SERVICE_NAME, getLocalServiceName()));
            eventData.setDimensions(dimensions);

            List<TsfEventDataPair> additionalMsg = new ArrayList<>();
            additionalMsg.add(new TsfEventDataPair(UPSTREAM_SERVICE_KEY, getLocalServiceName()));
            additionalMsg.add(new TsfEventDataPair(UPSTREAM_NAMESPACE_ID_KEY, tsfEventReporterConfig.getTsfNamespaceId()));
            additionalMsg.add(new TsfEventDataPair(DOWNSTREAM_SERVICE_KEY, flowEvent.getService()));
            additionalMsg.add(new TsfEventDataPair(DOWNSTREAM_NAMESPACE_ID_KEY, flowEvent.getNamespace()));
            String isolationObject = "";
            for (Map.Entry<String, String> entry : flowEvent.getAdditionalParams().entrySet()) {
                additionalMsg.add(new TsfEventDataPair(entry.getKey(), entry.getValue()));
                if (StringUtils.equals(entry.getKey(), ISOLATION_OBJECT_KEY)) {
                    isolationObject = entry.getValue();
                }
            }
            eventData.setAdditionalMsg(additionalMsg);

            if (StringUtils.isNotBlank(isolationObject)) {
                eventData.setInstanceId(tsfEventReporterConfig.getInstanceId() + "#" + isolationObject);
            } else {
                eventData.setInstanceId(tsfEventReporterConfig.getInstanceId());
            }

            // 如果满了就抛出异常
            try {
                v1EventQueue.add(eventData);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("add v1 event to v1EventQueue: {}", eventData);
                }
            } catch (Throwable throwable) {
                LOG.warn("v1EventQueue is full. Log this event and drop it. event={}, error={}.",
                        flowEvent, throwable.getMessage(), throwable);
            }
            return true;
        } catch (Throwable throwable) {
            LOG.warn("failed to add v1 event. {}", flowEvent, throwable);
            return false;
        }
    }

    private boolean reportReportEvent(FlowEvent flowEvent) {
        try {
            if (reportEventUri == null) {
                LOG.warn("build event report request url fail, can not sent event.");
                return false;
            }

            CloudEvent cloudEvent = new CloudEvent();
            cloudEvent.setEvent(TsfEventDataUtils.convertEventName(flowEvent));
            cloudEvent.setAppId(tsfEventReporterConfig.getAppId());
            cloudEvent.setRegion(tsfEventReporterConfig.getRegion());
            Byte status = TsfEventDataUtils.convertStatus(flowEvent);
            if (status == null || status == -1) {
                return true;
            }
            cloudEvent.setStatus(status);

            cloudEvent.putDimension(APP_ID_KEY, tsfEventReporterConfig.getAppId());
            cloudEvent.putDimension(NAMESPACE_ID_KEY, tsfEventReporterConfig.getTsfNamespaceId());
            cloudEvent.putDimension(SERVICE_NAME, getLocalServiceName());
            cloudEvent.putDimension(APPLICATION_ID, tsfEventReporterConfig.getApplicationId());

            cloudEvent.putExtensionMsg(UPSTREAM_SERVICE_KEY, flowEvent.getSourceService());
            cloudEvent.putExtensionMsg(UPSTREAM_NAMESPACE_ID_KEY, flowEvent.getSourceNamespace());
            String ruleId = flowEvent.getRuleName();
            for (Map.Entry<String, String> entry : flowEvent.getAdditionalParams().entrySet()) {
                cloudEvent.putExtensionMsg(entry.getKey(), entry.getValue());
                if (StringUtils.equals(entry.getKey(), RULE_ID_KEY)) {
                    ruleId = entry.getValue();
                }
            }

            String uniqueId = tsfEventReporterConfig.getInstanceId()
                    + "#" + tsfEventReporterConfig.getTsfNamespaceId()
                    + "#" + getLocalServiceName() + "#" + ruleId;
            cloudEvent.setId(uniqueId);
            cloudEvent.setObject(uniqueId);

            // 如果满了就抛出异常
            try {
                LinkedBlockingDeque<Event> reportEventQueue = reportEventQueueMap.computeIfAbsent(cloudEvent.getEvent(), k -> new LinkedBlockingDeque<>(QUEUE_THRESHOLD));
                reportEventQueue.add(cloudEvent);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("add report event to reportEventQueue: {}", cloudEvent);
                }
            } catch (Throwable throwable) {
                LOG.warn("reportEventQueue {} is full. Log this event and drop it. event={}, error={}.",
                        cloudEvent.getEvent(), cloudEvent, throwable.getMessage(), throwable);
            }
            return true;
        } catch (Throwable throwable) {
            LOG.warn("failed to add report event. {}", flowEvent, throwable);
            return false;
        }
    }

    @Override
    public String getName() {
        return DefaultPlugins.TSF_EVENT_REPORTER_TYPE;
    }

    @Override
    public Class<? extends Verifier> getPluginConfigClazz() {
        return TsfEventReporterConfig.class;
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
                    this.tsfEventReporterConfig = ctx.getConfig().getGlobal().getEventReporter()
                            .getPluginConfig(getName(), TsfEventReporterConfig.class);
                    if (tsfEventReporterConfig.isEnable()) {
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
                        v1EventExecutors.scheduleWithFixedDelay(new TsfV1EventTask(), 1000, 1000, TimeUnit.MILLISECONDS);

                        this.reportEventUri = new URIBuilder()
                                .setScheme("http")
                                .setHost(IPAddressUtils.getIpCompatible(tsfEventReporterConfig.getEventMasterIp()))
                                .setPort(tsfEventReporterConfig.getEventMasterPort())
                                .setPath("/event/report")
                                .setParameter("token", tsfEventReporterConfig.getToken())
                                .build();
                        reportEventExecutors.scheduleWithFixedDelay(new TsfReportEventTask(), 1000, 1000, TimeUnit.MILLISECONDS);
                        LOG.info("Tsf report event reporter init with uri: {}", reportEventUri);
                        LOG.info("Tsf event reporter starts reporting task.");
                    } catch (URISyntaxException e) {
                        LOG.error("Build event request url fail.", e);
                    }
                }
            }
        }
    }

    private String getLocalServiceName() {
        return CalleeMetadataContainerGroup.getStaticApplicationMetadataContainer().getRawMetadataStringValue(MetadataConstants.LOCAL_SERVICE);
    }

    /**
     * delay init event uri, as service name may not be ready at bootstrap stage.
     */
    private void initEventUri() {
        if (!eventUriInit) {
            synchronized (this) {
                if (!eventUriInit) {
                    try {
                        String v1Path = String.format("/v1/event/%s/%s",
                                URLEncoder.encode(getLocalServiceName(), "UTF-8"),
                                URLEncoder.encode(tsfEventReporterConfig.getInstanceId(), "UTF-8"));
                        v1EventUri = new URIBuilder()
                                .setScheme("http")
                                .setHost(IPAddressUtils.getIpCompatible(tsfEventReporterConfig.getEventMasterIp()))
                                .setPort(tsfEventReporterConfig.getEventMasterPort())
                                .setPath(v1Path)
                                .setParameter("token", tsfEventReporterConfig.getToken())
                                .build();
                        LOG.info("Tsf v1 event reporter init with uri: {}", v1EventUri);
                    } catch (URISyntaxException | UnsupportedEncodingException e) {
                        LOG.error("Build event request url fail.", e);
                    }
                    eventUriInit = true;
                }
            }
        }
    }

    /**
     * 网络异常时触发暂停：设置 paused=true，schedule retryIntervalMs 后恢复消费。
     * 所有事件保留在队列中，恢复后由定时任务统一重新消费，避免为每个事件单独重试导致任务堆积。
     */
    private void handleCommonException(Throwable e) {
        // 幂等保护：已经处于暂停状态则不重复调度 resume 任务
        if (paused) {
            return;
        }
        int count = commonRetryCount.incrementAndGet();
        if (count > COMMON_MAX_RETRY) {
            LOG.warn("Tsf event reporter failed after {} retries, giving up. Events in queue will be dropped.",
                    COMMON_MAX_RETRY, e);
            v1EventQueue.clear();
            reportEventQueueMap.values().forEach(LinkedBlockingDeque::clear);
            commonRetryCount.set(0);
            paused = false;
            return;
        }
        LOG.warn("Tsf event reporter task fail (retry {}/{}), pausing {}ms before next attempt.",
                count, COMMON_MAX_RETRY, retryIntervalMs, e);
        paused = true;
        retryExecutors.schedule(() -> {
            LOG.debug("Tsf event reporter resuming after pause (retry {}/{}).", commonRetryCount.get(), COMMON_MAX_RETRY);
            paused = false;
        }, retryIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void postV1Event(TsfGenericEvent genericEvent) throws Exception {
        StringEntity postBody = null;
        RequestConfig config = RequestConfig.custom().setConnectTimeout(2000).setConnectionRequestTimeout(10000).setSocketTimeout(10000).build();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
            initEventUri();
            HttpPost httpPost = null;
            HttpResponse httpResponse;

            // 基于 retCode 的重试逻辑，最多重试 V1_MAX_RETRY 次
            final String bodyJson = gson.toJson(genericEvent);
            for (int attempt = 0; attempt <= V1_MAX_RETRY; attempt++) {
                // 每次重试都重新创建 HttpPost 和 StringEntity，避免 entity 已被消费无法重复读
                httpPost = new HttpPost(v1EventUri);
                postBody = new StringEntity(bodyJson);
                httpPost.setEntity(postBody);
                httpPost.setHeader("Content-Type", "application/json");

                httpResponse = httpClient.execute(httpPost);
                String resultString = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
                TsfEventResponse response = gson.fromJson(resultString, TsfEventResponse.class);

                if (response.getRetCode() != 0) {
                    if (attempt < V1_MAX_RETRY) {
                        LOG.warn("Report v1 event failed (attempt {}/{}), retrying. Response = [{}].",
                                attempt + 1, V1_MAX_RETRY, resultString);
                        continue;
                    } else {
                        LOG.warn("Report v1 event failed after {} retries, giving up. Response = [{}].",
                                V1_MAX_RETRY, resultString);
                        return;
                    }
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("postV1Event body:{}", bodyJson);
                }
                LOG.info("Report v1 event To TSF event-center Success. Response is : {}", resultString);
                return;
            }
        }
    }

    private void postReportEvent(List<Event> eventData) throws Exception {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(2000).setConnectionRequestTimeout(10000).setSocketTimeout(10000).build();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {

            HttpPost httpPost = new HttpPost(reportEventUri);
            String body = gson.toJson(eventData);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Report report Event To TSF event-center. body is : {}", body);
            }
            httpPost.setEntity(new StringEntity(body));
            httpPost.setHeader("Content-Type", "application/json");

            HttpResponse httpResponse = httpClient.execute(httpPost);
            String resultString = EntityUtils.toString(httpResponse.getEntity(), "utf-8");
            EventResponse response = gson.fromJson(resultString, EventResponse.class);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Report report Event To TSF event-center. Response is : {}", resultString);
            }
            // errorInfo 非空时直接放弃，不重试
            if (StringUtils.isNotBlank(response.getErrorInfo())) {
                LOG.warn("Report report event failed, giving up (no retry). Response = [{}].", resultString);
                return;
            }
            LOG.info("Report report Event To TSF event-center Success. Response is : {}", resultString);
        }
    }

    @Override
    public void destroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{v1EventExecutors, reportEventExecutors, retryExecutors});
    }

    class TsfV1EventTask implements Runnable {

        @Override
        public void run() {
            // 暂停期间跳过，事件保留在队列中等待恢复
            if (paused) {
                return;
            }
            try {
                // 每次把eventQueue发空结束
                while (!v1EventQueue.isEmpty()) {
                    List<TsfEventData> eventDataList = new ArrayList<>();
                    TsfGenericEvent genericEvent = new TsfGenericEvent();

                    v1EventQueue.drainTo(eventDataList, MAX_BATCH_SIZE);
                    genericEvent.setEventData(eventDataList);
                    genericEvent.setAppId(tsfEventReporterConfig.getAppId());
                    genericEvent.setRegion(tsfEventReporterConfig.getRegion());

                    try {
                        postV1Event(genericEvent);
                        // 成功后重置重试计数
                        commonRetryCount.set(0);
                    } catch (Throwable e) {
                        // 网络异常：将已取出的事件逆序放回队列头部，保持原有顺序，避免丢失
                        // 一旦队列满导致 offerFirst 失败，立即停止回填并记录丢弃数量（继续尝试也必然失败，且会破坏顺序）
                        for (int i = eventDataList.size() - 1; i >= 0; i--) {
                            if (!v1EventQueue.offerFirst(eventDataList.get(i))) {
                                LOG.warn("v1EventQueue is full, drop {} events on re-enqueue (index 0~{} dropped).",
                                        i + 1, i);
                                break;
                            }
                        }
                        throw e;
                    }
                }
            } catch (Throwable e) {
                handleCommonException(e);
            }
        }
    }

    class TsfReportEventTask implements Runnable {

        @Override
        public void run() {
            // 暂停期间跳过，事件保留在队列中等待恢复
            if (paused) {
                return;
            }
            try {
                for (Map.Entry<String, LinkedBlockingDeque<Event>> entry : reportEventQueueMap.entrySet()) {
                    LinkedBlockingDeque<Event> queue = entry.getValue();
                    while (!queue.isEmpty()) {
                        List<Event> eventDataList = new ArrayList<>();
                        queue.drainTo(eventDataList, MAX_BATCH_SIZE);
                        try {
                            postReportEvent(eventDataList);
                            // 成功后重置重试计数
                            commonRetryCount.set(0);
                        } catch (Throwable e) {
                            // 网络异常：将已取出的事件逆序放回队列头部，保持原有顺序，避免丢失
                            // 一旦队列满导致 offerFirst 失败，立即停止回填并记录丢弃数量（继续尝试也必然失败，且会破坏顺序）
                            for (int i = eventDataList.size() - 1; i >= 0; i--) {
                                if (!queue.offerFirst(eventDataList.get(i))) {
                                    LOG.warn("reportEventQueue {} is full, drop {} events on re-enqueue (index 0~{} dropped).",
                                            entry.getKey(), i + 1, i);
                                    break;
                                }
                            }
                            throw e;
                        }
                    }
                }
            } catch (Throwable e) {
                handleCommonException(e);
            }
        }
    }
}


