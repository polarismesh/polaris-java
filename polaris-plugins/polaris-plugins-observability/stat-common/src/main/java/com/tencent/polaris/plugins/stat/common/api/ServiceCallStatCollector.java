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

package com.tencent.polaris.plugins.stat.common.api;

import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.api.pojo.InstanceGauge;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.client.api.ServiceCallResultListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于将每次调用的结果转为StatInfo，
 * 并交由statPlugins统计。
 */
public class ServiceCallStatCollector implements ServiceCallResultListener {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceCallStatCollector.class);

    private Collection<Plugin> statPlugins;

    private final AtomicBoolean init = new AtomicBoolean(false);

    /**
     * 初始化handler
     *
     * @param context SDK上下文
     */
    @Override
    public void init(SDKContext context) {
        if (!init.compareAndSet(false, true)) {
            return;
        }

        this.statPlugins = context.getPlugins().getPlugins(PluginTypes.STAT_REPORTER.getBaseType());
    }

    /**
     * 收到服务上报数据后的回调处理
     *
     * @param result 上报数据
     */
    @Override
    public void onServiceCallResult(InstanceGauge result) {
        if (null == statPlugins) {
            return;
        }

        try {
            for (Plugin statPlugin : statPlugins) {
                if (statPlugin instanceof StatReporter) {
                    ((StatReporter) statPlugin).reportStat(convert(result));
                }
            }
        } catch (Exception ex) {
            LOG.info("service call report encountered exception, e: {}", ex.getMessage());
        }
    }

    /**
     * 停机释放资源
     */
    @Override
    public void destroy() {
        if (!init.compareAndSet(true, false)) {
            return;
        }

        if (null == statPlugins) {
            return;
        }

        for (Plugin statPlugin : statPlugins) {
            statPlugin.destroy();
        }
        statPlugins = null;
    }

    private static StatInfo convert(InstanceGauge result) {
        StatInfo statInfo = new StatInfo();
        statInfo.setRouterGauge(result);
        return statInfo;
    }
}
