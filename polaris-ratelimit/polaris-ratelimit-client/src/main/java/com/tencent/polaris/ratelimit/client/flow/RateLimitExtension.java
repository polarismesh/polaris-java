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

package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.annonation.JustForTest;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.client.sync.RemoteSyncTask;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

import static com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter.*;

public class RateLimitExtension extends Destroyable {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitExtension.class);

    private final Extensions extensions;

    private final Map<String, ServiceRateLimiter> rateLimiters = new HashMap<>();

    private ServiceRateLimiter defaultRateLimiter;

    private final ScheduledExecutorService syncExecutor;

    private final ScheduledExecutorService windowExpireExecutor;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * cleanupContainers 的 expire job future，用于重复注册时取消旧的；
     * volatile 保证未来若加锁外读（如 cancelExpireJob）的可见性
     */
    private volatile ScheduledFuture<?> expireFuture;

    /**
     * 构造器
     *
     * @param extensions extensions
     */
    public RateLimitExtension(Extensions extensions) {
        this.extensions = extensions;
        Collection<Plugin> plugins = extensions.getPlugins().getPlugins(PluginTypes.SERVICE_LIMITER.getBaseType());
        for (Plugin plugin : plugins) {
            if (plugin.getName().equals(LIMITER_REJECT)) {
                defaultRateLimiter = (ServiceRateLimiter) plugin;
            }
            rateLimiters.put(plugin.getName(), (ServiceRateLimiter) plugin);
        }
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1,
                new NamedThreadFactory("rateLimit-sync"));
        executor.setMaximumPoolSize(1);
        ScheduledThreadPoolExecutor expireExecutor = new ScheduledThreadPoolExecutor(1,
                new NamedThreadFactory("rateLimit-expire"));
        expireExecutor.setMaximumPoolSize(1);
        syncExecutor = executor;
        windowExpireExecutor = expireExecutor;
    }

    /**
     * 测试构造器：跳过插件扫描，直接注入执行器。
     * defaultRateLimiter 默认为 null，若测试需要访问 getRateLimiter / getDefaultRateLimiter，
     * 应使用下面接收 defaultRateLimiter 的重载。
     */
    @JustForTest
    RateLimitExtension(Extensions extensions, ScheduledExecutorService syncExecutor,
                       ScheduledExecutorService windowExpireExecutor) {
        this(extensions, syncExecutor, windowExpireExecutor, null);
    }

    @JustForTest
    RateLimitExtension(Extensions extensions, ScheduledExecutorService syncExecutor,
                       ScheduledExecutorService windowExpireExecutor, ServiceRateLimiter defaultRateLimiter) {
        this.extensions = extensions;
        this.syncExecutor = syncExecutor;
        this.windowExpireExecutor = windowExpireExecutor;
        this.defaultRateLimiter = defaultRateLimiter;
    }

    public ServiceRateLimiter getDefaultRateLimiter() {
        return defaultRateLimiter;
    }

    public Extensions getExtensions() {
        return extensions;
    }

    public ServiceRateLimiter getRateLimiter(RateLimitProto.Rule.Resource resource, String action) {
        String name = getRateLimiterName(resource, action);
        return rateLimiters.get(name);
    }

    private String getRateLimiterName(RateLimitProto.Rule.Resource resource, String action) {
        if (null != resource && StringUtils.isNotBlank(action)) {
            if (StringUtils.equals(action, LIMITER_TSF)) {
                return LIMITER_TSF;
            }
            if (resource.equals(RateLimitProto.Rule.Resource.QPS)) {
                if (StringUtils.equals(action, LIMITER_UNIRATE)) {
                    return LIMITER_UNIRATE;
                } else if (StringUtils.equals(action, LIMITER_REJECT)) {
                    return LIMITER_REJECT;
                }
                return LIMITER_REJECT;
            } else if (resource.equals(RateLimitProto.Rule.Resource.CONCURRENCY)) {
                return LIMITER_CONCURRENCY;
            }
        }
        return LIMITER_REJECT;
    }

    /**
     * 提交同步任务
     *
     * @param task 任务
     */
    public void submitSyncTask(RemoteSyncTask task, long initialDelay, long delay) {
        // 用 computeIfAbsent 保证 "判定 + schedule + put" 原子，
        // 防止两个并发 caller 同时绕过 init() CAS 时各自 schedule 出一个 future
        ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
        ScheduledFuture<?> existing = scheduledTasks.computeIfAbsent(task.getWindow().getUniqueKey(), key -> {
            ScheduledFuture<?> sf = syncExecutor.scheduleWithFixedDelay(task, initialDelay, delay,
                    TimeUnit.MILLISECONDS);
            holder[0] = sf;
            return sf;
        });
        if (existing != holder[0]) {
            // init() 内部 CAS 保证只 submit 一次，重复进入说明上层调用方违反契约；
            // 这里只记录，不动 window.status——否则会和 handleRateLimitInitResponse 的状态守卫相冲突
            LOG.warn("task has exist, ignore, task {}, window {}, uniqueKey {} ", task, task.getWindow(),
                    task.getWindow().getUniqueKey());
            return;
        }
        LOG.info("submit sync task success, task {}, future {}, window {}, uniqueKey {} ", task, existing,
                task.getWindow(), task.getWindow().getUniqueKey());
    }

    private static final int EXPIRE_INTERVAL_SECOND = 5;

    /**
     * 提交过期检查任务
     *
     * @param task 任务
     */
    public synchronized void submitExpireJob(Runnable task) {
        if (expireFuture != null) {
            LOG.warn("[RateLimitExtension] expire job already submitted, cancelling old future");
            expireFuture.cancel(false);
            // 先置 null：若下面 scheduleWithFixedDelay 抛 RejectedExecutionException（destroy 后被调到），
            // 不能让 expireFuture 仍指向已 cancel 的 future，否则下次重入会再 cancel 一个无效引用
            expireFuture = null;
        }
        expireFuture = windowExpireExecutor
                .scheduleWithFixedDelay(task, EXPIRE_INTERVAL_SECOND, EXPIRE_INTERVAL_SECOND, TimeUnit.SECONDS);
    }

    /**
     * 停止同步任务
     *
     * @param uniqueKey 窗口唯一标识
     * @param window    限流窗口
     */
    public void stopSyncTask(String uniqueKey, RateLimitWindow window) {
        // 从connector初始化列表清理
        Runnable cleanTask = () -> {
            try {
                AsyncRateLimitConnector connector = window.getWindowSet().getAsyncRateLimitConnector();
                ServiceIdentifier identifier = new ServiceIdentifier(window.getSvcKey().getService(),
                        window.getSvcKey().getNamespace(), window.getLabels());
                // 窗口已 DELETED，不能借此处再新建 stream 连接
                StreamCounterSet streamCounterSet = connector.peekStreamCounterSet(window.getUniqueKey());
                if (streamCounterSet != null) {
                    streamCounterSet.deleteInitRecord(identifier, window);
                }
                LOG.info("clean task run success, window {}", window);
            } catch (Throwable e) {
                LOG.error("clean task run failed, window {}", window.getUniqueKey(), e);
            }
        };
        // syncExecutor 已 shutdown 说明 destroy 流程在跑，整个 SDK 都在关，connector 的状态变更交给 destroy 路径整体清理。
        // 避免 inline 在调用方线程跑 cleanTask，让 unInit 的调用方意外承担 connector 状态变更责任。
        if (syncExecutor.isShutdown()) {
            LOG.info("[RateLimitExtension] syncExecutor already shutdown, skip cleanTask for uniqueKey {}", uniqueKey);
        } else {
            try {
                syncExecutor.schedule(cleanTask, RateLimitConstants.STOP_SYNC_CLEAN_DELAY_MS, TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException e) {
                // shutdown 与 schedule 之间的窗口期，与上面 isShutdown 短路同因，仍以丢弃为准
                LOG.warn("[RateLimitExtension] syncExecutor rejected cleanTask for uniqueKey {}", uniqueKey);
            }
        }
        ScheduledFuture<?> future = scheduledTasks.remove(uniqueKey);
        LOG.info("scheduledTasks remove uniqueKey {}, future {}", uniqueKey, future);
        if (null != future) {
            future.cancel(true);
        }
    }

    @Override
    protected void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{syncExecutor, windowExpireExecutor});
    }

    @JustForTest
    Map<String, ScheduledFuture<?>> getScheduledTasks() {
        return scheduledTasks;
    }
}
