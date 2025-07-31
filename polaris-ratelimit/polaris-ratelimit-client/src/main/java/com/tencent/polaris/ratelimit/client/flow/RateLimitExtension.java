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

import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.ratelimit.client.sync.RemoteSyncTask;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import com.tencent.polaris.specification.api.v1.traffic.manage.RateLimitProto;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

import static com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter.*;

public class RateLimitExtension extends Destroyable {

    private final Extensions extensions;

    private final Map<String, ServiceRateLimiter> rateLimiters = new HashMap<>();

    private ServiceRateLimiter defaultRateLimiter;

    private final ScheduledExecutorService syncExecutor;

    private final ScheduledExecutorService windowExpireExecutor;

    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

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
        ScheduledFuture<?> scheduledFuture = syncExecutor
                .scheduleWithFixedDelay(task, 0, delay, TimeUnit.MILLISECONDS);
        scheduledTasks.put(task.getWindow().getUniqueKey(), scheduledFuture);
    }

    private static final int EXPIRE_INTERVAL_SECOND = 5;

    /**
     * 提交过期检查任务
     *
     * @param task 任务
     */
    public void submitExpireJob(Runnable task) {
        windowExpireExecutor
                .scheduleWithFixedDelay(task, EXPIRE_INTERVAL_SECOND, EXPIRE_INTERVAL_SECOND, TimeUnit.SECONDS);
    }

    public void stopSyncTask(String uniqueKey) {
        ScheduledFuture<?> future = scheduledTasks.remove(uniqueKey);
        if (null != future) {
            future.cancel(true);
        }
    }

    @Override
    protected void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{syncExecutor, windowExpireExecutor});
    }
}
