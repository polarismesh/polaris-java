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

package com.tencent.polaris.ratelimit.client.flow;

import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.compose.Extensions;
import com.tencent.polaris.api.plugin.ratelimiter.ServiceRateLimiter;
import com.tencent.polaris.api.utils.ThreadPoolUtils;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.client.util.NamedThreadFactory;
import com.tencent.polaris.ratelimit.client.sync.RemoteSyncTask;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
            if (plugin.getName().equals(ServiceRateLimiter.LIMITER_REJECT)) {
                defaultRateLimiter = (ServiceRateLimiter) plugin;
            }
            rateLimiters.put(plugin.getName(), (ServiceRateLimiter) plugin);
        }
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(0,
                new NamedThreadFactory("rateLimit-sync"));
        executor.setMaximumPoolSize(1);
        ScheduledThreadPoolExecutor expireExecutor = new ScheduledThreadPoolExecutor(0,
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

    public ServiceRateLimiter getRateLimiter(String name) {
        return rateLimiters.get(name);
    }

    /**
     * 提交同步任务
     *
     * @param task 任务
     */
    public void submitSyncTask(RemoteSyncTask task) {
        ScheduledFuture<?> scheduledFuture = syncExecutor
                .scheduleWithFixedDelay(task, 0, getTaskDelayInterval(), TimeUnit.MILLISECONDS);
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



    private static long getTaskDelayInterval() {
        Random random = new Random();
        return RateLimitConstants.STARTUP_DELAY_MS + random.nextInt(RateLimitConstants.RANGE_DELAY_MS);
    }

    public void stopSyncTask(String uniqueKey) {
        ScheduledFuture<?> future = scheduledTasks.remove(uniqueKey);
        future.cancel(true);
    }

    @Override
    protected void doDestroy() {
        ThreadPoolUtils.waitAndStopThreadPools(new ExecutorService[]{syncExecutor, windowExpireExecutor});
    }
}
