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

package com.tencent.polaris.ratelimit.client.api;

import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.stat.DefaultRateLimitResult;
import com.tencent.polaris.api.plugin.stat.RateLimitGauge;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.client.api.BaseEngine;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.ratelimit.api.core.LimitAPI;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.client.flow.QuotaFlow;
import com.tencent.polaris.ratelimit.client.pojo.CommonQuotaRequest;
import com.tencent.polaris.ratelimit.client.utils.LimitValidator;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import static com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode.QuotaResultOk;

/**
 * 默认的限流API实现
 */
public class DefaultLimitAPI extends BaseEngine implements LimitAPI {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLimitAPI.class);

    private final QuotaFlow quotaFlow = new QuotaFlow();

    private Collection<Plugin> statPlugins;

    public DefaultLimitAPI(SDKContext sdkContext) {
        super(sdkContext);
    }

    @Override
    protected void subInit() {
        quotaFlow.init(sdkContext.getExtensions());
        sdkContext.registerDestroyHook(new Destroyable() {
            @Override
            protected void doDestroy() {
                quotaFlow.destroy();
            }
        });
        statPlugins = sdkContext.getPlugins().getPlugins(PluginTypes.STAT_REPORTER.getBaseType());
    }

    @Override
    public QuotaResponse getQuota(QuotaRequest request) throws PolarisException {
        checkAvailable("LimitAPI");
        LimitValidator.validateQuotaRequest(request);
        CommonQuotaRequest commonQuotaRequest = new CommonQuotaRequest(request, sdkContext.getConfig());
        QuotaResponse response = quotaFlow.getQuota(commonQuotaRequest);
        reportRateLimit(request, response);
        return response;
    }

    private void reportRateLimit(QuotaRequest req, QuotaResponse rsp) {
        if (null != statPlugins) {
            try {
                DefaultRateLimitResult rateLimitGauge = new DefaultRateLimitResult();
                rateLimitGauge.setLabels(formatLabelsToStr(req.getLabels()));
                rateLimitGauge.setMethod(req.getMethod());
                rateLimitGauge.setNamespace(req.getNamespace());
                rateLimitGauge.setService(req.getService());
                rateLimitGauge.setResult(
                        rsp.getCode() == QuotaResultOk ? RateLimitGauge.Result.PASSED : RateLimitGauge.Result.LIMITED);
                StatInfo statInfo = new StatInfo();
                statInfo.setRateLimitGauge(rateLimitGauge);

                for (Plugin statPlugin : statPlugins) {
                    if (statPlugin instanceof StatReporter) {
                        ((StatReporter) statPlugin).reportStat(statInfo);
                    }
                }
            } catch (Exception ex) {
                LOG.info("rate limit report encountered exception, e: {}", ex.getMessage());
            }
        }
    }

    private static String formatLabelsToStr(Map<String, String> labels) {
        if (null == labels) {
            return null;
        }

        if (labels.isEmpty()) {
            return "";
        }

        List<String> tmpList = new ArrayList<>();
        String labelEntry;
        for (Map.Entry<String, String> entry : labels.entrySet()) {
            labelEntry = entry.getKey() + RateLimitConstants.DEFAULT_KV_SEPARATOR + labels.get(entry.getKey());
            tmpList.add(labelEntry);
        }
        Collections.sort(tmpList);
        return String.join(RateLimitConstants.DEFAULT_ENTRY_SEPARATOR, tmpList);
    }
}
