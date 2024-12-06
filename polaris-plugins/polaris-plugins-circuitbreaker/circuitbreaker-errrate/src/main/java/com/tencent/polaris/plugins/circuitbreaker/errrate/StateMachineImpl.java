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

package com.tencent.polaris.plugins.circuitbreaker.errrate;

import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.StatusDimension;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.plugins.circuitbreaker.common.AbstractStateMachine;
import com.tencent.polaris.plugins.circuitbreaker.common.ConfigGroup;
import com.tencent.polaris.plugins.circuitbreaker.common.ConfigSet;
import com.tencent.polaris.plugins.circuitbreaker.common.ConfigSetLocator;
import com.tencent.polaris.plugins.circuitbreaker.common.HalfOpenCounter;
import com.tencent.polaris.plugins.circuitbreaker.common.stat.SliceWindow;
import com.tencent.polaris.plugins.circuitbreaker.common.stat.TimeRange;
import org.slf4j.Logger;

/**
 * 基于错误率的状态机转换实现
 *
 * @author andrewshan
 * @date 2019/8/26
 */
public class StateMachineImpl extends AbstractStateMachine<Config> {

    private static final Logger LOG = LoggerFactory.getLogger(StateMachineImpl.class);

    private final long metricTimeWindowMs;

    public StateMachineImpl(ConfigGroup<Config> configGroup, int pluginId, ConfigSetLocator<Config> configSetLocator,
            long metricTimeWindowMs) {
        super(configGroup, pluginId, configSetLocator);
        this.metricTimeWindowMs = metricTimeWindowMs;
    }

    @Override
    public boolean closeToOpen(Instance instance, StatusDimension statusDimension, Parameter parameter) {
        HalfOpenCounter halfOpenCounter = getHalfOpenCounterOnClose(instance, statusDimension);
        if (halfOpenCounter == null) {
            return false;
        }
        ConfigSet<Config> configSet = getConfigSetByLocator(instance, statusDimension, configSetLocator);
        Config plugConfig = configSet.getPlugConfig();

        ErrRateCounter errRateCounter = (ErrRateCounter) halfOpenCounter;
        SliceWindow metricWindow = errRateCounter.getSliceWindow(statusDimension);
        long currentTimeMs = parameter.getCurrentTimeMs();
        TimeRange timeRange = new TimeRange(currentTimeMs - metricTimeWindowMs, currentTimeMs);
        long requestCount = metricWindow.calcMetricsBothIncluded(Dimension.keyRequestCount.ordinal(), timeRange);
        if (requestCount == 0 || requestCount < plugConfig.getRequestVolumeThreshold()) {
            //未达到其实请求数阈值
            return false;
        }
        long failCount = metricWindow.calcMetricsBothIncluded(Dimension.keyFailCount.ordinal(), timeRange);
        double failRatio = (double) failCount / (double) requestCount;
        LOG.debug("errRate statistic: request count {}, fail count {}, instance {}:{}, dimension {}, failRatio {}",
                requestCount, failCount, instance.getHost(), instance.getPort(), statusDimension, failRatio);
        //错误率达标
        return failRatio >= plugConfig.getErrRate();
    }

}
