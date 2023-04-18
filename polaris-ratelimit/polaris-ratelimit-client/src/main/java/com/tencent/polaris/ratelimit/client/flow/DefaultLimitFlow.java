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

import static com.tencent.polaris.ratelimit.api.rpc.QuotaResultCode.QuotaResultOk;

import com.tencent.polaris.api.config.global.FlowConfig;
import com.tencent.polaris.api.control.Destroyable;
import com.tencent.polaris.api.plugin.Plugin;
import com.tencent.polaris.api.plugin.common.PluginTypes;
import com.tencent.polaris.api.plugin.stat.DefaultRateLimitResult;
import com.tencent.polaris.api.plugin.stat.RateLimitGauge;
import com.tencent.polaris.api.plugin.stat.StatInfo;
import com.tencent.polaris.api.plugin.stat.StatReporter;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.logging.LoggerFactory;
import com.tencent.polaris.ratelimit.api.flow.LimitFlow;
import com.tencent.polaris.ratelimit.api.rpc.QuotaRequest;
import com.tencent.polaris.ratelimit.api.rpc.QuotaResponse;
import com.tencent.polaris.ratelimit.client.pojo.CommonQuotaRequest;
import com.tencent.polaris.ratelimit.client.utils.RateLimitConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class DefaultLimitFlow implements LimitFlow {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultLimitFlow.class);

    private final QuotaFlow quotaFlow = new QuotaFlow();

    private SDKContext sdkContext;

    private Collection<Plugin> statPlugins;

    @Override
    public String getName() {
        return FlowConfig.DEFAULT_FLOW_NAME;
    }

    @Override
    public void setSDKContext(SDKContext sdkContext) {
        this.sdkContext = sdkContext;
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
    public QuotaResponse getQuota(QuotaRequest request) {
        CommonQuotaRequest commonQuotaRequest = new CommonQuotaRequest(request, sdkContext.getConfig());
        QuotaResponse response = quotaFlow.getQuota(commonQuotaRequest);
        reportRateLimit(request, response);
        return response;
    }

    /**
     * 限流指标不在保留单独的指标视图，全部合并到 upstream_xxx 的指标视图中
     */
    @Deprecated
    private void reportRateLimit(QuotaRequest req, QuotaResponse rsp) {
        if (null != statPlugins && !RateLimitConstants.REASON_DISABLED.equals(rsp.getInfo())) {
            try {
                DefaultRateLimitResult rateLimitGauge = new DefaultRateLimitResult();
                rateLimitGauge.setLabels(formatLabelsToStr(req.getLabels()));
                rateLimitGauge.setMethod(req.getMethod());
                rateLimitGauge.setNamespace(req.getNamespace());
                rateLimitGauge.setService(req.getService());
                rateLimitGauge.setResult(
                        rsp.getCode() == QuotaResultOk ? RateLimitGauge.Result.PASSED : RateLimitGauge.Result.LIMITED);
                rateLimitGauge.setRuleName(rsp.getActiveRule() == null ? null : rsp.getActiveRule().getName().getValue());
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
